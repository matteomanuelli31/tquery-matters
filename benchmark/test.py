#!/usr/bin/env python3
import requests
import time
import statistics
import sys
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

SERVER_URL = "http://localhost:9000/filterProjects"

FILTERS = [
    {"status": "in_progress", "technology": "Python"},
    {"status": "completed", "technology": "Java"},
    {"status": "testing", "technology": "C++"},
    {"status": "planning", "technology": "Go"},
]

USE_TQUERY = True

def make_request(request_id, filter_data, use_tquery):
    request_data = {**filter_data, "useTQuery": use_tquery}
    start = time.time()
    try:
        response = requests.post(SERVER_URL, json=request_data, timeout=120)
        response.raise_for_status()
        elapsed_ms = int((time.time() - start) * 1000)
        count = response.json().get("count", 0)
        return elapsed_ms, count, None
    except Exception as e:
        elapsed_ms = int((time.time() - start) * 1000)
        return elapsed_ms, 0, str(e)

def run_benchmark(num_clients, requests_per_client, use_tquery):
    total_requests = num_clients * requests_per_client
    timings = []
    errors = 0

    mode = "WITH TQuery" if use_tquery else "WITHOUT TQuery"
    print(f"Starting benchmark ({mode}): {num_clients} parallel clients, {requests_per_client} requests each")
    print(f"Total requests: {total_requests}")

    start_time = time.time()

    with ThreadPoolExecutor(max_workers=num_clients) as executor:
        futures = []
        request_id = 0

        for client in range(num_clients):
            for req in range(requests_per_client):
                filter_data = FILTERS[request_id % len(FILTERS)]
                future = executor.submit(make_request, f"C{client}_R{req}", filter_data, use_tquery)
                futures.append(future)
                request_id += 1

        for future in as_completed(futures):
            elapsed_ms, count, error = future.result()
            timings.append(elapsed_ms)
            if error:
                errors += 1

    total_time_ms = int((time.time() - start_time) * 1000)

    timings.sort()
    min_time = min(timings)
    max_time = max(timings)
    avg_time = int(statistics.mean(timings))
    median_time = int(statistics.median(timings))
    percentile_95 = timings[int(len(timings) * 0.95)]
    percentile_99 = timings[int(len(timings) * 0.99)]
    throughput = total_requests / (total_time_ms / 1000)

    mode = "WITH TQuery" if use_tquery else "WITHOUT TQuery"
    results_text = f"""
=== BENCHMARK RESULTS ({mode}) ===

Configuration:
  - Mode: {mode}
  - Concurrent clients: {num_clients}
  - Requests per client: {requests_per_client}
  - Total requests: {total_requests}
  - Errors: {errors}

Timing Statistics (milliseconds):
  - Min:  {min_time}ms
  - Max:  {max_time}ms
  - Avg:  {avg_time}ms
  - P50:  {median_time}ms
  - P95:  {percentile_95}ms
  - P99:  {percentile_99}ms

Performance:
  - Total time: {total_time_ms}ms
  - Throughput: {throughput:.2f} req/sec
"""

    print(results_text)

    script_dir = os.path.dirname(os.path.abspath(__file__))
    suffix = "_tquery" if use_tquery else "_no_tquery"

    with open(os.path.join(script_dir, f"results{suffix}.txt"), "w") as f:
        f.write(results_text)

    with open(os.path.join(script_dir, f"timings{suffix}.txt"), "w") as f:
        for t in timings:
            f.write(f"{t}\n")

num_clients = int(sys.argv[1]) if len(sys.argv) > 1 else 10
requests_per_client = int(sys.argv[2]) if len(sys.argv) > 2 else 10
use_tquery = sys.argv[3].lower() in ['true', '1', 'yes'] if len(sys.argv) > 3 else True

run_benchmark(num_clients, requests_per_client, use_tquery)
