"""Reference version of Orbit's photo picker (the apps implement the same steps).

Kept in Python so the rules can be checked quickly against tools/frames/pick-cases.json:
    python3 tools/frames/pick.py            # runs the cases, prints any that disagree
The Android (ai/Frames.kt) and iPhone (Engine/Frames.swift) versions must give the same answers;
the Android unit test reads the same cases file.
"""
import json
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
MIN_SCORE = 3


def normalize(text: str) -> str:
    """Lower-case; anything that is not a letter, digit or combining mark becomes a space."""
    out = []
    for ch in text.lower():
        cat = unicodedata.category(ch)
        out.append(ch if cat[0] in "LNM" else " ")
    return " " + " ".join("".join(out).split()) + " "


def has(text: str, key: str) -> bool:
    k = normalize(key).strip()
    if not k:
        return False
    if " " + k + " " in text:
        return True
    return k.isascii() and (" " + k + "s ") in text


def score(text: str, frame: dict) -> int:
    return sum(w for k, w in frame["keys"].items() if has(text, k))


def pick(catalog: dict, question: str, context: str = "", shown=(), hour=None, city: str = ""):
    """The frame to send with the answer to [question], or None.

    context: the previous question and answer (for "can you show me?" follow-ups).
    shown: frame ids already in this conversation (never sent twice).
    hour: the person's local hour (0-23), used when the question does not say day or night.
    city: where the person usually looks up from (picks the home region for "what do you see?").
    """
    q = normalize(question)
    asks = "?" in question or "\u061f" in question  # a question mark (Latin or Arabic)
    wants_show = any(has(q, w) for w in catalog["show_words"]) or (
        asks and any(has(q, w) for w in catalog["see_words"]))
    wants_loop = any(has(q, w) for w in catalog["loop_words"])
    says_night = any(has(q, w) for w in catalog["night_words"])
    says_day = any(has(q, w) for w in catalog["day_words"])
    clock_night = hour is not None and (hour >= 19 or hour < 6)
    night = says_night or (not says_day and clock_night)
    shown = set(shown)

    frames = catalog["frames"]
    scores = [score(q, f) for f in frames]
    if max(scores) < MIN_SCORE and wants_show and context.strip():
        c = normalize(context)
        scores = [s + score(c, f) for s, f in zip(scores, frames)]

    best, best_rank = None, None
    for s, f in zip(scores, frames):
        if s < MIN_SCORE or f["id"] in shown:
            continue
        rank = s
        if wants_loop and f["kind"] == "loop":
            rank += 2
        if f["time"] != "any":
            if night:
                rank += 2 if f["time"] == "night" else -2
            elif says_day or hour is not None:
                rank += 2 if f["time"] == "day" else -2
        if best_rank is None or rank > best_rank:
            best, best_rank = f, rank
    if best is not None:
        return best
    if not wants_show or any(has(q, w) for w in catalog["about_me_words"]):
        return None

    # "What do you see?" with no subject: a view over the person's part of the world.
    where = q + " " + normalize(context) + " " + normalize(city)
    region = next((r for r in catalog["regions"] if any(has(where, k) for k in r["keys"])), catalog["regions"][-1])
    choices = region["night"] if night else region["day"]
    by_id = {f["id"]: f for f in frames}
    ordered = sorted(choices, key=lambda i: 0 if (wants_loop and by_id[i]["kind"] == "loop") else 1)
    for fid in ordered:
        if fid not in shown:
            return by_id[fid]
    return None


def load(path=HERE / "frames.json"):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def run_cases(catalog, cases_path=HERE / "pick-cases.json") -> int:
    cases = json.loads(Path(cases_path).read_text(encoding="utf-8"))["cases"]
    bad = 0
    for c in cases:
        f = pick(catalog, c["q"], c.get("context", ""), c.get("shown", []), c.get("hour"), c.get("city", ""))
        got = f["id"] if f else None
        want = c["want"]
        ok = got == want if not isinstance(want, list) else got in want
        if not ok:
            bad += 1
            print(f"MISMATCH  {c['q']!r}: got {got}, want {want}")
    print(f"{len(cases) - bad}/{len(cases)} cases agree")
    return bad


if __name__ == "__main__":
    sys.exit(1 if run_cases(load()) else 0)
