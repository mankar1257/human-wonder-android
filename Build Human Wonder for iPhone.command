#!/bin/bash
# Double-click in Finder to build Human Wonder for iPhone (runs tools/build-ios-on-mac.sh in Terminal).
cd "$(dirname "$0")" || exit 1
exec bash tools/build-ios-on-mac.sh
