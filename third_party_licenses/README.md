# Third-party licenses

| Component | Where | License |
|-----------|-------|---------|
| OfflineLLM by jegly (github.com/jegly/OfflineLLM, commit b001c2c) | app architecture (`app/src/main/java/ai/humanwonder/poc/{ai,data,di,ui/navigation,utils}`), `smollm/` | Apache-2.0, `LICENSE-OfflineLLM.txt` (OfflineLLM ships no NOTICE file) |
| SmolChat-Android by shubham0204 (via OfflineLLM) | `smollm/src/main/cpp` | Apache-2.0 |
| llama.cpp / ggml (submodule `third_party/llama.cpp`, commit 620d222) | native inference | MIT, `third_party/llama.cpp/LICENSE` and `app/src/main/assets/licenses/LICENSE-llama.cpp.txt` |
| Geist, Newsreader, JetBrains Mono | `app/src/main/res/font` | SIL OFL 1.1, `app/src/main/assets/licenses/OFL-*.txt` |
| Material Symbols | `app/src/main/res/drawable/ic_*.xml` | Apache-2.0 |
| Bundled model file (`assets/hw-core.gguf`, fetched at build time) | APK assets | Apache-2.0 |

The texts the app shows under Profile & settings → Open-source licenses are the files in
`app/src/main/assets/licenses/` (including `NOTICE.txt`, the derived-from notice).
