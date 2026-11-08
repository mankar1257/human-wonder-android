# Human Wonder Android (POC)

**Status:** working APK on the owner's phone (2026-11-07 build 12:16 IST). Next: home page, side menu
with chat history, profile; responsive fixes; decide base (own app vs OfflineLLM fork).

## Where
- Mac: `~/Desktop/stitch-mcp/human-wonder-android` (copy synced from the cloud workspace; not a git checkout to commit from)
- GitHub: `mankar1257/human-wonder-android` (public), branch `main`
- Preview canvas: https://claude.ai/artifact/Qo42XvUTr9HFJnjETQmNC6 (slug c0b04488-5a95-4a93-a2de-52267f7dfd47)
- Build tools on the Mac: `~/human-wonder-android-tools` (JDK 17, Android SDK, NDK 29.0.14206865, CMake 3.31.6)

## Stack
Kotlin 2.3, Jetpack Compose (BOM 2026.04.01), AGP 8.13.2, compileSdk 36, minSdk 28, arm64.
llama.cpp v0.9.0 submodule via Arm `com.arm.aichat` JNI wrapper (`lib/`). Model Qwen3-0.6B Q4_0
bundled in the APK as `assets/hw-core.gguf` (fetched + SHA-256 checked at build time). No INTERNET permission.

## Screens (current, v2 on branch v2-offlinellm; Mac folder human-wonder-v2)
1. Landing: Liwa dunes photo, Human Wonder lockup, "The sky has something to tell you.", teal CTA.
2. Name: black sky over the Earth at night from orbit (Gulf/Liwa photo), plain field, "Meet Orbit 412".
3. Home: greeting, Orbit 412 photo card with "Talk to Orbit 412", recent transmissions, "Try asking" chips.
4. Side menu (Gemini-style): Home, New conversation, search, history by Today/Yesterday/Earlier, profile.
5-6. Chat: aurora-valley shader, Gemini-style readable answers, composer (Orbit/Ground, tone, mic, send).
7. Profile & settings: name, email, place, point of view, tone, read aloud, haptics.

## Decision log
- 2026-11-06: llama.cpp + Qwen3-0.6B chosen; model first downloaded at runtime, then bundled in APK.
- 2026-11-06: GitHub Actions blocked (billing lock) -> local Mac build script.
- 2026-11-07: Glowing "transmission" chat rejected as unreadable -> Gemini-style chat.
- 2026-11-07: Old Stitch styling on screens 1-2 rejected -> same language as chat; brand everywhere.
- 2026-11-07: Dot grid over photos and dotted arc lines rejected.
- 2026-11-07: Name screen background -> live star-trail sky (research: long-exposure photography).
- 2026-11-07: Device feedback: spacing/wording off, headlines too big, send button squeezed.
  Wants home page, Gemini-style side menu with chat history, profile (name, email...), and asked
  to evaluate open-source bases (found OfflineLLM by jegly, SmolChat-Android; both Apache-2.0).
- 2026-11-07: Owner chose to fork OfflineLLM (jegly, Apache-2.0) as v2; phone is Android 13+.
- 2026-11-07: Star-trail Name screen and aurora Home called "too fancy and childish" -> Earth-from-
  orbit photo on Name, plain black Home with one photo card, quiet list, plain chips, solid avatar.
- 2026-11-07: Owner missed the moving orbit ("that was cool") -> Orbit 412 now glides along the
  Earth's horizon on Name and across the Home photo card (still on Welcome and the empty chat).
- 2026-11-08: First v2 build succeeded on the Mac (BUILD SUCCESSFUL in 6m 36s once Java 21,
  SDK 37, NDK 29 and Gradle 9.8.2 were in place). APK 482 MB at human-wonder-v2/HumanWonder-POC.apk,
  model inside, no INTERNET permission. v2 lives on branch v2-offlinellm (not merged to main yet).
