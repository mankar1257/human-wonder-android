# Lessons (do not repeat)

## 2026-11-07: "noob mistakes" on the real phone
What happened: screens were designed and checked only in a 390-wide web canvas at 100% text.
On the phone (different width and font scale) headlines were too big, spacing/wording felt off,
and the composer row overflowed so the send button was squeezed into the corner.

Rules from now on:
1. Every screen is checked at 360, 393 and 412 dp widths and at font scale 1.0, 1.3 and 1.5
   (Material 3 R4.4) in the preview AND reasoned through in Compose before asking for a build.
2. Fixed-size, never-shrinking primary actions (send, mic, CTA). Flexible items (chips, labels)
   shrink, wrap or collapse to icons first. No row may depend on everything fitting.
3. Display text uses scale-aware sizes (cap very large headlines; balanced line breaks).
4. Run the skills, not just read them: design-critique + accessibility-review on screenshots of
   each screen before showing; ux-copy for every string; Material 3 rules for touch targets/contrast.
5. Ask for the phone model and screenshots; use Oh My Android to see the real device.
6. The app must match the preview exactly; the preview must be tested at the same sizes.

## 2026-11-07: "too fancy and childish"
What happened: the Name screen (star trails swirling round the pole star over a dune silhouette,
a glowing satellite with a label) and Home (aurora streaks, a glowing Orbit mark on every row,
sparkle icons, gradient avatar) read as childish. The owner wants professional and clean, yet
rich and premium.

Rules from now on:
1. Spend boldness in one place per screen: one real photograph (or the chat's aurora), never
   stacked effects. Everything else plain black, Geist, one teal action.
2. No decorative motion or glow on reading screens; brand mark only in the header lockup.
3. Check "would a top-tier first-party app ship this?" before showing a screen.

## 2026-11-07: v2 first build and the owner's network
- GitHub's file servers (release downloads, objects CDN) crawl at ~20 KB/s or time out on the
  owner's connection, while Amazon/Google/CloudFront run at ~3 MB/s. Never depend on a GitHub
  download in the build: Java comes from Corretto first; llama.cpp is delivered by me (git fetch
  in the cloud + bridge); Gradle is fetched by the script from mirrors and checked against
  gradle.org's SHA-256, then placed where the wrapper looks (tools/WrapperHash.java).
- The build script is one { ... } block so it can be updated while a build runs, and it logs to
  tools/build.log so progress can be read through the bridge without asking the owner.
- device_commit_files can deliver a stale copy when the same staged path is reused: give every
  staged file a new name, and always md5-check on the Mac after committing.

## 2026-11-08: "all the words seem a little big"
The type scale was a step too large for a premium feel (display 40-42sp, body 16sp). Now:
display 34/40, name question 28/34, answers 15/24, sub-lines 14/21, row/titles 15/21,
menu/segments 14/20, chips 13/18, labels 12/17 (never below 12sp). Same steps in the preview
(font-size/line-height pairs mapped in every .dc.html and generator). Premium = restrained type.
A phone's own Font size setting also scales everything: the app now follows it only up to 1.15x
(MainActivity caps LocalDensity.fontScale), after "the text takes too much space, I can't read
anything" on the owner's phone.

## 2026-11-08: "fix it properly, we need to make sure it works"
Text crowding has three causes, fixed together: (1) the phone's Font size (capped at 1.15x),
(2) Display size / Screen zoom, which shrinks the dp width (layout never narrower than 360dp;
both in HumanWonderScale), (3) the keyboard: with it open the Name and empty-chat headlines had
nowhere to go and drew over the input (FitColumn drops sub-line then headline; the chat's tall
composer, suggestions and footer step aside on short space). "Make sure it works" means
pictures: the Mac build now takes two screenshots on the phone (plus its font_scale and wm
density) and draws every screen at 5 phone set-ups into screens-check/. The cloud sandbox and
the Mac-side VM have no Maven/Google access, so the Mac's own build is the only compiler: get an
independent compile review of main-code changes before asking the owner to build.

## Earlier lessons
- Show the design in the preview before building (builds cost the owner 5-15 minutes).
- Never reveal the model or how it works in any user-facing text, error, or status.
- Gradle Kotlin DSL: import java.net / java.security at the top; bare `java.` is the java extension.
- Kotlin `by` delegation needs getValue/setValue imports (don't prune them).
