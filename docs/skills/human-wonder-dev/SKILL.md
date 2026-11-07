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

v2 (branch `v2-offlinellm`) is built on OfflineLLM (jegly/OfflineLLM b001c2c, Apache-2.0): Hilt,
Room, Navigation Compose, its SettingsRepository and its `smollm` llama.cpp module, with
Human Wonder's persona, UI, model and build. Attribution: `NOTICE`, `third_party_licenses/`,
`app/src/main/assets/licenses/NOTICE.txt`.

Code map (`app/src/main/java/ai/humanwonder/poc/`):
- `MainActivity.kt` (Hilt entry point, NavGraph, keep-awake), `HumanWonderApplication.kt`,
  `AppViewModel.kt` (profile, sky state, side-menu list/search, settings actions, export),
  `ChatViewModel.kt` (one chat: Room messages + streaming answer, TTS, haptics),
  `ChatState.kt` (Perspective, Tone, SkyState, ChatMessage, UiState).
- `ai/Persona.kt`: Orbit 412 system prompt (name + home sky), Perspective/Tone hints + `/no_think`,
  `visibleAnswer()` sanitizer, `lostSignalMessage()`. `ai/ModelManager.kt`: copies
  `assets/hw-core.gguf` on first launch (Aligning n% → Waking → Ready → Failed), `prepareChat()`
  points the native chat at a conversation. `ai/InferenceEngine.kt`: SmolLM wrapper.
- `data/` Room (`Conversation`, `Message`, DAOs) + `ChatRepository`, `SettingsRepository`
  (encrypted prefs, the profile as a StateFlow). `di/` Hilt modules. `utils/` TtsHelper etc.
- `ui/`: `OnboardingScreens.kt`, `ChatScreen.kt` (composer row measured by a SubcomposeLayout),
  `HomeScreen.kt`, `ConversationDrawer.kt` (side menu), `ProfileScreen.kt`, `LicensesScreen.kt`,
  `Components.kt` (BrandLockup fixed-dp logo, PrimaryButton, OrbitMark, HwIcon, InitialAvatar,
  `glass()`), `Sky.kt`, `GalaxyBackground.kt` (aurora shader, single source of truth), `Theme.kt`
  (`capScale()`: 28sp+ text grows at most 1.15× with font scale). `ui/navigation/` Routes, NavGraph.
- `smollm/` (package `ai.humanwonder.poc.smollm`): JNI glue + `third_party/llama.cpp` (submodule
  pinned to 620d222, also written in `third_party/llama.cpp.commit`). CPU only (no Vulkan).
- `app/build.gradle.kts` `fetchModel` downloads and SHA-checks the model into the APK (imports
  java.net / java.security at the top: in Gradle Kotlin DSL a bare `java.` is the java extension,
  not the package).

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
actions, ink #ECEFF0 / #A7B2B0 / #8D9997, glass panels (radius 28, gradient rgba(14,15,20,.9) to
rgba(70,74,128,.6), white 12-14% border). Rejected and never to return: Newsreader serif headings,
mono all-caps labels, heavy dark scrims, glowing "transmission" text, dot grid over photos,
dotted arc lines.

- Landing: Liwa dunes photo at full colour, light scrims, slow zoom; lockup; "The sky has
  something to tell you."; "Human Wonder gives the sky a voice: Orbit 412, a satellite passing
  over you tonight."; teal "Begin your first look up".
- Name: live star-trail sky (long exposure around the pole star, 1°/s, seed 412) over a dune
  silhouette; the pole star brightens as you type and each letter sends a ring; "What should
  Orbit 412 call you?" (keep "Orbit 412" unbroken); glass name field; teal "Meet Orbit 412".
- Chat (Gemini + Stitch home): aurora-valley shader + faint dot grid; `u_calm` sinks the aurora
  when talking. Answers Geist 16/25 #E8ECEC, no glow; newest ~18 chars fade in. User bubble
  #1D2225. Orbit mark + "Orbit 412" per answer, copy action. Composer (padding 14/8/8/12, row gap
  4): Orbit/Ground, tone chip, mic and send as fixed 48dp targets (send: 40dp circle inside);
  narrow rows hide the tone label first, then the Orbit/Ground labels. Header: "Open menu",
  Human Wonder lockup + status, new conversation. System back → Home.
- Home, side menu, Profile & settings: values in `design/preview/gen/hub.py` (glass
  rgba(20,24,30,.72) radius 24, sheet #0B0D12, avatar gradient #2FE6C8 → #7C5CFF).
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
   installs over USB if a phone is connected. Model cached in `app/build/hw-model/`. v2 needs
   Java 21 (downloaded to `jdk21/`), `platforms;android-37`, NDK 29.0.14206865, CMake 3.31.6;
   the script fetches llama.cpp at the pinned commit as a GitHub tarball when the folder lacks
   it (marker `third_party/llama.cpp/.hw-llama-commit`), since synced copies have no submodule.
   First v2 build ~15-30 min (Gradle 9 toolchain download + llama.cpp with 7 CPU variants).
7. Errors print in the Terminal; ask the owner to paste them (`tools/build.log` may be stale).
   Success check: `HumanWonder-POC.apk` modified time and ~500 MB size.

## Known constraints

- GitHub Actions cannot run (mankar1257 billing lock). Build on the Mac.
- POC signing key in `keystore/`; installs update over each other.
- The public GitHub README and the GGUF metadata reveal the setup to anyone who looks.
- First launch unpacks the model (status "Orbit 412 is rising over the horizon, n%"); phones that
  ran v1 already have the copy (same path, `files/models/hw-core.gguf`).
- Toolchain is OfflineLLM's (Gradle 9.8.2, AGP 9.5.1 built-in Kotlin, Kotlin 2.4.21, KSP 2.3.13,
  Hilt 2.60.1, Room 2.8.5, compileSdk 37, minSdk 33). Bump versions only together with OfflineLLM.
- versionCode starts at 200 (v1 was 2), versionName 2.0; same keystore, so v2 installs over v1.
- The open-source licenses screen shows the bundled notices verbatim, which name OfflineLLM and
  ggml: that is the one place such names appear, for license compliance.
