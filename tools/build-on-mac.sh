#!/bin/bash
# Human Wonder POC: build the Android app on this Mac and install it on a USB-connected phone.
#
#   bash ~/Desktop/stitch-mcp/human-wonder-android/tools/build-on-mac.sh
#
# No Android Studio and no admin password needed. Everything goes into one folder,
# ~/human-wonder-android-tools (delete it to remove all of it). Re-running is safe: finished
# steps are skipped.
set -euo pipefail

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$HOME/human-wonder-android-tools"
export ANDROID_HOME="$TOOLS/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
NDK_VERSION="29.0.14206865"
CMAKE_VERSION="3.31.6"
APK_OUT="$PROJECT/HumanWonder-POC.apk"

say()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m%s\033[0m\n' "$*"; }
die()  { printf '\n\033[1;31mERROR: %s\033[0m\n' "$*"; exit 1; }

case "$(uname -m)" in
  arm64)  JDK_ARCH="aarch64" ;;
  x86_64) JDK_ARCH="x64" ;;
  *) die "Unsupported Mac CPU: $(uname -m)" ;;
esac

[ -f "$PROJECT/settings.gradle.kts" ] || die "Can't find the project next to this script ($PROJECT)."
[ -f "$PROJECT/third_party/llama.cpp/CMakeLists.txt" ] || die "third_party/llama.cpp is missing. If you cloned from GitHub, use: git clone --recursive ..."

cat <<EOF

Human Wonder POC: Android build
-------------------------------
This will:
  1. Download a Java 17 runtime (Eclipse Temurin, about 190 MB) unless you already have one.
  2. Download Google's Android command-line tools and ask YOU to accept the Android SDK licenses.
  3. Install the Android SDK pieces the app needs (about 2-3 GB download, ~5 GB on disk) into:
       $TOOLS
  4. Build the app (the first build compiles llama.cpp: about 10-20 minutes).
  5. Install it on your phone if one is connected by USB with USB debugging on.

EOF
read -r -p "Continue? [y/N] " answer
case "$answer" in y|Y|yes|YES) ;; *) echo "Cancelled."; exit 0 ;; esac

mkdir -p "$TOOLS"
free_gb=$(df -g "$HOME" | awk 'NR==2 {print $4}')
if [ "${free_gb:-0}" -lt 7 ]; then
  warn "Only ${free_gb} GB free on this disk; about 7 GB is recommended."
  read -r -p "Try anyway? [y/N] " a2
  case "$a2" in y|Y) ;; *) exit 1 ;; esac
fi

# 1. Java 17 -----------------------------------------------------------------------------------
if JH=$(/usr/libexec/java_home -v 17 2>/dev/null); then
  export JAVA_HOME="$JH"
  say "Using your installed Java 17: $JAVA_HOME"
elif [ -x "$TOOLS/jdk/Contents/Home/bin/java" ]; then
  export JAVA_HOME="$TOOLS/jdk/Contents/Home"
  say "Using the Java 17 downloaded earlier"
else
  say "Downloading Java 17 (Eclipse Temurin, $JDK_ARCH)"
  curl -fL --retry 3 -o "$TOOLS/jdk.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/17/ga/mac/$JDK_ARCH/jdk/hotspot/normal/eclipse"
  rm -rf "$TOOLS/jdk-extract" && mkdir -p "$TOOLS/jdk-extract"
  tar -xzf "$TOOLS/jdk.tar.gz" -C "$TOOLS/jdk-extract"
  mv "$TOOLS"/jdk-extract/* "$TOOLS/jdk"
  rm -rf "$TOOLS/jdk-extract" "$TOOLS/jdk.tar.gz"
  export JAVA_HOME="$TOOLS/jdk/Contents/Home"
fi
"$JAVA_HOME/bin/java" -version 2>&1 | head -1

# 2. Android command-line tools ---------------------------------------------------------------
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  say "Finding Google's latest Android command-line tools"
  ZIP=$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml \
        | grep -o 'commandlinetools-mac-[0-9]*_latest\.zip' | sort -t- -k3 -n | tail -1)
  [ -n "$ZIP" ] || die "Could not find the command-line tools download on dl.google.com"
  say "Downloading $ZIP"
  curl -fL --retry 3 -o "$TOOLS/cmdline-tools.zip" "https://dl.google.com/android/repository/$ZIP"
  rm -rf "$TOOLS/cmdline-extract" && mkdir -p "$TOOLS/cmdline-extract" "$ANDROID_HOME/cmdline-tools"
  unzip -q "$TOOLS/cmdline-tools.zip" -d "$TOOLS/cmdline-extract"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$TOOLS/cmdline-extract/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$TOOLS/cmdline-extract" "$TOOLS/cmdline-tools.zip"
fi

# 3. Licenses (you decide) and SDK packages -----------------------------------------------------
say "Android SDK licenses: please read them and answer 'y' to accept"
"$SDKMANAGER" --licenses

say "Installing SDK packages (platform-tools, Android 16 platform, NDK $NDK_VERSION, CMake $CMAKE_VERSION)"
"$SDKMANAGER" --install "platform-tools" "platforms;android-36" "ndk;$NDK_VERSION" "cmake;$CMAKE_VERSION"

# 4. Build ----------------------------------------------------------------------------------------
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$PROJECT/local.properties"
chmod +x "$PROJECT/gradlew"
say "Building the app (first time: about 10-20 minutes)"
( cd "$PROJECT" && ./gradlew :app:assembleRelease --no-daemon --console=plain )
cp "$PROJECT/app/build/outputs/apk/release/app-release.apk" "$APK_OUT"
say "APK ready: $APK_OUT"

# 5. Install on a connected phone -----------------------------------------------------------------
ADB="$ANDROID_HOME/platform-tools/adb"
"$ADB" start-server >/dev/null 2>&1 || true
devices=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
unauthorized=$("$ADB" devices | awk 'NR>1 && $2=="unauthorized" {print $1}')
if [ -n "$devices" ]; then
  for d in $devices; do
    say "Installing on phone $d"
    "$ADB" -s "$d" install -r "$APK_OUT"
    "$ADB" -s "$d" shell am start -n ai.humanwonder.poc/.MainActivity >/dev/null 2>&1 || true
  done
  say "Done. The app is open on your phone."
elif [ -n "$unauthorized" ]; then
  warn "A phone is connected but hasn't allowed this Mac yet. Unlock it, tap 'Allow' on the USB debugging prompt, then run:"
  echo "  \"$ADB\" install -r \"$APK_OUT\""
else
  warn "No phone found over USB. Either:"
  echo "  - connect the phone with USB debugging on and run: \"$ADB\" install -r \"$APK_OUT\""
  echo "  - or send $APK_OUT to the phone (AirDrop won't work; use Google Drive, WhatsApp, or a cable) and open it there."
fi
