# Install Human Wonder on your iPhone

*For testers with a Mac and an iPhone. Free, with your own Apple ID. About 25 minutes the first time; later updates take 2 minutes and no cable.*

You will receive one file from us: **HumanWonder-iPhone.ipa** (about 400 MB). Apple only lets apps from outside the App Store run if they are signed with an Apple ID, so you will use two free tools to sign it with yours:

- **iloader** on your Mac, used once with a cable to put SideStore on your iPhone.
- **SideStore** on your iPhone, which installs and renews the app over Wi-Fi from then on.

## What you need

- An iPhone on **iOS 17 or later**, with at least 2 GB free.
- A Mac and the iPhone's charging cable.
- Your Apple ID email and password, with your two-factor code device nearby. A free Apple ID is enough; some people prefer to use a spare one.
- Wi-Fi on both devices.
- The file **HumanWonder-iPhone.ipa**.

## Part 1. On the iPhone: install LocalDevVPN (2 minutes)

1. Open the **App Store**, search for **LocalDevVPN** and install it.
2. Open LocalDevVPN and tap **Connect**. When iOS asks to add a VPN configuration, tap **Allow** and enter your passcode.

LocalDevVPN does not route your internet traffic. SideStore needs it switched on whenever it installs or renews an app.

## Part 2. On the Mac: put SideStore on the iPhone (10 minutes, cable needed)

3. In a browser, go to **github.com/nab138/iloader/releases**. Under the newest release, download the file ending in **darwin-universal.dmg**.
4. Open the downloaded file and drag **iloader** into **Applications**. Open it from Applications.
   - If macOS says it cannot be opened, go to **System Settings → Privacy & Security**, scroll down and click **Open Anyway**.
5. Connect the iPhone to the Mac with the cable. Unlock the iPhone. If it asks **Trust This Computer?**, tap **Trust** and enter your passcode.
6. In iloader, **sign in with your Apple ID** and enter the two-factor code when asked.
7. Choose your iPhone, then click **Install SideStore (Stable)**. Wait until iloader says it is done (a few minutes). Keep the cable connected until then.

## Part 3. On the iPhone: allow SideStore to run (5 minutes)

8. **Trust your Apple ID as a developer:** Settings → General → **VPN & Device Management** → tap your Apple ID under "Developer App" → **Trust**. The iPhone needs internet for this.
9. **Turn on Developer Mode:** Settings → **Privacy & Security** → scroll to the bottom → **Developer Mode** → switch it on → **Restart**. After the restart, tap **Turn On** and enter your passcode.
10. Open **LocalDevVPN** and tap **Connect** (if it is not connected already).
11. Open **SideStore**. Allow it to find devices on the local network and to send notifications.
12. In SideStore, **sign in with the same Apple ID** you used in iloader.
13. Go to **My Apps** and tap the **7 DAYS** button next to SideStore. This finishes the setup.

You can unplug the cable now. You will not need the Mac again unless SideStore itself expires (see "Keeping it working").

## Part 4. Install Human Wonder (5 minutes, no cable)

14. Put **HumanWonder-iPhone.ipa** on the iPhone, using either way:
    - **From the Mac:** right-click the file → Share → **AirDrop** → your iPhone. If it asks where to open or save it, choose **Save to Files**.
    - **From a link we sent:** open the link on the iPhone and save the file to **Files**.
15. Check that **LocalDevVPN** is connected.
16. Open **SideStore** → **My Apps** → tap **+** (top left) → choose **HumanWonder-iPhone.ipa**. The file is large, so installing takes a few minutes. Keep SideStore open until it finishes.
17. Open **Human Wonder** from the Home Screen. The first start can take a little longer while it gets ready. After that, it works fully offline.

## Keeping it working

- **Every 7 days** an app signed with a free Apple ID must be renewed. SideStore does this by itself in the background while LocalDevVPN is connected and the iPhone is on Wi-Fi. To be sure, once a week open SideStore → **My Apps** → **Refresh All**.
- **If Human Wonder will not open**, it has probably expired: connect LocalDevVPN, open SideStore, and refresh it.
- **If SideStore itself has expired and will not open**, repeat Part 2 (iloader, with the cable). Your apps and conversations stay.
- **Free Apple ID limits:** up to 3 apps installed this way at once (SideStore counts as one), and up to 10 new app IDs per week.
- **New versions:** when we send a new `.ipa`, repeat Part 4. It replaces the old version and keeps your conversations.

## If something goes wrong

| What you see | What to do |
|---|---|
| iloader: "Failed to fetch URL Bag" or another network error when signing in | Turn off any VPN or firewall app on the Mac, or try another network (a phone hotspot works), then sign in again. |
| iloader does not see the iPhone | Unlock the iPhone, tap Trust, try another cable or USB port. |
| iPhone: "Untrusted Developer" | Do step 8 (VPN & Device Management → Trust). |
| iPhone: "Developer Mode required" | Do step 9. |
| SideStore cannot install or refresh | Open LocalDevVPN and tap Connect, check you are on Wi-Fi, then try again. |
| SideStore: "Maximum number of App IDs" | You have hit the weekly free limit. Wait a few days, or remove an app you installed more than 7 days ago. |
| The app opened before but now closes at once | It expired: refresh it in SideStore (LocalDevVPN on). |

## About your Apple ID

iloader and SideStore use your Apple ID only to ask Apple to sign the apps for your own iPhone. Every tester uses their own Apple ID; nobody else's account is needed. Human Wonder itself asks for no account and needs no internet.
