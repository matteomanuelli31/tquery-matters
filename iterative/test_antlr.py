#!/usr/bin/env python3
import subprocess

def run(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True)

CP = ".:generated:antlr4.jar:json-simple.jar"

# Setup and compile
run("rm -rf generated && mkdir generated && java -jar antlr4.jar -visitor -o generated SelectQuery.g4 && javac -cp antlr4.jar generated/*.java")

# Compile sources and tests
sources = "Value.java ValueVector.java TypeCastingException.java SelectBuilder.java JsonUtils.java"
tests = "SelectSimpleTest SelectArrayTest SelectNestedTest SelectCompaniesTest SelectFieldTest"
run(f"rm -f *.class && javac -cp {CP} {sources} {' '.join([t + '.java' for t in tests.split()])}")

# Run tests
results = [(t, run(f"java -cp {CP} {t}")) for t in tests.split()]

# Report
print(f"\n{'='*60}")
[print(f"{'✓' if r.returncode == 0 else '✗'} {t}") for t, r in results]
print(f"{'='*60}")
print("✓ ALL PASSED" if all(r.returncode == 0 for _, r in results) else "✗ FAILED")

# Cleanup
run("rm -f *.class && rm -rf generated")
exit(0 if all(r.returncode == 0 for _, r in results) else 1)
