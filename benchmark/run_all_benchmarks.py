#!/usr/bin/env python3
"""TQuery Benchmark Runner - Measures server performance under stress"""
import subprocess, time, sys, statistics, threading, asyncio, aiohttp
from typing import TypedDict


class BenchmarkStats(TypedDict):
    p50: int
    p95: int
    max_heap_mb: float
    ygc: int
    fgc: int


FILTERS = [
    {"status": "in_progress", "technology": "Python"},
    {"status": "completed", "technology": "Java"},
    {"status": "testing", "technology": "C++"},
    {"status": "planning", "technology": "Go"},
]


def run_cmd(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True)


def kill_server():
    run_cmd("fuser -k 9000/tcp 2>/dev/null || true")
    time.sleep(2)


def start_server(cmd, log_file):
    kill_server()
    run_cmd(f"{cmd} > {log_file} 2>&1 &")
    time.sleep(3)
    assert run_cmd("lsof -i :9000").returncode == 0, f"Server failed to start:\n{open(log_file).read()}"


async def make_request(session, filter_data, use_tquery):
    start = time.time()
    async with session.post("http://localhost:9000/filterProjects", json={**filter_data, "useTQuery": use_tquery}, timeout=aiohttp.ClientTimeout(total=60)) as resp:
        resp.raise_for_status()
        await resp.read()
        return int((time.time() - start) * 1000)


def monitor_memory(stop_event, mem_data, pid):
    while not stop_event.wait(timeout=0.05):
        vals = run_cmd(f"jstat -gc {pid} 1 1").stdout.strip().split("\n")[1].split()
        heap_mb = sum(float(vals[i]) for i in [2, 3, 5, 7]) / 1024
        mem_data.append((heap_mb, int(vals[12]), int(vals[14])))


async def run_requests(total_requests, use_tquery):
    async with aiohttp.ClientSession() as session:
        tasks = [make_request(session, FILTERS[i % len(FILTERS)], use_tquery) for i in range(total_requests)]
        return await asyncio.gather(*tasks)

def run_benchmark(name, total_requests, use_tquery, process_name) -> BenchmarkStats:
    print(f"{name}: {total_requests} parallel requests", end=" ... ", flush=True)

    mem_data, stop_event = [], threading.Event()
    pid = run_cmd(f"pgrep -f '{process_name}'").stdout.strip().split()[0]
    monitor_thread = threading.Thread(target=monitor_memory, args=(stop_event, mem_data, pid), daemon=True)
    monitor_thread.start()

    timings = asyncio.run(run_requests(total_requests, use_tquery))

    stop_event.set()
    monitor_thread.join(timeout=2)

    timings.sort()
    p50, p95 = int(statistics.median(timings)), timings[int(len(timings) * 0.95)]
    max_heap_mb = max(m[0] for m in mem_data)
    ygc, fgc = mem_data[-1][1], mem_data[-1][2]

    print("done")
    return {"p50": p50, "p95": p95, "max_heap_mb": max_heap_mb, "ygc": ygc, "fgc": fgc}


def print_summary(stats_no_tq, stats_tq, stats_jsonpath):
    print(f"""
{"=" * 70}
BENCHMARK SUMMARY
{"=" * 70}
{'Metric':<20} {'WITHOUT TQuery':<20} {'WITH TQuery':<20} {'JsonPath':<20}
{"-" * 70}
{'P50 (ms)':<20} {stats_no_tq['p50']:<20} {stats_tq['p50']:<20} {stats_jsonpath['p50']:<20}
{'P95 (ms)':<20} {stats_no_tq['p95']:<20} {stats_tq['p95']:<20} {stats_jsonpath['p95']:<20}
{'Max Heap (MB)':<20} {stats_no_tq['max_heap_mb']:<20.1f} {stats_tq['max_heap_mb']:<20.1f} {stats_jsonpath['max_heap_mb']:<20.1f}
{'Young GC':<20} {stats_no_tq['ygc']:<20} {stats_tq['ygc']:<20} {stats_jsonpath['ygc']:<20}
{'Full GC':<20} {stats_no_tq['fgc']:<20} {stats_tq['fgc']:<20} {stats_jsonpath['fgc']:<20}
{"=" * 70}""")


def main():
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 10

    jolie_cmd = """java -cp /usr/lib/jolie/lib/libjolie.jar:/usr/lib/jolie/lib/automaton.jar:/usr/lib/jolie/lib/commons-text.jar:/usr/lib/jolie/lib/jolie-js.jar:/usr/lib/jolie/lib/json-simple.jar:/usr/lib/jolie/jolie.jar:/usr/lib/jolie/jolie-cli.jar \
        jolie.Jolie -l ./lib/*:/usr/lib/jolie/lib:/usr/lib/jolie/javaServices/*:/usr/lib/jolie/extensions/* \
        -i /usr/lib/jolie/include -p /usr/lib/jolie/packages benchmark/server.ol"""

    # WITHOUT TQuery
    start_server(jolie_cmd, "/tmp/jolie_server.log")
    stats_no_tq = run_benchmark("WITHOUT TQuery", n, False, "benchmark/server.ol")

    # WITH TQuery
    start_server(jolie_cmd, "/tmp/jolie_server.log")
    stats_tq = run_benchmark("WITH TQuery", n, True, "benchmark/server.ol")

    # JsonPath
    start_server("cd benchmark && gradle run -PmainClass=JsonPathServer", "/tmp/jsonpath_server.log")
    stats_jsonpath = run_benchmark("JsonPath", n, False, "JsonPathServer")

    kill_server()
    print_summary(stats_no_tq, stats_tq, stats_jsonpath)


try:
    main()
except KeyboardInterrupt:
    kill_server()
    exit("\nInterrupted")
