#!/bin/bash
# Human Wonder POC: build the Android app on this Mac and install it on a USB-connected phone.
#
#   bash ~/Desktop/stitch-mcp/human-wonder-android/tools/build-on-mac.sh
#
# No Android Studio and no admin password needed. Everything goes into one folder,
# ~/human-wonder-android-tools (delete it to remove all of it). Re-running is safe: finished
# steps are skipped.
set -euo pipefail
# The whole script is one { ... } block, so bash reads all of it before running: an update to
# this file while a build is running cannot change what that build does.
{

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$HOME/human-wonder-android-tools"
export ANDROID_HOME="$TOOLS/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
NDK_VERSION="29.0.14206865"
CMAKE_VERSION="3.31.6"
# compileSdk 37 (the OfflineLLM library set needs it). Newer sdkmanager builds may name it 37.0.
PLATFORM="platforms;android-37"
APK_OUT="$PROJECT/HumanWonder-POC.apk"
LLAMA_DIR="$PROJECT/third_party/llama.cpp"
LLAMA_PIN="$(tr -d '[:space:]' 2>/dev/null < "$PROJECT/third_party/llama.cpp.commit" || true)"

say()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m%s\033[0m\n' "$*"; }
die()  { printf '\n\033[1;31mERROR: %s\033[0m\n' "$*"; exit 1; }

# fetch_one OUT URL MODE: "quick" drops a source that runs under 150 KB/s for 30 s (so a slow
# mirror never holds the build up); "patient" keeps it unless it nearly stops (under 5 KB/s for
# 2 minutes).
fetch_one() {
  local out="$1" url="$2" opts
  if [ "$3" = quick ]; then
    opts=(--retry 1 --speed-limit 150000 --speed-time 30)
  else
    opts=(--retry 4 --retry-delay 3 --retry-all-errors --speed-limit 5000 --speed-time 120)
  fi
  rm -f "$out"
  echo "    from ${url%%\?*}"
  curl -fL --connect-timeout 20 "${opts[@]}" -o "$out" "$url"
}

# fetch OUT URL [URL...]: download OUT from the first source that works; every source but the last
# is tried "quick", the last one "patient".
fetch() {
  local out="$1" url
  shift
  while [ $# -gt 0 ]; do
    url="$1"
    shift
    if [ $# -gt 0 ]; then
      fetch_one "$out" "$url" quick && return 0
      warn "That source is too slow or failed; trying the next one."
    else
      fetch_one "$out" "$url" patient && return 0
    fi
  done
  return 1
}

case "$(uname -m)" in
  arm64)  JDK_ARCH="aarch64" ;;
  x86_64) JDK_ARCH="x64" ;;
  *) die "Unsupported Mac CPU: $(uname -m)" ;;
esac

[ -f "$PROJECT/settings.gradle.kts" ] || die "Can't find the project next to this script ($PROJECT)."
[ -n "$LLAMA_PIN" ] || die "third_party/llama.cpp.commit is missing; copy the whole project folder again."

cat <<EOF

Human Wonder POC: Android build
-------------------------------
This will:
  1. Download a Java 21 runtime (Eclipse Temurin, about 200 MB) unless you already have one.
  2. Download Google's Android command-line tools and ask YOU to accept the Android SDK licenses.
  3. Install the Android SDK pieces the app needs (about 2-3 GB download, ~5 GB on disk) into:
       $TOOLS
  4. Fetch the pinned llama.cpp source (about 60 MB) if this folder doesn't have it yet.
  5. Build the app. The first v2 build downloads the new build tools (Gradle 9, about 1 GB) and
     compiles llama.cpp: about 15-30 minutes. Later builds take a few minutes.
  6. Install it on your phone if one is connected by USB with USB debugging on.

EOF
# tools/.auto-continue (left by Claude when it starts a build for you) answers this once.
if [ -f "$PROJECT/tools/.auto-continue" ]; then
  rm -f "$PROJECT/tools/.auto-continue"
  answer=y
  echo "Continue? [y/N] y (started for you)"
else
  read -r -p "Continue? [y/N] " answer
fi
case "$answer" in y|Y|yes|YES) ;; *) echo "Cancelled."; exit 0 ;; esac

# Keep a copy of everything below in tools/build.log, so progress and errors can be read later.
mkdir -p "$PROJECT/tools"
exec > >(tee "$PROJECT/tools/build.log") 2>&1
echo "Build started $(date)"

mkdir -p "$TOOLS"
free_gb=$(df -g "$HOME" | awk 'NR==2 {print $4}')
if [ "${free_gb:-0}" -lt 8 ]; then
  warn "Only ${free_gb} GB free on this disk; about 8 GB is recommended."
  read -r -p "Try anyway? [y/N] " a2
  case "$a2" in y|Y) ;; *) exit 1 ;; esac
fi

