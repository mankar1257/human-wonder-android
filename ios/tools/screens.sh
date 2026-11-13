#!/bin/bash
# Screen checks for the iPhone app: opens every screen in the iOS simulator (the app's -hwDemo
# scenes) and saves a screenshot of each, plus one real question to Orbit 412 and its answer.
#
#   ios/tools/screens.sh <path to HumanWonder.app built for the simulator> <output folder>
set -uo pipefail
APP="$1"
OUT="$2"
BID="ai.humanwonder.orbit"
mkdir -p "$OUT"

# The on-screen keyboard only shows when no hardware keyboard is "connected".
defaults write com.apple.iphonesimulator ConnectHardwareKeyboard -bool false || true

run_device() {
  local kind="$1" folder="$2" scenes="$3" textsize="${4:-}"
  local udid
  udid=$(python3 ios/tools/pick_sim.py "$kind" 2> "$OUT/$folder.device.txt")
  mkdir -p "$OUT/$folder"
  xcrun simctl boot "$udid" 2>/dev/null || true
  xcrun simctl bootstatus "$udid" -b > /dev/null
  xcrun simctl status_bar "$udid" override --time "9:41" --batteryState charged --batteryLevel 100 \
    --cellularMode active --cellularBars 4 --wifiBars 3 2>/dev/null || true
  if [ -n "$textsize" ]; then xcrun simctl ui "$udid" content_size "$textsize" || true; fi
  xcrun simctl install "$udid" "$APP"
  for scene in $scenes; do
    xcrun simctl terminate "$udid" "$BID" 2>/dev/null || true
    xcrun simctl launch "$udid" "$BID" -hwDemo "$scene" > /dev/null
    case "$scene" in
      *keyboard) sleep 6 ;;
      side-menu) sleep 5 ;;
      *) sleep 4 ;;
    esac
    xcrun simctl io "$udid" screenshot "$OUT/$folder/$scene.png" > /dev/null 2>&1 || echo "no screenshot for $scene"
  done
  echo "$udid"
}

ALL="landing-first landing-back name name-keyboard chat-empty chat-keyboard chat-conversation chat-conversation-keyboard side-menu profile licenses"

UDID=$(run_device large 1-iphone "$ALL" | tail -1)

# One real answer from the model (CPU in the simulator, so it takes a while).
xcrun simctl terminate "$UDID" "$BID" 2>/dev/null || true
xcrun simctl launch "$UDID" "$BID" -hwDemo ask > /dev/null
DATA=$(xcrun simctl get_app_container "$UDID" "$BID" data)
for i in $(seq 1 60); do
  [ -f "$DATA/Documents/hw-demo-answer.txt" ] && break
  sleep 5
done
sleep 2
xcrun simctl io "$UDID" screenshot "$OUT/1-iphone/ask-answer.png" > /dev/null 2>&1 || true
cp "$DATA/Documents/hw-demo-answer.txt" "$OUT/answer.txt" 2>/dev/null || echo "no answer within 5 minutes" > "$OUT/answer.txt"
xcrun simctl shutdown "$UDID" || true

# The biggest text the app allows (it caps the phone's text size), and a small iPhone.
run_device large 2-largest-text "landing-back name-keyboard chat-empty chat-conversation profile" accessibility-extra-extra-extra-large > /dev/null
xcrun simctl shutdown all || true
run_device small 3-small-iphone "landing-first name name-keyboard chat-empty chat-keyboard chat-conversation side-menu profile" > /dev/null
xcrun simctl shutdown all || true
ls -R "$OUT"
