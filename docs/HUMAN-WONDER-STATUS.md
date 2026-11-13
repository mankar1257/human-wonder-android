# Human Wonder: status, builds and installs

*14 November 2026 (updated 00:05). Covers the Android app (v2), the iPhone app, how to build and install both without paid accounts, and the new photos in the chat (in progress).*

## In short

- **Android app (v2)** builds on the Mac with one double-click. The latest build (8 Nov, 11:31) has the smaller type, the unified look, the dunes welcome screen with the swipe-up, and the text-size and keyboard fixes. It has **not been installed on a phone yet**: no Android phone was connected during that build.
- **iPhone app** is built and **installed on an iPhone** with a free Apple ID (13 November). It is a SwiftUI version of the same app (same screens, fonts, icons, photos, Orbit 412 voice and the same model on the phone), built with Xcode 27.2 on macOS 27.2.1 as `HumanWonder-iPhone.ipa` (401 MB).
- **New, in progress: Orbit sends pictures with its answers** (the designer's request). The code for both apps is written and reviewed; it is waiting for the photos to be downloaded on the Mac (see the next section).
- **Installing on iPhone costs nothing:** the app is signed with a free Apple ID through iloader or SideStore. The catch: it must be renewed every 7 days and fits 3 sideloaded apps per iPhone.
- **GitHub is locked for billing** (since about 8 November), so GitHub's build machines run nothing for this account. Everything is built on the Mac instead. Pushing code still works.
- **Next:** double-click **Get Orbit Photos.command** on the Mac, then build both apps with the pictures.

## New: pictures from Orbit's archive (in progress)

The designer asked for pictures or GIFs with Orbit's answers. Orbit 412 now sends a "frame from its archive" under an answer when the question is about something it can show.

- **The pictures are real.** They are photographs taken by astronauts on the International Space Station, which flies at about 410 km, almost exactly Orbit's height. The "GIFs" are short loops cut from the station's time-lapses (aurora, a night pass over Arabia, one over India, lightning, the Milky Way, a sunrise). NASA material is free to use; the app credits NASA in the full-screen view and on the licences screen. Orbit calls them frames "from my archive", never photos it took itself.
- **32 pictures** (25 photos, 7 loops) cover what people ask Orbit about: the Gulf and the Emirates by day and night, the Empty Quarter dunes, India at night, the Himalayas and Everest, sunsets and the thin blue line, the Moon, hurricanes, lightning, the Milky Way, airglow, a volcano, the Maldives and the sea.
- **The app picks the picture, and the model presents it.** This was tested on the app's own model with 60 questions:

| How the picture is chosen | Right picture (50 questions that have one) |
|---|---|
| The model tags its answer with a picture name | 7 of 100 (it also invented names like "504") |
| The model answers a separate question that only allows a name or "none" | 3 of 50 |
| **The app's keyword rules** (English, Arabic, Hindi) | **50 of 50** (and none for all 10 that should get none) |

- **Better answers too.** The example answer in Orbit's instructions was being copied into 40% of all answers ("the coast glows like a string of gold beads", even about the Moon). It is gone. With a picture, the model is told what the picture shows, and the answer now describes it 46 times in 50, with no copying at all.
- **On screen:** the picture fades in under the answer once the words have arrived, with the line "From Orbit's archive" and the picture's title. Tap it for full screen with the credit. Loops play while on screen and hold still when the phone's animations are turned off. The picture is saved with the conversation.
- **What's left:** download the photos on the Mac (one double-click, below), choose the best few seconds of each time-lapse for the loops, build the app files (about 15 MB in total), then build both apps and check.

**To do on the Mac:** double-click **Get Orbit Photos.command** in `~/Desktop/stitch-mcp/human-wonder-v2/`. It downloads the 25 photos and about 470 small time-lapse frames from NASA's astronaut photo archive (eol.jsc.nasa.gov), roughly 150 to 300 MB. It keeps only resized copies (about 30 MB) in `media-src/` and writes `tools/frames/fetch.log`.

Details, numbers and the test method: `tools/frames/README.md` on the `ios` branch.

## 1. What changed this week

| Area | Change |
|---|---|
| Flow | Every launch opens on the Liwa dunes with Orbit 412 passing over. Swiping up leads to the name screen the first time, or straight into a new chat once a name is saved. Back from the chat returns to the dunes. |
| Look | One look everywhere: the chat's glass surface, its send button and its chips are used on every screen. Teal is used only for actions. |
| Type | One step smaller across the app: headlines 34, the name question 28, answers 15/24, labels never below 12. |
| Swipe-up | Three chevrons light up from the bottom (the top one teal), over a label with a slow light sweep. |
| Text size | The phone's Font size setting can make text at most 15% bigger; a large Display size can't squeeze the layout below a normal phone width. Pop-ups and menus follow the same rules. |
| Keyboard | With the keyboard open, headlines give up their sub-line first, then themselves, instead of drawing over the input. The chat's tall composer, suggestions and footer step aside on short screens. |
| Checks | Each Android build now draws all 10 screens on 5 phone set-ups (50 pictures in `screens-check/`). When a phone is connected, it also takes screenshots on that phone and records its Font size and Display size. |

## 2. Android app

**Build:** on the Mac, plug in the phone (USB debugging on, screen unlocked), then double-click **Build Human Wonder.command** in `~/Desktop/stitch-mcp/human-wonder-v2/`. The build installs and opens the app on the phone, then draws the screen-check pictures.

| Output | Where |
|---|---|
| App file | `HumanWonder-POC.apk` (about 480 MB; the model is inside) |
| Build log | `tools/build.log` |
| Screen pictures | `screens-check/` (and `screens-check/0-your-phone/` when a phone is connected) |

**Phone requirements:** Android 13 or newer, a 64-bit phone, and about 1 GB free.

**"There was a problem parsing the package"** when opening the APK means one of two things:

- the phone runs Android 12 or older (Settings > About phone > Android version), or
- the file hadn't finished downloading. Wait for the full download, then open it from the Files app, not from inside WhatsApp.

**Status:** built on 8 November with every change above. Not yet installed on a phone. The 50 screen-check pictures from that build exist but haven't been reviewed yet.

## 3. iPhone app

### What it is

A native iPhone version (SwiftUI, iOS 17 or newer) with the same screens and behaviour:

- the dunes welcome screen with the swipe-up and "Welcome back, *name*."
- the name screen
- the chat: aurora sky, Orbit 412's pass, streaming answers, Orbit/Ground, tone, suggestions, copy, voice input, read aloud and haptics
- the side menu with search, rename and delete
- Profile & settings, with export and clear-all
- the licences

It uses the same fonts, icons, photos, persona rules and the same model (run on the phone with llama.cpp, using the iPhone's graphics chip).

Differences are only where iPhone users expect them:

- confirm boxes and menus are the iPhone's own
- voice input uses the iPhone's on-phone speech recognition
- swiping in from the left edge goes back

The code is in the project folder under `ios/`, and on GitHub on the `ios` branch.

### Why it is not on the App Store or TestFlight

Both need the Apple Developer Program, which costs ₹8,700 a year in India. Until then the app is installed with a free Apple ID (section 4).

### Building it on the Mac

1. **Update macOS.** The Xcode in the App Store today (Xcode 27) needs macOS Tahoe 26.6 or later.
    - If you'd rather not update yet: Xcode 26.3 runs on macOS Sequoia 15.6 or later. It is a free download from developer.apple.com/download after signing in with an Apple ID.
2. **Install Xcode** from the Mac App Store (free). Keep about 15 GB free while it installs; this Mac had about 26 GB free on 13 November.
3. **Open Xcode once:** agree to the licence and let it finish. Only iOS is needed, so skip the extra simulator downloads it offers.
4. **Double-click Build Human Wonder for iPhone.command** in the project folder. The first build takes about 15–20 minutes (it builds llama.cpp for iPhone once); later builds take a few minutes. It produces `HumanWonder-iPhone.ipa` (about 400 MB).

What the build does on its own:

- downloads Xcode's Metal compiler if Xcode needs it
- downloads XcodeGen (a small free tool that creates the Xcode project)
- reuses the Android build's model file and CMake

It needs no admin password and no Apple developer account.

| Log | What it shows |
|---|---|
| `tools/build-ios.log` | every step |
| `tools/build-ios-xcode.log` | the app's compile output |
| `tools/build-ios-llama.log` | the llama.cpp build |

**Status:** built on 13 November at 22:12 (Xcode 27.2, macOS 27.2.1) on the first compile, with one harmless warning. The `.ipa` (401 MB) holds the app, the llama.cpp framework, the model, the fonts, icons, photos, the compiled sky shader and the licences. On this Mac's connection, downloads from GitHub stall, so the build now uses a copy of XcodeGen placed in `tools/`. Not yet installed on an iPhone; nothing has been tried on a phone yet.

## 4. Installing on an iPhone without paying

The free route signs the app with the tester's own Apple ID. Limits of a free Apple ID:

- the app runs for 7 days, then needs renewing
- up to 3 sideloaded apps per iPhone
- up to 10 different apps a week

Each tester signs with their own Apple ID, so any number of people can test.

**Once per iPhone:** Settings > Privacy & Security > **Developer Mode** > On, then restart. After the first install, Settings > General > **VPN & Device Management** > tap your Apple ID > **Trust**.

**Option A: quickest (renew from the Mac every 7 days)**

1. On the Mac, install **iloader** (free, from github.com/nab138/iloader/releases).
2. Connect the iPhone with a cable and tap Trust on the iPhone.
3. In iloader, sign in with your Apple ID, choose **Import IPA**, and pick `HumanWonder-iPhone.ipa`.

**Option B: renews itself on the iPhone (SideStore)**

1. On the iPhone, install **LocalDevVPN** from the App Store.
2. On the Mac, open iloader, sign in, and choose **Install SideStore (Stable)**.
3. On the iPhone, open LocalDevVPN and tap Connect. Then open SideStore, sign in with the same Apple ID, and refresh SideStore once (the "7 DAYS" badge).
4. Put `HumanWonder-iPhone.ipa` in the iPhone's Files app (AirDrop from the Mac works), then in SideStore tap **+** and pick it.
5. Keep LocalDevVPN connected now and then on Wi-Fi; SideStore renews the apps in the background.

**Avoid** websites offering "free developer certificates". They sign apps with other companies' certificates, Apple keeps cancelling them, and you'd be handing the app to strangers.

## 5. GitHub

- GitHub stopped running builds for this account with: *"The job was not started because your account is locked due to a billing issue."* The Android workflow has failed this way since about 8 November; the iPhone workflow failed the same way on 13 November.
- **To fix:** github.com/settings/billing (often a failed card or an unpaid bill). Builds for this public repository are free once the lock is gone.
- After that, the iPhone workflow (`.github/workflows/ios.yml`) builds the `.ipa` on GitHub's Macs and screenshots every screen in the iPhone simulator, including one real answer from Orbit 412. No Mac work is needed then.

| Branch | Holds | Latest commit |
|---|---|---|
| `main` | design preview, docs (this document), memory and skill | `4c92ed7` |
| `v2-offlinellm` | the Android app | `ba9b9f0` |
| `ios` | the Android app plus the iPhone app (`ios/`) and its build script | `e2d06a8` |

## 6. Where things are on the Mac

| What | Path |
|---|---|
| Project | `~/Desktop/stitch-mcp/human-wonder-v2/` |
| Android build | `Build Human Wonder.command` |
| iPhone build | `Build Human Wonder for iPhone.command` |
| Android app file | `HumanWonder-POC.apk` |
| iPhone app file (after the build) | `HumanWonder-iPhone.ipa` |
| Photo download for Orbit's archive | `Get Orbit Photos.command` (writes `media-src/`, log in `tools/frames/fetch.log`) |
| Picture list, rules and test results | `tools/frames/` (`README.md`, `frames.json`, `manifest.txt`) |
| Build tools (Android SDK, NDK, CMake, XcodeGen) | `~/human-wonder-android-tools/` |
| Memory and skill notes | `~/Desktop/stitch-mcp/memory/`, `~/Desktop/stitch-mcp/skills/` |
| This document | `~/Desktop/stitch-mcp/human-wonder-v2/docs/HUMAN-WONDER-STATUS.md` (and `.pdf`) |

## 7. Rules both apps keep

- Nothing leaves the phone: no internet use while chatting. The profile and conversations are stored only on the phone.
- The app never names the model or its maker, and never says "AI", "app", "model" or "on-device". It only presents itself as Human Wonder and Orbit 412.
- Free tools only. No paid accounts are needed to build or test.
- Pictures in the chat are NASA astronaut photographs that ship inside the app (nothing is downloaded on the phone). NASA is credited; Orbit calls them frames from its archive.

## 8. Next steps

- [x] Update macOS and install Xcode (macOS 27.2.1, Xcode 27.2).
- [x] Build the iPhone app (`HumanWonder-iPhone.ipa`, 13 November).
- [x] Install on an iPhone (section 4) with a free Apple ID (13 November).
- [ ] Double-click **Get Orbit Photos.command** so the pictures for Orbit's answers can be built.
- [ ] Build both apps with the pictures and check them (Android: **Build Human Wonder**; iPhone: **Build Human Wonder for iPhone**, then install the new `.ipa` through SideStore).
- [ ] Send screenshots of each iPhone screen to check them against the Android app.
- [ ] Android: plug in the phone, run **Build Human Wonder**, and check the phone screenshots and the 50 screen-check pictures.
- [ ] Optional: clear the GitHub billing lock so builds and screen checks run on GitHub again.
- [ ] Later, for real users: the Apple Developer Program (App Store and TestFlight) and a Google Play listing.

*Sources: Apple's Xcode release notes and Xcode 27 requirements; Apple's free-account limits (developer.apple.com/support/compare-memberships); iloader (github.com/nab138/iloader); SideStore (docs.sidestore.io).*