# 1. Java 21 -----------------------------------------------------------------------------------
# v2 builds with Gradle 9 and Android Gradle Plugin 9, on Java 21 like OfflineLLM's own CI.
# (The Java 17 that v1 downloaded into $TOOLS/jdk is left alone; it is no longer used.)
if JH=$(/usr/libexec/java_home -v 21 2>/dev/null); then
  export JAVA_HOME="$JH"
  say "Using your installed Java 21: $JAVA_HOME"
elif [ -x "$TOOLS/jdk21/Contents/Home/bin/java" ]; then
  export JAVA_HOME="$TOOLS/jdk21/Contents/Home"
  say "Using the Java 21 downloaded earlier"
else
  say "Downloading Java 21 ($JDK_ARCH, about 200 MB)"
  # Three builds of the same OpenJDK 21; each unpacks to <name>/Contents/Home.
  case "$JDK_ARCH" in aarch64) CORRETTO_ARCH="aarch64" ;; *) CORRETTO_ARCH="x64" ;; esac
  fetch "$TOOLS/jdk21.tar.gz" \
    "https://corretto.aws/downloads/latest/amazon-corretto-21-$CORRETTO_ARCH-macos-jdk.tar.gz" \
    "https://aka.ms/download-jdk/microsoft-jdk-21-macos-$CORRETTO_ARCH.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/21/ga/mac/$JDK_ARCH/jdk/hotspot/normal/eclipse" \
    || die "Couldn't download Java 21 from any source. Check the internet connection and double-click again."
  rm -rf "$TOOLS/jdk21-extract" "$TOOLS/jdk21" && mkdir -p "$TOOLS/jdk21-extract"
  tar -xzf "$TOOLS/jdk21.tar.gz" -C "$TOOLS/jdk21-extract"
  mv "$TOOLS"/jdk21-extract/* "$TOOLS/jdk21"
  rm -rf "$TOOLS/jdk21-extract" "$TOOLS/jdk21.tar.gz"
  export JAVA_HOME="$TOOLS/jdk21/Contents/Home"
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
  fetch "$TOOLS/cmdline-tools.zip" "https://dl.google.com/android/repository/$ZIP" \
    || die "Couldn't download the Android command-line tools. Check the internet connection and double-click again."
  rm -rf "$TOOLS/cmdline-extract" && mkdir -p "$TOOLS/cmdline-extract" "$ANDROID_HOME/cmdline-tools"
  unzip -q "$TOOLS/cmdline-tools.zip" -d "$TOOLS/cmdline-extract"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$TOOLS/cmdline-extract/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$TOOLS/cmdline-extract" "$TOOLS/cmdline-tools.zip"
fi

# 3. Licenses (you decide) and SDK packages -----------------------------------------------------
say "Android SDK licenses: please read them and answer 'y' to accept"
"$SDKMANAGER" --licenses

say "Installing SDK packages (platform-tools, Android 17 platform, NDK $NDK_VERSION, CMake $CMAKE_VERSION)"
"$SDKMANAGER" --install "platform-tools" "ndk;$NDK_VERSION" "cmake;$CMAKE_VERSION"
if ! "$SDKMANAGER" --install "$PLATFORM"; then
  warn "sdkmanager has no '$PLATFORM'; trying '${PLATFORM}.0'."
  "$SDKMANAGER" --install "${PLATFORM}.0" || warn "Could not install the Android 17 platform now; Gradle will try to fetch it during the build."
fi

# 3b. llama.cpp at the pinned commit ------------------------------------------------------------
# The project folder may be a copy without the git submodule, or hold the older llama.cpp that
# v1 used. Fetch the exact commit OfflineLLM pins (as a GitHub tarball: no git needed).
llama_ok() {
  [ -f "$LLAMA_DIR/CMakeLists.txt" ] || return 1
  [ "$(tr -d '[:space:]' 2>/dev/null < "$LLAMA_DIR/.hw-llama-commit")" = "$LLAMA_PIN" ] && return 0
  # A git checkout at the right commit is fine too (only asked when Apple's git tools exist,
  # so the Mac never pops up an "install developer tools" dialog).
  if [ -e "$LLAMA_DIR/.git" ] && xcode-select -p >/dev/null 2>&1; then
    [ "$(git -C "$LLAMA_DIR" rev-parse HEAD 2>/dev/null)" = "$LLAMA_PIN" ] && return 0
  fi
  return 1
}
if llama_ok; then
  say "llama.cpp is at the pinned commit ${LLAMA_PIN:0:7}"
else
  say "Fetching llama.cpp ${LLAMA_PIN:0:7} (one time)"
  fetch "$TOOLS/llama.cpp.tar.gz" \
    "https://github.com/ggml-org/llama.cpp/archive/$LLAMA_PIN.tar.gz" \
    "https://codeload.github.com/ggml-org/llama.cpp/tar.gz/$LLAMA_PIN" \
    || die "Couldn't download the llama.cpp source. Check the internet connection and double-click again."
  rm -rf "$TOOLS/llama-extract" && mkdir -p "$TOOLS/llama-extract"
  tar -xzf "$TOOLS/llama.cpp.tar.gz" -C "$TOOLS/llama-extract"
  [ -f "$TOOLS/llama-extract/llama.cpp-$LLAMA_PIN/CMakeLists.txt" ] || die "The llama.cpp download looks incomplete; run this again."
  if [ -e "$LLAMA_DIR" ]; then
    rm -rf "$LLAMA_DIR.previous"
    mv "$LLAMA_DIR" "$LLAMA_DIR.previous"
  fi
  mkdir -p "$(dirname "$LLAMA_DIR")"
  mv "$TOOLS/llama-extract/llama.cpp-$LLAMA_PIN" "$LLAMA_DIR"
  printf '%s\n' "$LLAMA_PIN" > "$LLAMA_DIR/.hw-llama-commit"
  rm -rf "$TOOLS/llama-extract" "$TOOLS/llama.cpp.tar.gz" "$LLAMA_DIR.previous"
fi

# 3c. Gradle itself -----------------------------------------------------------------------------
# services.gradle.org now hands the download to GitHub's file servers, which can crawl or time out
# on some connections (the wrapper gives up after 10 s). So fetch the zip here: the official
# SHA-256 comes from gradle.org, the bytes from the first source that is fast, and the zip goes
# where the wrapper looks first.
WRAPPER_PROPS="$PROJECT/gradle/wrapper/gradle-wrapper.properties"
GRADLE_URL=$(sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPS" | sed 's/\\:/:/g' | tr -d '\r')
GRADLE_ZIP=$(basename "$GRADLE_URL")
GRADLE_VER=${GRADLE_ZIP%-bin.zip}
GRADLE_VER=${GRADLE_VER#gradle-}
GRADLE_HASH=$("$JAVA_HOME/bin/java" "$PROJECT/tools/WrapperHash.java" "$GRADLE_URL")
GRADLE_DIST="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/${GRADLE_ZIP%.zip}/$GRADLE_HASH"
if [ -f "$GRADLE_DIST/$GRADLE_ZIP.ok" ] || [ -f "$GRADLE_DIST/$GRADLE_ZIP" ]; then
  say "Gradle $GRADLE_VER is already downloaded"
else
  say "Downloading Gradle $GRADLE_VER (about 150 MB, one time)"
  mkdir -p "$GRADLE_DIST"
  GRADLE_SHA=$(curl -fsSL --connect-timeout 20 --max-time 180 --retry 3 --retry-all-errors "$GRADLE_URL.sha256" 2>/dev/null | tr -d '[:space:]' || true)
  if [[ "$GRADLE_SHA" =~ ^[0-9a-f]{64}$ ]]; then
    echo "    official SHA-256: $GRADLE_SHA"
    sources=("$GRADLE_URL"
             "https://mirrors.cloud.tencent.com/gradle/$GRADLE_ZIP"
             "https://mirrors.huaweicloud.com/gradle/$GRADLE_ZIP"
             "https://mirrors.aliyun.com/gradle/distributions/v$GRADLE_VER/$GRADLE_ZIP"
             "$GRADLE_URL")
  else
    warn "Couldn't read Gradle's official checksum, so only gradle.org itself will be used."
    sources=("$GRADLE_URL")
  fi
  got_gradle=""
  for i in "${!sources[@]}"; do
    if [ "$i" -lt $(( ${#sources[@]} - 1 )) ]; then mode=quick; else mode=patient; fi
    fetch_one "$GRADLE_DIST/$GRADLE_ZIP.download" "${sources[$i]}" "$mode" || true
    [ -s "$GRADLE_DIST/$GRADLE_ZIP.download" ] || continue
    if [ -n "$GRADLE_SHA" ] && [ "$(shasum -a 256 "$GRADLE_DIST/$GRADLE_ZIP.download" | awk '{print $1}')" != "$GRADLE_SHA" ]; then
      warn "That copy doesn't match the official checksum; trying the next source."
      rm -f "$GRADLE_DIST/$GRADLE_ZIP.download"
      continue
    fi
    got_gradle=yes
    break
  done
  [ -n "$got_gradle" ] || die "Couldn't download Gradle $GRADLE_VER. Check the internet connection and double-click again."
  mv "$GRADLE_DIST/$GRADLE_ZIP.download" "$GRADLE_DIST/$GRADLE_ZIP"
  echo "    Gradle $GRADLE_VER downloaded and verified"
fi

# 4. Build ----------------------------------------------------------------------------------------
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$PROJECT/local.properties"
chmod +x "$PROJECT/gradlew"
say "Building the app (first v2 build: about 15-30 minutes)"
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
exit 0
}
