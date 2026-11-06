# Human Wonder — on-device chatbot POC (Android)

A basic chatbot app with a small language model that runs **entirely on the phone**.
After a one-time model download, which the app asks permission for, no internet is
needed and nothing typed leaves the device.

| | |
|---|---|
| Model | **Qwen3-0.6B**, 4-bit `Q4_0` GGUF (Apache-2.0), 429 MB, from `ggml-org/Qwen3-0.6B-GGUF`, pinned to a commit and SHA-256 checked |
| Engine | **llama.cpp v0.9.0** (git submodule) with Arm's JNI wrapper from llama.cpp's own Android example |
| CPU support | All Arm CPU variants are built (armv8.0 → armv9.2 with dotprod/i8mm/SVE/SME) and the best one is chosen at runtime |
| Phones | Android 9+ (API 28), 64-bit Arm. 3 GB+ RAM recommended. Uses ~0.8 GB RAM while chatting |
| UI | Jetpack Compose, styled after the Stitch "Human Wonder" design (dark phthalo + aurora teal) |

## Install the APK

1. On the phone, open the repo's **Releases** page and download `human-wonder-poc.apk`
   (or download the `human-wonder-poc-apk` artifact from the latest **Actions** run).
2. Open it and allow "Install unknown apps" for your browser or Files app when Android asks.
3. On first launch, the app shows what it needs (model size, Wi-Fi/mobile data, storage, RAM)
   and downloads **only after you tap "Allow download"**. The download can be paused and resumes.
4. Ask a question. Use the ⋮ menu for **New chat**, **About this model**, or **Delete model**.

No network? Copy `Qwen3-0.6B-Q4_0.gguf` to the phone and use **"I already have the model file"**.
Any other `.gguf` chat model can be imported the same way.

## Build

Every push to `main` builds a signed release APK in GitHub Actions
(`.github/workflows/android.yml`) and publishes it as a GitHub Release.

On a Mac without Android Studio, one script downloads Java 17 and the Android SDK into
`~/human-wonder-android-tools` (it asks you to accept the SDK licenses), builds the APK and
installs it on a USB-connected phone:

```bash
bash tools/build-on-mac.sh
```

Or with a recent Android Studio, or the command line:

```bash
git clone --recursive https://github.com/mankar1257/human-wonder-android.git
cd human-wonder-android
./gradlew :app:assembleRelease      # APK: app/build/outputs/apk/release/app-release.apk
```

The first build compiles llama.cpp for every Arm CPU variant (about 10–20 minutes).
Android Studio installs the pinned NDK (29.0.13113456) and CMake (3.31.6) automatically.

## How it works

```
app/  (Kotlin, Compose)
  MainActivity ─ ChatViewModel ─ ModelStore            consent → download/resume → SHA-256 → load
                      │
lib/  (com.arm.aichat)  InferenceEngine (Kotlin)  ── JNI ──  ai_chat.cpp ── llama.cpp (C++)
third_party/llama.cpp   pinned v0.9.0 (cd959bae)
```

- The model is stored in the app's private storage. No storage permission is needed, and it is removed on uninstall.
- Context window: 2,048 tokens. When it fills up, the oldest half of the conversation is dropped.
- Qwen3's "thinking" mode is switched off (`/no_think`) so answers start immediately.
- Sampling: temperature 0.7, top-k 20, top-p 0.8, presence penalty 0.5.

Changes to the vendored Arm/llama.cpp wrapper are marked `Human Wonder change` in
`lib/src/main/cpp` and `lib/src/main/java`: minSdk 33 → 28, 2,048 context instead of 8,192
(saves ~700 MB RAM), OpenMP off, Qwen3 sampling defaults, no `@FastNative` on long-running
calls, and a stop-position bug fix.

## Limitations (it's a POC)

- A 0.6B model is small: it can be confidently wrong about facts and weak at maths. Good for
  basic questions and showing that on-device AI works; not a source of truth.
- CPU only (no GPU/NPU). Expect roughly 10–30 tokens/s depending on the phone.
- Signed with a committed **POC key** (`keystore/`) so builds install over each other. For any
  real distribution, sign with a private key via the `HW_KEYSTORE*` environment variables
  (e.g. GitHub secrets).
- iOS is out of scope for now. llama.cpp also runs on iOS if that changes.

## Licences

App code: © Human Wonder / Null Field Systems. llama.cpp and the Arm AI Chat wrapper: MIT
(`lib/LICENSE-llama.cpp`). Qwen3-0.6B: Apache-2.0.
