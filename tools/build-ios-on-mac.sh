#!/bin/bash
# Human Wonder for iPhone: build the app on this Mac as an .ipa file for SideStore.
#
#   Double-click "Build Human Wonder for iPhone.command" (or: bash tools/build-ios-on-mac.sh)
#
# Needs Xcode from the Mac App Store (free). No admin password and no paid Apple developer
# account: SideStore signs the app with your own free Apple ID when it installs it on the iPhone.
# Reuses what the Android build already set up (llama.cpp source, CMake, the model file).
# Everything it prints also goes to tools/build-ios.log.
set -euo pipefail
{

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$HOME/human-wonder-android-tools"
LOG="$PROJECT/tools/build-ios.log"
IPA="$PROJECT/HumanWonder-iPhone.ipa"
LLAMA="$PROJECT/third_party/llama.cpp"
PIN="$(tr -d '[:space:]' 2>/dev/null < "$PROJECT/third_party/llama.cpp.commit" || true)"
MODEL_URL="https://huggingface.co/ggml-org/Qwen3-0.6B-GGUF/resolve/13ac06624948e4d8589cb4b6f90100837342fcb8/Qwen3-0.6B-Q4_0.gguf"
MODEL_SHA256="da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4"

mkdir -p "$PROJECT/tools"
exec > >(tee "$LOG") 2>&1

say()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m%s\033[0m\n' "$*"; }
die()  { printf '\n\033[1;31mERROR: %s\033[0m\n' "$*"; exit 1; }
free_gb() { df -g "$HOME" | awk 'NR==2 {print $4}'; }

fetch() { # fetch OUT URL
  rm -f "$1"
  curl -fL --connect-timeout 20 --retry 4 --retry-delay 3 --speed-limit 5000 --speed-time 120 -o "$1" "$2"
}

# 1. This Mac and Xcode --------------------------------------------------------------------------
say "Human Wonder for iPhone"
echo "    macOS $(sw_vers -productVersion) on $(uname -m), $(free_gb) GB free, $(date)"

find_xcode() {
  if xcodebuild -version >/dev/null 2>&1; then return 0; fi
  local x
  for x in /Applications/Xcode*.app "$HOME/Applications/Xcode"*.app; do
    [ -d "$x/Contents/Developer" ] || continue
    export DEVELOPER_DIR="$x/Contents/Developer" # uses this Xcode without changing any system setting
    if xcodebuild -version >/dev/null 2>&1; then return 0; fi
  done
  unset DEVELOPER_DIR
  return 1
}

if ! find_xcode; then
  say "Xcode is needed first (free, from the Mac App Store)"
  echo "  1. Make room: Xcode needs about 15 GB free while it installs. This Mac has $(free_gb) GB free."
  echo "     Apple menu > System Settings > General > Storage shows what takes the space."
  echo "     Build folders this project uses (safe to know about; the Android build needs them):"
  du -sh "$TOOLS" "$HOME/.gradle" "$HOME/Library/Caches" "$HOME/Library/Developer" 2>/dev/null | sed 's/^/       /' || true
  echo "  2. Install Xcode (the App Store page is opening now). Then open Xcode once and let it finish."
  echo "     You only need iOS; skip any extra simulator downloads it offers."
  echo "  3. Double-click \"Build Human Wonder for iPhone\" again."
  open "macappstore://apps.apple.com/app/xcode/id497799835" 2>/dev/null || true
  exit 0
fi
XCODE_APP="$(cd "$(xcode-select -p 2>/dev/null)/../.." 2>/dev/null && pwd || true)"
[ -n "${DEVELOPER_DIR:-}" ] && XCODE_APP="${DEVELOPER_DIR%/Contents/Developer}"
echo "    $(xcodebuild -version | tr '\n' ' ')at $XCODE_APP"

if ! xcodebuild -license check >/dev/null 2>&1 || ! xcodebuild -checkFirstLaunchStatus >/dev/null 2>&1; then
  say "Xcode needs to finish setting up (one time)"
  echo "  Xcode is opening: agree to the licence and let it install its components."
  echo "  You only need iOS; skip any extra simulator downloads. Then quit Xcode and double-click again."
  open -a "$XCODE_APP" 2>/dev/null || open -a Xcode 2>/dev/null || true
  exit 0
fi

# Xcode 26 downloads its Metal compiler separately; the chat's aurora is a Metal shader.
METAL=1
if ! xcrun -sdk iphoneos metal --version >/dev/null 2>&1; then
  say "Getting Xcode's Metal compiler (one time, about 700 MB)"
  xcodebuild -downloadComponent MetalToolchain || true
  if ! xcrun -sdk iphoneos metal --version >/dev/null 2>&1; then
    METAL=0
    warn "    No Metal compiler: the chat's aurora is drawn the simpler way in this build."
  fi
fi

[ "$(free_gb)" -ge 3 ] || die "The build needs about 3 GB free and this Mac has $(free_gb) GB. Free some space and double-click again."

# 2. XcodeGen (turns ios/project.yml into the Xcode project) --------------------------------------
XG="$(find "$TOOLS/xcodegen-dist" -type f -name xcodegen -perm -u+x 2>/dev/null | head -n 1 || true)"
if [ -z "$XG" ]; then
  say "Getting XcodeGen (one time, a few MB)"
  mkdir -p "$TOOLS"
  if [ -f "$PROJECT/tools/xcodegen.zip" ]; then
    # A copy placed next to this script (GitHub downloads can stall on this Mac's connection).
    cp "$PROJECT/tools/xcodegen.zip" "$TOOLS/xcodegen.zip"
  else
    fetch "$TOOLS/xcodegen.zip" "https://github.com/yonaskolb/XcodeGen/releases/latest/download/xcodegen.zip" \
      || die "Couldn't download XcodeGen. Check the internet connection and double-click again."
  fi
  rm -rf "$TOOLS/xcodegen-dist"
  mkdir -p "$TOOLS/xcodegen-dist"
  unzip -q "$TOOLS/xcodegen.zip" -d "$TOOLS/xcodegen-dist"
  rm -f "$TOOLS/xcodegen.zip"
  XG="$(find "$TOOLS/xcodegen-dist" -type f -name xcodegen -perm -u+x | head -n 1 || true)"
  [ -n "$XG" ] || die "The XcodeGen download looks incomplete; double-click again."
fi
echo "    XcodeGen $("$XG" --version 2>/dev/null | tail -n 1)"

# 3. llama.cpp for iPhone ---------------------------------------------------------------------------
XCF="$LLAMA/build-apple/llama.xcframework"
[ -f "$LLAMA/build-xcframework.sh" ] || die "llama.cpp isn't in this folder yet: build the Android app once (it fetches it), then try again."
if [ ! -f "$XCF/Info.plist" ] || [ "$(cat "$LLAMA/build-apple/.hw-commit" 2>/dev/null || true)" != "$PIN" ]; then
  say "Building llama.cpp for iPhone (one time, about 10-15 minutes)"
  if ! command -v cmake >/dev/null 2>&1; then
    CMAKE_BIN="$(ls -d "$TOOLS"/android-sdk/cmake/*/bin 2>/dev/null | sort | tail -n 1 || true)"
    [ -n "$CMAKE_BIN" ] && export PATH="$CMAKE_BIN:$PATH"
  fi
  command -v cmake >/dev/null 2>&1 || die "CMake is missing: build the Android app once (it installs CMake), then try again."
  rm -rf "$LLAMA/build-apple" "$LLAMA/build-ios-device"
  if ! (cd "$LLAMA" && bash ./build-xcframework.sh ios-device) > "$PROJECT/tools/build-ios-llama.log" 2>&1; then
    tail -n 30 "$PROJECT/tools/build-ios-llama.log"
    die "llama.cpp didn't build for iPhone. The details are in tools/build-ios-llama.log."
  fi
  echo "$PIN" > "$LLAMA/build-apple/.hw-commit"
  rm -rf "$LLAMA/build-ios-device" # the intermediate build; the framework is all the app needs
fi
echo "    llama.cpp ${PIN:0:7} framework ready"

# 4. The model (the same file as the Android app's) --------------------------------------------------
MODEL="$PROJECT/ios/HumanWonder/Model/hw-core.gguf"
mkdir -p "$(dirname "$MODEL")"
if [ ! -f "$MODEL" ] || [ "$(shasum -a 256 "$MODEL" | awk '{print $1}')" != "$MODEL_SHA256" ]; then
  rm -f "$MODEL"
  SRC="$PROJECT/app/build/hw-model/assets/hw-core.gguf"
  if [ -f "$SRC" ] && [ "$(shasum -a 256 "$SRC" | awk '{print $1}')" = "$MODEL_SHA256" ]; then
    ln "$SRC" "$MODEL" 2>/dev/null || cp "$SRC" "$MODEL" # a hard link takes no extra space
  else
    say "Downloading the sky (one time, about 430 MB)"
    fetch "$MODEL" "$MODEL_URL" || die "Couldn't download it. Check the internet connection and double-click again."
    [ "$(shasum -a 256 "$MODEL" | awk '{print $1}')" = "$MODEL_SHA256" ] || die "The download was damaged; double-click again."
  fi
fi
echo "    model ready"

# 5. The app -----------------------------------------------------------------------------------------
say "Building Human Wonder for iPhone (a few minutes)"
cd "$PROJECT/ios"
SPEC="project.yml"
if [ "$METAL" = 0 ]; then
  sed -e 's#"UI/Aurora.metal.excluded"#"UI/Aurora.metal"#' -e 's#HW_METAL\]#HW_NO_METAL]#' project.yml > project.generated.yml
  SPEC="project.generated.yml"
fi
"$XG" generate --spec "$SPEC" --quiet || die "XcodeGen couldn't make the Xcode project (see above)."
rm -rf "$PROJECT/ios/build/ipa"
if ! xcodebuild -project HumanWonder.xcodeproj -scheme HumanWonder -configuration Release \
    -sdk iphoneos -destination "generic/platform=iOS" -derivedDataPath "$PROJECT/ios/build/derived" \
    CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" build \
    > "$PROJECT/tools/build-ios-xcode.log" 2>&1; then
  grep -E "error:" "$PROJECT/tools/build-ios-xcode.log" | sort -u | head -n 40 || true
  die "The iPhone app didn't build. The details are in tools/build-ios-xcode.log."
fi

APP="$PROJECT/ios/build/derived/Build/Products/Release-iphoneos/HumanWonder.app"
[ -d "$APP" ] || die "The build finished but the app is missing."
mkdir -p "$PROJECT/ios/build/ipa/Payload"
cp -R "$APP" "$PROJECT/ios/build/ipa/Payload/"
rm -f "$IPA"
(cd "$PROJECT/ios/build/ipa" && zip -qry "$IPA" Payload)
rm -rf "$PROJECT/ios/build/ipa"

say "Human Wonder for iPhone is ready: $IPA ($(du -h "$IPA" | awk '{print $1}'))"
echo "  Put it on the iPhone with SideStore, using your own free Apple ID (see the steps in the chat)."
open -R "$IPA" 2>/dev/null || true
exit 0
}
