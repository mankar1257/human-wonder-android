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

## Screens (current)
1. Landing: Liwa dunes photo, Human Wonder lockup, "The sky has something to tell you.", teal CTA.
2. Name: star-trail sky over dune silhouette; "What should Orbit 412 call you?"; your star brightens as you type.
3. Chat: aurora-valley shader, Gemini-style readable answers, composer (Orbit/Ground, tone, mic, send), copy action.

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
