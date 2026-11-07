# Human Wonder — "Talk to the infinite sky" (Android POC)

A chatbot whose small language model runs **entirely on the phone**. The model ships inside the
APK, so there is no download step, no network permission, and nothing the user types ever leaves
the device. In the app the AI is **Orbit**, the voice of the Human Wonder satellite; the
underlying model is never named to users.

## The experience (from the Stitch "Human Wonder Mobile App" design)

1. **Landing** – the lone wanderer on the Liwa dunes, "The sky has something to tell you."
2. **Name** – Earth at night over the Gulf, "What should the sky call you?" (Orbit uses the name).
3. **Talk to the infinite sky** – a calm, readable chat in the spirit of the Gemini app and the
   Stitch home screen:
   - true black with a faint dot grid and a live **aurora** (OpenGL shader) that rises around the
     message box on the empty screen and sinks low once you talk, so it never sits behind text;
   - Orbit's satellite crosses the empty sky, then lives on as the small mark beside each answer
     (it circles faster while Orbit thinks; "Listening across the dark…" shimmers);
   - answers are plain off-white text that fades in as it streams, with a copy button when done;
   - **Orbit / Ground** and **Balanced / Poetic / Scientific / Whisper** change how it answers,
     **+** (top right) starts a new conversation, the **mic** uses the phone's voice typing.

Fonts (Geist, Newsreader, JetBrains Mono), colours, icons and photos come from the Stitch project
(`design/stitch` in the working folder) and are bundled, so the app works offline. A clickable
preview of every screen lives on a Claude Design canvas; its source is in `design/preview/`.

## Build (Mac, no Android Studio needed)

Double-click **`Build Human Wonder.command`** (or run `bash tools/build-on-mac.sh`). The first run
installs Java 17 + the Android SDK into `~/human-wonder-android-tools` (you accept Google's SDK
licenses), downloads the model once into `app/build/hw-model/` (SHA-256 verified), builds
`HumanWonder-POC.apk` (~500 MB) and installs it on a USB-connected phone.

Or: `git clone --recursive …` and `./gradlew :app:assembleRelease` with any Android SDK.

## How it works

```
app/  Compose UI (Landing, Name, Chat) · GalaxyView (OpenGL ES shader) · ChatViewModel (Orbit persona)
      ModelStore: copies assets/hw-core.gguf out of the APK once (llama.cpp needs a file to mmap)
lib/  com.arm.aichat JNI wrapper ── llama.cpp (third_party, pinned v0.9.0), all Arm CPU variants
```

- Model file and checksum are pinned in `app/build.gradle.kts` (`fetchModel` task); it is not
  committed to git. `HW_MODEL_FILE=/path/model.gguf` builds with a local file instead.
- Orbit's persona lives in `ChatViewModel.systemPrompt()`. Any slip where the model names its
  underlying model or maker is replaced in the UI by "Orbit" / "Human Wonder".
- Context window 2,048 tokens; older turns are dropped automatically. ~0.8 GB RAM while chatting.

## Notes

- Answers come from a very small model: charming, but it can be wrong about facts.
- Anyone who unpacks the APK can still identify the model file from its metadata; the app just
  never says it. Keep this repository **private** if that matters.
- Signed with a committed POC key (`keystore/`) so builds install over each other; use the
  `HW_KEYSTORE*` environment variables to sign with a private key for real distribution.
- Licences: llama.cpp + Arm wrapper (MIT), fonts (OFL), model (Apache-2.0); texts are bundled in
  the APK under `assets/licenses/`.
