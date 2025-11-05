#!/usr/bin/env python3

import subprocess

def run(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True)

# Clean and compile
run("rm -f *.class")
print("Compiling...")
sources = run("ls *.java | grep -E '(Test|Exception|Value|Builder)'").stdout.strip().split('\n')

if run(f"javac {' '.join(sources)}").returncode != 0:
    exit(1)

# Run tests
tests = [f.replace('.java', '') for f in sources if 'Test' in f]
results = [(test, run(f"java {test}")) for test in tests]

# Report
print(f"\n{'='*60}")
[print(f"{'✓' if r.returncode == 0 else '✗'} {test}") for test, r in results]
print(f"{'='*60}")
print("✓ ALL PASSED" if all(r.returncode == 0 for _, r in results) else "✗ FAILED")
exit(0 if all(r.returncode == 0 for _, r in results) else 1)
