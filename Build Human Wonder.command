#!/bin/bash
# Double-click in Finder to build Human Wonder (runs tools/build-on-mac.sh in Terminal).
cd "$(dirname "$0")" || exit 1
exec bash tools/build-on-mac.sh
