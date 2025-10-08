#!/bin/bash

# run from root dir where there is lib/ dir
cd "$(dirname "$0")/.." || exit 1

# kill any existing server on port 9000
echo "Cleaning up any existing server..."
fuser -k 9000/tcp 2>/dev/null || true
sleep 1

echo "Starting server..."
jolie AnyToArr/server_tags.ol > /tmp/server.log 2>&1 &
SERVER_PID=$!
sleep 1

echo "=== Test 1: Undefined ==="
curl -s -X POST http://localhost:9000/normalizeTags \
  -H "Content-Type: application/json" \
  -d '{"name": "User without tags"}'

echo -e "\n\n=== Test 2: Single string ==="
curl -s -X POST http://localhost:9000/normalizeTags \
  -H "Content-Type: application/json" \
  -d '{"name": "User with tag", "tags": "admin"}'

echo -e "\n\n=== Test 3: Array ==="
curl -s -X POST http://localhost:9000/normalizeTags \
  -H "Content-Type: application/json" \
  -d '{"name": "User with tags", "tags": ["user", "admin", "moderator"]}'

echo -e "\n\nStopping server..."
kill $SERVER_PID 2>/dev/null
sleep 1

echo -e "\n=== SERVER LOG ==="
cat /tmp/server.log
