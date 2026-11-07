# Design preview (Claude Design canvas)

Live canvas: https://claude.ai/artifact/Qo42XvUTr9HFJnjETQmNC6 (slug `c0b04488-5a95-4a93-a2de-52267f7dfd47`).

`project/` holds the canvas's source files, one `.dc.html` per screen, in the same order as the app:

| Artboard | App screen |
|---|---|
| `Main.dc.html` | Landing (`OnboardingScreens.kt`, `LandingScreen`) |
| `Name.dc.html` | Name (`OnboardingScreens.kt`, `NameScreen`) |
| `Chat.dc.html` | Chat, live and interactive (`ChatScreen.kt`) |
| `Transmission.dc.html` | Same chat code, auto-playing a conversation |

The preview uses sample answers (the `topics()` list in the chat artboards); the app answers with
the on-device model. Photos and fonts are uploaded to the canvas as assets (`/_blob/...` URLs).

Keep the preview and the app in step:

1. Change `ChatScreen.kt` / `Theme.kt` and the matching markup or logic in `Chat.dc.html` and
   `Transmission.dc.html` (the two share the same code; only `var demo = true/false` differs).
2. The sky shader lives in `GalaxyBackground.kt` only. After changing it, run
   `python3 design/preview/sync_shader.py`.
3. `node design/preview/test_logic.js` checks the chat logic.
4. Republish the changed files to the canvas (one file per publish, `project/<name>`).

## Screenshot of every screen

`python3 design/preview/tour.py` renders all seven screens side by side at 393 x 852 dp (text 100%), with the live sky, star trails and the chat demo running, into `design/preview/test-renders/tour.png`. Use `render_test.py` for the size and font-scale stress test.
