#!/bin/bash
# Double-click to download the astronaut photographs Orbit 412 shows in the chat.
#
# What it does
#   Reads tools/frames/manifest.txt and downloads each listed photo from NASA's
#   Gateway to Astronaut Photography of Earth (eol.jsc.nasa.gov; images.nasa.gov as a backup).
#   NASA material is not protected by copyright; the app credits NASA JSC as asked.
#   Big originals go to a temporary folder, are shrunk with macOS "sips", and the temporary
#   folder is removed when the script ends. Only the shrunk copies are kept, in media-src/.
#   Files that are already there are skipped, so it is safe to run again.
#   A log is written to tools/frames/fetch.log for Claude to read.
cd "$(dirname "$0")" || exit 1
ROOT="$PWD"
MANIFEST="$ROOT/tools/frames/manifest.txt"
OUT="$ROOT/media-src"
LOG="$ROOT/tools/frames/fetch.log"
TMP="$(mktemp -d /tmp/orbit-frames.XXXXXX)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT/stills" "$OUT/sheets" "$OUT/loops"
: > "$LOG"

say() { echo "$*" | tee -a "$LOG"; }

# One job per line: kind|dest|size|mission|frame
JOBS="$TMP/jobs.txt"
: > "$JOBS"
while read -r kind a b c d e; do
  case "$kind" in
    still) echo "still|$OUT/stills/$a-E-$b.jpg|large|$a|$b" >> "$JOBS" ;;
    sheet|loop)
      dir="$OUT/${kind}s/$a"; mkdir -p "$dir"
      f=$c
      while [ "$f" -le "$d" ]; do
        echo "$kind|$dir/$b-E-$f.jpg|small|$b|$f" >> "$JOBS"
        f=$((f + e))
      done ;;
  esac
done < <(grep -v '^[[:space:]]*#' "$MANIFEST" | grep -v '^[[:space:]]*$')

fetch() {
  IFS='|' read -r kind dest size mission frame <<< "$1"
  [ -s "$dest" ] && { echo "skip $kind $mission-E-$frame"; return 0; }
  lower=$(echo "$mission" | tr 'A-Z' 'a-z')
  pad=$(printf '%06d' "$frame")
  tmp="$TMP/$(basename "$dest").part"
  for url in \
    "https://eol.jsc.nasa.gov/DatabaseImages/ESC/$size/$mission/$mission-E-$frame.JPG" \
    "https://eol.jsc.nasa.gov/DatabaseImages/ESC/$size/$mission/$mission-E-$frame.jpg" \
    "https://images-assets.nasa.gov/image/${lower}e$pad/${lower}e$pad~large.jpg"; do
    if curl -fsSL --retry 2 --connect-timeout 20 --max-time 300 -o "$tmp" "$url" 2>/dev/null \
       && [ "$(head -c 2 "$tmp" | od -An -tx1 | tr -d ' \n')" = "ffd8" ]; then
      w=$(sips -g pixelWidth "$tmp" 2>/dev/null | awk '/pixelWidth/{print $2}')
      h=$(sips -g pixelHeight "$tmp" 2>/dev/null | awk '/pixelHeight/{print $2}')
      bytes=$(stat -f %z "$tmp")
      if [ "$kind" = "still" ]; then
        sips -Z 2400 -s format jpeg -s formatOptions 92 "$tmp" --out "$dest" >/dev/null 2>&1 || cp "$tmp" "$dest"
      else
        cp "$tmp" "$dest"
      fi
      rm -f "$tmp"
      echo "ok $kind $mission-E-$frame ${w}x${h} $bytes bytes from $url"
      return 0
    fi
  done
  rm -f "$tmp"
  echo "FAILED $kind $mission-E-$frame"
}
export -f fetch
export TMP

total=$(wc -l < "$JOBS" | tr -d ' ')
say "== $(date)  $total files to check (photos and time-lapse frames)"
say "== This can take a few minutes. Keep this window open."
tr '\n' '\0' < "$JOBS" | xargs -0 -P 6 -I{} bash -c 'fetch "$1"' _ {} 2>&1 | tee -a "$LOG" | \
  awk '{n++; if (n % 20 == 0) printf("   %d done\n", n) > "/dev/stderr"}'
ok=$(grep -c '^ok ' "$LOG"); skip=$(grep -c '^skip ' "$LOG"); bad=$(grep -c '^FAILED ' "$LOG")
say "== Finished: $ok downloaded, $skip already here, $bad failed."
say "== Kept in media-src: $(du -sh "$OUT" | cut -f1)"
echo
echo "Done. You can close this window and tell Claude."
