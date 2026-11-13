"""Print the UDID of an available iPhone simulator.

    python3 ios/tools/pick_sim.py [large|small]

large: the newest "Pro" iPhone (else any recent iPhone); small: an iPhone SE or mini (else the
smallest available). Picks the newest iOS runtime.
"""
import json
import re
import subprocess
import sys

want = sys.argv[1] if len(sys.argv) > 1 else "large"
data = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "-j"]))


def runtime_version(key):
    m = re.search(r"iOS-(\d+)-(\d+)", key)
    return (int(m.group(1)), int(m.group(2))) if m else (0, 0)


candidates = []
for runtime, devices in data["devices"].items():
    if "iOS" not in runtime:
        continue
    for d in devices:
        if d["name"].startswith("iPhone"):
            candidates.append((runtime_version(runtime), d["name"], d["udid"]))

if not candidates:
    sys.exit("no iPhone simulators")

newest = max(c[0] for c in candidates)
pool = [c for c in candidates if c[0] == newest]


def model_number(name):
    m = re.search(r"iPhone (\d+)", name)
    return int(m.group(1)) if m else 0


if want == "small":
    small = [c for c in pool if "SE" in c[1] or "mini" in c[1]]
    pick = sorted(small or pool, key=lambda c: (("SE" not in c[1]), -model_number(c[1])))[0]
else:
    pro = [c for c in pool if c[1].endswith("Pro")]
    pick = sorted(pro or pool, key=lambda c: -model_number(c[1]))[0]

print(pick[2])
print(pick[1], file=sys.stderr)
