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

## Earlier lessons
- Show the design in the preview before building (builds cost the owner 5-15 minutes).
- Never reveal the model or how it works in any user-facing text, error, or status.
- Gradle Kotlin DSL: import java.net / java.security at the top; bare `java.` is the java extension.
- Kotlin `by` delegation needs getValue/setValue imports (don't prune them).
