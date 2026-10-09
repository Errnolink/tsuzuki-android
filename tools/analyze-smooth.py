import json
import pathlib
import re
import subprocess

root = pathlib.Path(__file__).resolve().parents[1] / ".verification/r4-smooth"
rows = []
for path in sorted(root.glob("*-framestats.txt")):
    raw = path.read_text(encoding="utf-8")
    def extract(pattern, convert=int):
        match = re.search(pattern, raw)
        return convert(match.group(1)) if match else None
    rows.append({
        "file": path.name,
        "frames": extract(r"Total frames rendered: (\d+)"),
        "jank_percent": extract(r"Janky frames: \d+ \(([\d.]+)%\)", float),
        "p50_ms": extract(r"50th percentile: (\d+)ms"),
        "p90_ms": extract(r"90th percentile: (\d+)ms"),
        "p99_ms": extract(r"99th percentile: (\d+)ms"),
        "slow_ui": extract(r"Number Slow UI thread: (\d+)"),
    })
root.joinpath("frame-summary.json").write_text(json.dumps(rows, indent=2), encoding="utf-8")
for row in rows:
    print(json.dumps(row), flush=True)

processor = root / "trace_processor_shell.exe"
queries = {
    "main-slices": "SELECT s.name,count(*) AS n,round(sum(s.dur)/1e6,2) AS total_ms,round(max(s.dur)/1e6,2) AS max_ms FROM slice s JOIN thread_track tt ON tt.id=s.track_id JOIN thread t ON t.utid=tt.utid JOIN process p ON p.upid=t.upid WHERE p.name='{pkg}' AND t.is_main_thread=1 AND s.dur>0 GROUP BY s.name ORDER BY total_ms DESC LIMIT 30",
    "effects": "SELECT s.name,t.name AS thread,count(*) AS n,round(sum(s.dur)/1e6,2) AS total_ms,round(max(s.dur)/1e6,2) AS max_ms FROM slice s JOIN thread_track tt ON tt.id=s.track_id JOIN thread t ON t.utid=tt.utid JOIN process p ON p.upid=t.upid WHERE p.name='{pkg}' AND s.dur>0 AND (s.name LIKE 'Haze%' OR s.name LIKE '%decode%' OR s.name LIKE '%SQLite%') GROUP BY s.name,t.name ORDER BY total_ms DESC LIMIT 40",
    "cpu": "SELECT t.name,round(sum(s.dur)/1e6,2) AS running_ms FROM sched s JOIN thread t ON t.utid=s.utid JOIN process p ON p.upid=t.upid WHERE p.name='{pkg}' GROUP BY t.utid ORDER BY running_ms DESC LIMIT 12",
}
if processor.is_file():
    for trace in sorted(root.glob("*.perfetto-trace")):
        pkg = "dev.errnolink.tsuzuki." + ("benchmark" if "benchmark" in trace.name else "dev")
        for name, query in queries.items():
            result = subprocess.run([str(processor), "query", str(trace), query.format(pkg=pkg)], capture_output=True, text=True, check=True)
            root.joinpath(f"{trace.stem}-{name}.csv").write_text(result.stdout, encoding="utf-8")
