# Memory (Human Wonder workspace)

Read this first in every session in this folder. Deep detail lives in `memory/`.
The project skill is `skills/human-wonder-dev/SKILL.md` (copy it into `.claude/skills/` so Claude Code
auto-loads it: `mkdir -p .claude/skills && cp -R skills/human-wonder-dev .claude/skills/`). Load it before any app work.

## Me
Owner of **Human Wonder**. Mac user `mankarvaibhav`, GitHub `mankar1257`, timezone IST.
Building an Android POC to sell the Human Wonder idea; tests on a real Android phone.

## Terms
| Term | Meaning |
|------|---------|
| **Human Wonder** | The brand/product. Must be visible on every screen |
| **Orbit 412** | The voice of the app: the Human Wonder satellite, 412 km up. The assistant's only identity |
| **the preview / demo screens** | Claude Design canvas https://claude.ai/artifact/Qo42XvUTr9HFJnjETQmNC6 (source: `human-wonder-android/design/preview/`) |
| **the build** | Double-click `human-wonder-android/Build Human Wonder.command` -> `HumanWonder-POC.apk` (~500 MB) |
| **Stitch** | Google Stitch designs in `design/stitch/` (original screens, DESIGN.md) |
| **SLM** | The on-device model (Qwen3-0.6B Q4_0 via llama.cpp). NEVER named or hinted at in the app |
| **POC** | Proof of concept: free tools only, no paid services |
| → Full glossary | `memory/glossary.md` |

## Projects
| Name | What |
|------|------|
| **Human Wonder Android** | `human-wonder-android/` + GitHub `mankar1257/human-wonder-android`. Details: `memory/projects/human-wonder-android.md` |

## Preferences (hard rules, learned the hard way)
- The app on the phone must look **exactly** like the preview. Never show one thing and ship another.
- Use the design skills **before** showing work, every time: design-critique, accessibility-review,
  ux-copy, Material 3 (ehmo android guidelines), mobile-design-system. Test every screen at
  360 / 393 / 412 dp widths and font scale 1.0 / 1.3 / 1.5. See `memory/context/lessons.md`.
- Premium, rich, colourful, alive. Not "basic", not "noob". Be critical and creative; research.
- Never say on-device / offline / AI / model / SLM in the app. Sell Human Wonder everywhere.
- No dot grid over photos; no dotted lines across screens.
- Persist memory and skills **locally in this folder** (and in the repo), not only in a cloud sandbox.
- Free only (GitHub Actions is billing-locked; build on the Mac).
- Ask for phone screenshots when something "looks wrong"; check on the device (Oh My Android).
