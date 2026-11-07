---
name: human-wonder-dev
description: Use for any work on the Human Wonder Android app (Orbit 412, the on-device sky chatbot): design or chat UI changes, the preview canvas, Kotlin/Compose code, persona rules, and building on the Mac.
---

# Human Wonder app: development workflow

Human Wonder is an Android POC. A small language model runs fully on the phone, but to the
person it is Orbit 412, the Human Wonder satellite, talking from 412 km up. The app exists to
sell Human Wonder: brand and persona come first. Read this and `CLAUDE.md` +
`memory/context/lessons.md` (workspace root) before changing anything.

## Where things live

- GitHub: `mankar1257/human-wonder-android` (branch `main`). In a cloud session attach it with
  add_repo and clone with `--recurse-submodules` (llama.cpp submodule at `third_party/llama.cpp`).
- The owner's Mac: `~/Desktop/stitch-mcp/human-wonder-android` (the synced copy that gets built).
  Memory: `~/Desktop/stitch-mcp/CLAUDE.md` and `~/Desktop/stitch-mcp/memory/`. This skill:
  `~/Desktop/stitch-mcp/skills/human-wonder-dev/SKILL.md` (copied into `.claude/skills/` for Claude Code;
  keep it updated here AND in the repo under `docs/skills/`).
- Stitch source designs: `~/Desktop/stitch-mcp/design/stitch/`.
- Preview canvas (Claude Design): https://claude.ai/artifact/Qo42XvUTr9HFJnjETQmNC6, slug
  `c0b04488-5a95-4a93-a2de-52267f7dfd47`. Source in the repo at `design/preview/project/`
  (Main, Name, Chat, Transmission `.dc.html` + `canvas.json`); read `design/preview/README.md`.

Code map (`app/src/main/java/ai/humanwonder/poc/`): `MainActivity.kt` (screens, voice input),
`ChatViewModel.kt` (state, streaming, Orbit 412 system prompt, `visibleAnswer()` sanitizer,
`lostSignalMessage()`), `model/ModelStore.kt` (unpacks the bundled model on first launch),
`ui/OnboardingScreens.kt`, `ui/ChatScreen.kt`, `ui/Components.kt` (BrandLockup, PrimaryButton,
OrbitMark, HwIcon), `ui/Sky.kt` (DotGrid, SatelliteArc, StarTrailSky, DuneHorizon),
`ui/GalaxyBackground.kt` (aurora shader, single source of truth), `ui/Theme.kt`. `lib/` is the
llama.cpp JNI wrapper. `app/build.gradle.kts` `fetchModel` downloads and SHA-checks the model into
the APK (imports java.net / java.security at the top: in Gradle Kotlin DSL a bare `java.` is the
java extension, not the package).

## Non-negotiables

1. The app on the phone must look exactly like the preview. Change both together with the same
   values. Never ship an app change the preview does not show.
2. Run the design skills before showing anything, every time (not just read them):
   - `design:design-critique` and `design:accessibility-review` on screenshots of every screen,
   - `design:ux-copy` for every string,
   - Material 3 rules (ehmo/platform-design-skills android): text >= 12sp, touch targets 48dp,
     contrast >= 4.5:1, content descriptions, test at 200% font scale,
   - `oh-my-android:android-ui-check` on the real phone when the Oh My Android app is installed.
3. Responsive check is mandatory: every screen at 360 / 393 / 412 dp widths and font scale
   1.0 / 1.3 / 1.5, in the preview (add test artboards) and in Compose reasoning. Primary actions
   (send, mic, CTA) never shrink; chips and labels collapse first; no row relies on everything
   fitting; very large headlines are capped. (A squeezed send button reached the phone once.)
4. Never reveal how it works: no model or vendor names, and no "on-device", "offline", "AI",
   "model", "SLM", "engine", "download" or "app" in any screen, status, placeholder or error.
   Errors go through `lostSignalMessage()`, never `e.message`.
5. Human Wonder everywhere: logo lockup on every screen; greeting "your Human Wonder satellite";
   chat footer "Human Wonder's Orbit 412 sees far, but it can still be wrong."; copied answers end
   with "— Orbit 412, Human Wonder".
6. Orbit 412 voice: status "Orbit 412 is rising over the horizon, n%" / "… is finding your
   signal…" / "… is overhead now" / "… is receiving you…" / "… is transmitting…" / "Orbit 412's
   signal is lost". Placeholder "Ask Orbit 412 anything". Thinking lines rotate. Answers: plain
   text, 2-4 short sentences, first person from orbit, sometimes a radio opener. Clear first,
   poetic second. Keep the system prompt short with one example.
7. Fully offline (no INTERNET permission); the model ships inside the APK. Free tools only.

## Design system

