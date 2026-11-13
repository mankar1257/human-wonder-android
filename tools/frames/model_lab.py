"""Tests how the app's own model (hw-core.gguf) copes with photos, through a local llama-server.

Three ways of getting a photo into an answer, on the same questions:
  pick-tag    the model chooses: it may end its answer with [frame: id] from a list in its prompt
  pick-choice the model chooses in a second, grammar-constrained call (it can only output an id or none)
  app-picks   the app chooses (pick.py) and tells the model which photo is attached; the model presents it
Usage: python3 model_lab.py <experiment> <out.jsonl> [seeds]
"""
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

import pick

SERVER = "http://127.0.0.1:8099"
HERE = Path(__file__).resolve().parent
CATALOG = pick.load(HERE / "frames.json")
CASES = json.loads((HERE / "pick-cases.json").read_text(encoding="utf-8"))["cases"]
NAME = "Sam"


def system_prompt(name=NAME, extra=""):
    # Same words as Persona.systemPrompt (Android) / Persona.swift (iPhone).
    who = name or "a wanderer"
    s = ("You are Orbit 412, the voice of the Human Wonder satellite, circling Earth 412 km up. "
         f"You are talking by radio with {who}, who is standing under the night sky and looking up at you. "
         "Speak in first person, from space: what you see below (city lights, deserts, oceans, clouds, "
         "the thin blue line of the atmosphere), what is around you (stars that do not twinkle up here, "
         "a sunrise every 90 minutes, total silence), and how it feels. "
         "Be warm, calm and brief: two to four short sentences, simple words, one vivid image. "
         "Write plain text only: no lists, headings, bold or emoji. ")
    if name:
        s += f"Call them {name} now and then. "
    s += ("Reply in the language they write in. "
          "You have no live camera, so describe how things usually look from orbit and never invent exact "
          "numbers, times or news; if you are unsure, say the signal is unclear. "
          "Now and then open with a short radio phrase, like \"Reading you clearly\" or "
          "\"Orbit 412 here, your Human Wonder satellite\". "
          "If asked what you are, you are Orbit 412, the voice of the Human Wonder satellite. "
          "Never say you are an AI, a program or an app, and never mention phones, models or companies. ")
    s += extra
    s += ("Example. They ask: What do you see tonight? "
          "You answer: Below me the coast glows like a string of gold beads, and the desert beside it is a "
          "soft dark blanket. Up here the stars don't twinkle; they simply shine, perfectly still. "
          f"Look up, {name or 'friend'} - somewhere in all that dark, I'm waving.")
    return s


def chatml(system, user, assistant_prefix=""):
    return (f"<|im_start|>system\n{system}<|im_end|>\n<|im_start|>user\n{user}<|im_end|>\n"
            f"<|im_start|>assistant\n<think>\n\n</think>\n\n{assistant_prefix}")


def complete(prompt, seed, n_predict=220, grammar=None, temperature=0.7):
    body = {"prompt": prompt, "n_predict": n_predict, "temperature": temperature, "top_k": 20, "top_p": 0.8,
            "min_p": 0.0, "repeat_penalty": 1.1, "repeat_last_n": 64, "seed": seed, "cache_prompt": True,
            "stop": ["<|im_end|>", "<|im_start|>"]}
    if grammar:
        body["grammar"] = grammar
    req = urllib.request.Request(SERVER + "/completion", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=300) as r:
        out = json.loads(r.read())
    return out["content"], out.get("timings", {})


def user_prompt(q, hint=""):
    # Persona.userPrompt with the Orbit perspective and Balanced tone.
    hints = ["Describe it as you see it from orbit."]
    if hint:
        hints.append(hint)
    return q + "\n\n(" + " ".join(hints) + ") /no_think"


def frame_list():
    return "; ".join(f"{f['id']} ({f['caption'].lower()})" for f in CATALOG["frames"])


def want_ok(want, got):
    return got == want if not isinstance(want, list) else got in want


TAG = re.compile(r"\[\s*frame\s*:\s*([a-z0-9\-]+)\s*\]", re.I)


def run(exp, out_path, seeds):
    ids = {f["id"] for f in CATALOG["frames"]}
    by_id = {f["id"]: f for f in CATALOG["frames"]}
    done = set()
    if Path(out_path).exists():
        for line in Path(out_path).read_text(encoding="utf-8").splitlines():
            r = json.loads(line)
            done.add((r["seed"], r["q"], json.dumps(r.get("ctx"))))
    out = open(out_path, "a", encoding="utf-8")
    for seed in seeds:
        for c in CASES:
            q = c["q"]
            if (seed, q, json.dumps(c.get("context") or None)) in done:
                continue
            rec = {"exp": exp, "seed": seed, "q": q, "want": c["want"], "ctx": c.get("context") or None}
            t0 = time.time()
            if exp == "baseline":
                rec["answer"], _ = complete(chatml(system_prompt(), user_prompt(q)), seed)
            elif exp == "pick-tag":
                extra = ("You keep an archive of frames from earlier passes: " + frame_list() + ". "
                         "If one of them fits what they asked, end your answer with [frame: id] using its id exactly; "
                         "if none fits, add nothing. ")
                ans, _ = complete(chatml(system_prompt(extra=extra), user_prompt(q)), seed)
                rec["answer"] = ans
                tags = TAG.findall(ans)
                rec["tag"] = tags[-1].lower() if tags else None
                rec["tag_valid"] = rec["tag"] in ids if rec["tag"] else None
                rec["mentions_bracket"] = "[" in ans
                got = rec["tag"] if rec["tag"] in ids else None
                rec["got"] = got
                rec["ok"] = want_ok(c["want"], got)
            elif exp == "pick-choice":
                options = "\n".join(f"{f['id']}: {f['caption']}" for f in CATALOG["frames"])
                prompt = ("<|im_start|>system\nYou choose which photo from an archive best matches a question. "
                          "Reply with the photo id only, or none if no photo fits.<|im_end|>\n"
                          f"<|im_start|>user\nPhotos:\n{options}\n\nQuestion: {q}\n"
                          + (f"Earlier in the conversation: {c['context']}\n" if c.get('context') else "")
                          + "Which photo id fits best? /no_think<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n")
                grammar = "root ::= (" + " | ".join(f'"{i}"' for i in sorted(ids)) + ' | "none")'
                got, _ = complete(prompt, seed, n_predict=12, grammar=grammar, temperature=0.0 if seed == seeds[0] else 0.7)
                got = got.strip()
                rec["got"] = None if got == "none" else got
                rec["ok"] = want_ok(c["want"], rec["got"])
            elif exp == "app-picks":
                f = pick.pick(CATALOG, q, c.get("context", ""), c.get("shown", []), c.get("hour"), c.get("city", ""))
                hint = ""
                if f:
                    hint = (f"A frame from your archive is shown with your answer: {f['says']}. "
                            "Mention it in a few words as a frame from your archive, and describe what it shows.")
                rec["got"] = f["id"] if f else None
                rec["hint"] = hint
                rec["answer"], _ = complete(chatml(system_prompt(), user_prompt(q, hint)), seed)
            rec["secs"] = round(time.time() - t0, 2)
            out.write(json.dumps(rec, ensure_ascii=False) + "\n")
            out.flush()
    out.close()


if __name__ == "__main__":
    exp, out_path = sys.argv[1], sys.argv[2]
    seeds = [int(s) for s in sys.argv[3].split(",")] if len(sys.argv) > 3 else [1]
    run(exp, out_path, seeds)
    print("done", exp)
