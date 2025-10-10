#!/usr/bin/env python3
import subprocess
import time
import sys
import os
import signal

def kill_port_9000():
    """Kill any process listening on port 9000"""
    try:
        subprocess.run(['fuser', '-k', '9000/tcp'], stderr=subprocess.DEVNULL)
        time.sleep(2)
    except:
        pass

def start_server(heap_size):
    """Start Jolie server with specified heap size"""
    print(f"Starting server with heap size: {heap_size}")

    cmd = [
        'java',
        f'-Xmx{heap_size}', f'-Xms{heap_size}',
        '-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags',
        '-ea:jolie...', '-ea:joliex...',
        '-Djava.rmi.server.codebase=file://usr/lib/jolie/extensions/rmi.jar',
        '-cp', '/usr/lib/jolie/lib/libjolie.jar:/usr/lib/jolie/lib/automaton.jar:/usr/lib/jolie/lib/commons-text.jar:/usr/lib/jolie/lib/jolie-js.jar:/usr/lib/jolie/lib/json-simple.jar:/usr/lib/jolie/jolie.jar:/usr/lib/jolie/jolie-cli.jar',
        'jolie.Jolie',
        '-l', './lib/*:/usr/lib/jolie/lib:/usr/lib/jolie/javaServices/*:/usr/lib/jolie/extensions/*',
        '-i', '/usr/lib/jolie/include',
        '-p', '/usr/lib/jolie/packages',
        'benchmark/server.ol'
    ]

    with open('/tmp/benchmark_server.log', 'w') as log:
        subprocess.Popen(cmd, stdout=log, stderr=log)

    time.sleep(3)

    # Check if server started
    result = subprocess.run(['lsof', '-i', ':9000'], capture_output=True)
    if result.returncode == 0:
        print("Server started successfully")
        return True
    else:
        print("Error: Server failed to start")
        with open('/tmp/benchmark_server.log', 'r') as f:
            print(f.read())
        return False

def run_benchmark(num_clients, requests_per_client, use_tquery):
    """Run a single benchmark"""
    mode = "WITH TQuery" if use_tquery else "WITHOUT TQuery"
    print(f"\n--- Running {mode} ---")

    try:
        subprocess.run([
            'python3', 'benchmark/test.py',
            str(num_clients), str(requests_per_client),
            'true' if use_tquery else 'false'
        ], timeout=180, check=True)
        return True
    except subprocess.TimeoutExpired:
        print(f"WARNING: {mode} benchmark timed out (likely due to memory constraints)")
        return False
    except subprocess.CalledProcessError as e:
        print(f"WARNING: {mode} benchmark failed with error: {e}")
        return False

def show_gc_stats():
    """Show GC statistics from log"""
    if os.path.exists('/tmp/gc.log'):
        print("\n--- GC Statistics ---")
        try:
            result = subprocess.run(
                ['grep', '-E', 'GC\\([0-9]+\\)', '/tmp/gc.log'],
                capture_output=True, text=True
            )
            if result.stdout:
                lines = result.stdout.strip().split('\n')
                for line in lines[-5:]:
                    print(line)
            else:
                print("No GC events recorded")
        except:
            print("Could not read GC log")
    else:
        print("\n--- No GC log found ---")

def run_benchmarks_with_heap(num_clients, requests_per_client, heap_size):
    """Run benchmarks with specific heap size"""
    print("\n" + "="*50)
    print(f"Benchmarking with heap size: {heap_size}")
    print(f"Clients: {num_clients}, Requests per client: {requests_per_client}")
    print("="*50)

    # Clear GC log
    if os.path.exists('/tmp/gc.log'):
        os.remove('/tmp/gc.log')

    # Kill existing server and start new one
    kill_port_9000()
    if not start_server(heap_size):
        return

    # Run WITHOUT TQuery
    run_benchmark(num_clients, requests_per_client, False)

    time.sleep(2)

    # Run WITH TQuery
    run_benchmark(num_clients, requests_per_client, True)

    # Show GC statistics
    show_gc_stats()

def main():
    # Change to project root
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    os.chdir(project_root)

    print("="*50)
    print("Running benchmarks...")
    print("="*50)

    # Generate test data if not present
    if not os.path.exists('benchmark/large_data.json'):
        print("Generating test data...")
        subprocess.run(['python3', 'benchmark/generate_data.py'])

    # Run benchmarks with 500MB heap
    run_benchmarks_with_heap(2, 5, '500m')

    print("\n" + "="*50)
    print("All benchmarks completed!")
    print("Results saved in benchmark/results_*.txt")
    print("="*50)

    # Clean up
    kill_port_9000()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\nBenchmark interrupted by user")
        kill_port_9000()
        sys.exit(1)