One visual language: Geist (light display, regular body), true black, one teal accent #83FFE5 for
actions, ink #ECEFF0 / #A7B2B0 / #8D9997. The bar (owner, 2026-11-07): professional, clean, yet
rich and premium. Richness comes from one real photograph and precise type, never from effects.
Rejected and never to return: Newsreader serif headings, mono all-caps labels, heavy dark scrims,
glowing "transmission" text, dot grid over photos, dotted arc lines, and (called "fancy and
childish") the swirling star-trail sky, the dune silhouette, the aurora behind Home, a glowing
Orbit mark on every list row, sparkle icons on chips, rainbow-gradient avatars.

- Landing: Liwa dunes photo at full colour, light scrims, slow zoom; lockup; "The sky has
  something to tell you."; "Human Wonder gives the sky a voice: Orbit 412, a satellite passing
  over you tonight."; teal "Begin your first look up".
- Name: black sky; the Earth at night from orbit (`bg_earth_orbit`, the Gulf and Liwa) starts 17%
  down the screen (static); scrim black 0-17%, clear 33-58%, black by 92%. Orbit 412 glides
  just above the horizon, label above it (`SatelliteArc(place = ::nameScreenArc, labelAbove)`):
  the owner loves seeing the orbit move, so keep a pass on every photo of the sky, but slow:
  one pass takes 90 s everywhere (`ORBIT_PASS_MS`; "not too fast please").
  "What should Orbit 412 call you?" (32/38 light, keep "Orbit 412" unbroken), plain field
  (56dp, radius 16, rgba(14,17,19,.82), white 16% border, no icon), teal "Meet Orbit 412",
  lock + "Your name stays on this phone." in #8D9997.
- Home (v2): plain black, no shader. Header menu / lockup / solid avatar (#1F2629, white 12%
  border). Greeting 30/36 light. Orbit 412 photo card (radius 24): Earth photo at card width,
  shifted up 30% of the width, a 120dp clear band, then status / "Orbit 412" 24/30 / one line /
  teal 56dp button on a dark band that grows with the text. Orbit 412 crosses the card's photo
  above the horizon, no label (`place = ::homeCardArc`). Arc numbers: apex y=357, drop 40 px on
  the 768x1376 photo (Sky.kt EARTH_ARC_*, preview gen/orbit_js.py). "Recent transmissions" + See all,
  grouped list (radius 20, white 4% fill, 8% border), rows title + time + grey chevron.
  "Try asking": outlined chips that wrap, no icons.
- Chat (Gemini + Stitch home): aurora-valley shader + faint dot grid; `u_calm` sinks the aurora
  when talking. Answers Geist 16/25 #E8ECEC, no glow; newest ~18 chars fade in. User bubble
  #1D2225. Orbit mark + "Orbit 412" per answer, copy action. Composer: Orbit/Ground, tone chip,
  mic, send (grey / teal / stop). Header: back, Human Wonder lockup + status, new conversation.
- Orbit 412 crossing a sky (`SatelliteArc`): 18-dot comet trail, glow r13 at 0.5, core r2.3,
  label 11sp medium at 0.78, no arc line.

## How to make a change

1. Design it in the preview first: edit `design/preview/project/*.dc.html`, run
   `node design/preview/test_logic.js`, render screenshots at the test sizes (headless Chrome:
   `--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader`), run the design skills on
   them, then republish each changed file (publish_files, one file per call, `as` =
   `project/<name>`, mode patch). Tell the owner what to look at.
2. Port to Compose with identical values. Shader changes: edit `GalaxyBackground.kt`, then
   `python3 design/preview/sync_shader.py`.
3. Android cannot compile in the cloud (Google Maven blocked). Have an independent agent review
   the changed Kotlin by reading (overloads, scopes, imports incl. getValue/setValue for `by`,
   resources, GLSL ES 1.0). In a local Claude Code session on the Mac, just build.
4. Commit and push to `main` with the session's attribution lines.
5. Sync to the Mac (cloud sessions): `git archive` (drop `third_party/llama.cpp`), gzip, copy to
   `/mnt/user-data/outputs/`, `device_commit_files` to `<mac project>/.transfer/<unique>.tgz`,
   extract with python3 via `device_bash` writing each file with `open(..., 'wb')` (no deletes
   there; overwrite removed files with a stub). Compare md5sums.
6. The owner double-clicks "Build Human Wonder.command" (`tools/build-on-mac.sh`; tools in
   `~/human-wonder-android-tools`). Output `HumanWonder-POC.apk` (~500 MB, next to the command);
   installs over USB if a phone is connected. Model cached in `app/build/hw-model/`.
7. Errors print in the Terminal; ask the owner to paste them (`tools/build.log` may be stale).
   Success check: `HumanWonder-POC.apk` modified time and ~500 MB size.

## Known constraints

- GitHub Actions cannot run (mankar1257 billing lock). Build on the Mac.
- POC signing key in `keystore/`; installs update over each other.
- The public GitHub README and the GGUF metadata reveal the setup to anyone who looks.
- First launch unpacks the model (status "Orbit 412 is rising over the horizon, n%").
- Candidate open-source bases (Apache-2.0, Kotlin/Compose/llama.cpp): jegly/OfflineLLM (zero
  network, bundled model, Room history, settings; minSdk 33), shubham0204/SmolChat-Android
  (ObjectBox history, drawer, model downloads).
