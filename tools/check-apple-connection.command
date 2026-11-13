#!/bin/bash
# Double-click: checks whether this Mac can reach Apple's sign-in server (what iloader and
# SideStore use to sign apps with your Apple ID). Writes tools/apple-check.log; nothing is changed.
cd "$(dirname "$0")" || exit 1
LOG="apple-check.log"
{
  echo "== $(date)  macOS $(sw_vers -productVersion)"
  echo "== DNS for gsa.apple.com"
  dscacheutil -q host -a name gsa.apple.com 2>&1 | head -n 12
  echo "== Proxy settings"
  scutil --proxy 2>&1 | grep -E "Enable|Proxy|Exceptions" | head -n 12
  echo "== VPN / network services"
  scutil --nc list 2>&1 | head -n 10
  echo "== Apple sign-in server (gsa.apple.com)"
  curl -sS -o /dev/null --max-time 30 -w "http %{http_code}, connect %{time_connect}s, tls %{time_appconnect}s, total %{time_total}s, ip %{remote_ip}\n" \
    https://gsa.apple.com/grandslam/GsService2/lookup 2>&1
  echo "== Same over IPv4 only"
  curl -4 -sS -o /dev/null --max-time 30 -w "http %{http_code}, total %{time_total}s, ip %{remote_ip}\n" \
    https://gsa.apple.com/grandslam/GsService2/lookup 2>&1
  echo "== For comparison: apple.com, github.com"
  curl -sS -o /dev/null --max-time 20 -w "apple.com http %{http_code} %{time_total}s\n" https://www.apple.com 2>&1
  curl -sS -o /dev/null --max-time 20 -w "github.com http %{http_code} %{time_total}s\n" https://github.com 2>&1
  echo "== Done"
} 2>&1 | tee "$LOG"
echo
echo "Saved to tools/$LOG. You can close this window."
