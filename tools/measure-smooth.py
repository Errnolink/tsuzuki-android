import argparse
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("--build", choices=["debug", "benchmark"], required=True)
parser.add_argument("--phase", choices=["before", "after"], required=True)
parser.add_argument("--journey", choices=["navigation", "reader", "traces"], required=True)
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parents[1] / ".verification/r4-smooth"
root.mkdir(parents=True, exist_ok=True)
pkg = "dev.errnolink.tsuzuki." + ("dev" if args.build == "debug" else "benchmark")
adb = ["C:/Android/Sdk/platform-tools/adb.exe", "-s", "emulator-5554"]
prefix = f"{args.phase}-{args.build}"
started = time.monotonic()
actions = []


def run(*command):
    if time.monotonic() - started > 810:
        raise RuntimeError("Device batch exceeded its bounded scenario window")
    result = subprocess.run(adb + list(command), capture_output=True, timeout=120, check=True)
    return result.stdout.decode("utf-8", errors="replace")


def action(*command, pause=4):
    actions.append({"seconds": round(time.monotonic() - started, 3), "input": command, "pause": pause})
    run("shell", "input", *command)
    time.sleep(pause)


def hierarchy():
    run("shell", "uiautomator", "dump", "/data/local/tmp/r4-window.xml")
    return run("shell", "cat", "/data/local/tmp/r4-window.xml")


def snapshot(name):
    xml = hierarchy()
    root.joinpath(name + ".xml").write_text(xml, encoding="utf-8")
    root.joinpath(name + ".png").write_bytes(subprocess.check_output(adb + ["exec-out", "screencap", "-p"], timeout=60))
    return xml


def click_label(label):
    tree = ET.fromstring(hierarchy())
    node = next((node for node in tree.iter("node") if label in (node.get("text"), node.get("content-desc"))), None)
    if node is None:
        raise RuntimeError(f"Expected UI control absent: {label}")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    action("tap", str((x1 + x2) // 2), str((y1 + y2) // 2), pause=2)


def reset():
    run("shell", "dumpsys", "gfxinfo", pkg, "reset")


def metric(scenario):
    raw = run("shell", "dumpsys", "gfxinfo", pkg, "framestats")
    root.joinpath(f"{prefix}-{scenario}-framestats.txt").write_text(raw, encoding="utf-8")
    fields = [line for line in raw.splitlines()[:40] if any(key in line for key in (
        "Total frames", "Janky frames:", "50th percentile", "90th percentile", "99th percentile", "Number Slow UI",
    ))]
    print(scenario, "; ".join(fields), flush=True)


def mode(label):
    click_label("Reading settings")
    click_label("Reading mode")
    click_label(label)
    click_label("Close Reading settings")


def page_one():
    action("tap", "75", "2120", pause=3)
    if not re.search(r'text="1 of \d+"', hierarchy()):
        raise RuntimeError("Reader fixture did not reset to page one")


def prepare_reader():
    action("tap", "350", "875", pause=8)
    action("tap", "540", "1200", pause=2)
    mode("Paged (left to right)")
    page_one()
    action("keyevent", "4")


if "1080x2400" not in run("shell", "wm", "size"):
    raise RuntimeError("Scenario coordinates require the recorded 1080x2400 emulator")
run("shell", "am", "start", "-n", pkg + "/eu.kanade.tachiyomi.ui.main.MainActivity")
time.sleep(5)
xml = snapshot(prefix + "-" + args.journey + "-start")
if 'text="Library"' not in xml or 'text="Continue reading"' not in xml:
    raise RuntimeError("Start at the populated PTR Library top, with search closed")

try:
    if args.journey == "navigation":
        scenarios = [
            ("a-details", [("tap", "190", "1280"), ("keyevent", "4")], 3),
            ("b-tabs", [("tap", str(x), "2220") for x in (424, 650, 875, 200)], 2),
            ("c-settings", [("tap", "875", "2220"), ("tap", "350", "1980"), ("tap", "350", "557"), ("keyevent", "4"), ("keyevent", "4"), ("tap", "200", "2220")], 2),
        ]
        for name, commands, repeats in scenarios:
            reset()
            for _ in range(repeats):
                for command in commands:
                    action(*command)
            metric(name)
            snapshot(prefix + "-" + name + "-end")
    elif args.journey == "reader":
        prepare_reader()
        reset()
        action("tap", "350", "875", pause=8)
        metric("d-reader-open-warm")
        action("tap", "540", "1200", pause=2)
        page_one()
        snapshot(prefix + "-reader-paged-standard-start")
        action("tap", "540", "1200", pause=2)
        reset()
        for _ in range(10):
            action("tap", "930", "1200", pause=2)
        metric("e-paged-standard")
        action("tap", "540", "1200", pause=2)
        xml = snapshot(prefix + "-reader-paged-standard-end")
        if not re.search(r'text="11 of \d+"', xml):
            raise RuntimeError("Ten page turns did not advance page 1 to 11")
        page_one()
        mode("Long strip")
        action("tap", "540", "1200", pause=2)
        reset()
        for _ in range(5):
            action("swipe", "540", "1850", "540", "650", "800", pause=2)
        metric("e-longstrip-standard")
        action("tap", "540", "1200", pause=2)
        xml = snapshot(prefix + "-reader-longstrip-standard-end")
        match = re.search(r'text="(\d+) of (\d+)"', xml)
        if not match or int(match.group(1)) <= 1:
            raise RuntimeError("Long-strip scroll did not advance the page")
        mode("Paged (left to right)")
        page_one()
        action("keyevent", "4")
    else:
        for name, commands in [
            ("b-tabs", [("tap", str(x), "2220") for x in (424, 650, 875, 200)]),
            ("c-settings", [("tap", "875", "2220"), ("tap", "350", "1980"), ("tap", "350", "557"), ("keyevent", "4"), ("keyevent", "4"), ("tap", "200", "2220")]),
        ]:
            remote = f"/data/misc/perfetto-traces/{prefix}-{name}.perfetto-trace"
            recorder = subprocess.Popen(adb + ["shell", "perfetto", "-o", remote, "-t", "35s", "-b", "32mb", "-a", pkg, "sched", "gfx", "view", "am"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            time.sleep(2)
            for command in commands:
                action(*command)
            output = recorder.communicate(timeout=60)[0]
            root.joinpath(f"{prefix}-{name}-trace.log").write_bytes(output)
            if recorder.returncode:
                raise RuntimeError("Perfetto recording failed")
            run("pull", remote, str(root / f"{prefix}-{name}.perfetto-trace"))
finally:
    root.joinpath(prefix + "-" + args.journey + "-actions.json").write_text(json.dumps(actions, indent=2), encoding="utf-8")
