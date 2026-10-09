import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app" / "src" / "main" / "java"
SCAN_DIRS = [SRC / "eu" / "kanade", SRC / "exh"]
ROUTES_KT = SRC / "dev" / "errnolink" / "tsuzuki" / "ui" / "Routes.kt"
MARKER = "dev.errnolink.tsuzuki.ui"

DECL_RE = re.compile(
    r"\b(?:object|class)\s+([A-Z]\w*)\s*(\((?:[^()]|\([^()]*\))*\))?\s*:\s*([^\{=]+)",
    re.S,
)
PACKAGE_RE = re.compile(r"^package\s+([\w.]+)", re.M)


def classes_in(path):
    text = path.read_text(encoding="utf-8")
    package_match = PACKAGE_RE.search(text)
    package = package_match.group(1) if package_match else ""
    results = []
    for match in DECL_RE.finditer(text):
        name = match.group(1)
        if name in ("Screen", "Tab"):
            continue
        supertypes = {tok.split(".")[-1] for tok in re.findall(r"[\w.]+", match.group(3))}
        if supertypes & {"Screen", "Tab"}:
            results.append((f"{package}.{name}", name, text))
    return results


def main():
    scanned = {}
    for scan_dir in SCAN_DIRS:
        for path in sorted(scan_dir.rglob("*.kt")):
            for fqn, name, text in classes_in(path):
                scanned[fqn] = (name, path, text)

    routed = set()
    for fqn, (name, path, text) in scanned.items():
        if MARKER in text:
            routed.add(fqn)

    caller_files = []
    for scan_dir in SCAN_DIRS:
        for path in sorted(scan_dir.rglob("*.kt")):
            text = path.read_text(encoding="utf-8")
            if MARKER in text:
                caller_files.append(text)
    for fqn, (name, path, text) in scanned.items():
        if fqn in routed:
            continue
        for caller in caller_files:
            if re.search(rf"\b{re.escape(name)}\b", caller):
                routed.add(fqn)
                break

    if ROUTES_KT.exists():
        routes_text = ROUTES_KT.read_text(encoding="utf-8")
        listed = set(re.findall(r'upstream\s*=\s*"([\w.]+)"', routes_text))
        routed |= {fqn for fqn in scanned if fqn in listed}

    unrouted = sorted(set(scanned) - routed)
    for fqn in unrouted:
        name, path, _ = scanned[fqn]
        rel = path.relative_to(ROOT).as_posix()
        print(f"{fqn} ({rel}) — not yet redesigned")
    print(f"\n{len(scanned)} Voyager screens/tabs scanned, {len(scanned) - len(unrouted)} routed, {len(unrouted)} not yet redesigned")
    return 0


if __name__ == "__main__":
    sys.exit(main())
