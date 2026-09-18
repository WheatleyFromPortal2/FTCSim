#!/bin/bash
# Development helper: (re)start FTCSim in the background against a repo and wait for the UI port.
# usage: tools/dev-run.sh <repo> <port> [extra args...]
REPO="$1"; PORT="$2"; shift 2
cd "$(dirname "$0")/.."
pkill -f "ftcsim.Main.*--port $PORT( |$)" 2>/dev/null; sleep 1
export JAVA_TOOL_OPTIONS= FTCSIM_NO_BROWSER=1
LOG=build/ftcsim-dev-$PORT.log
mkdir -p build
(timeout 3600 ./gradlew :sim:run -Prepo="$REPO" --args="--no-browser --port $PORT $*" --console=plain > "$LOG" 2>&1 &)
n=0
until [ "$(curl -s -m 2 -o /dev/null -w '%{http_code}' http://localhost:$PORT/api/opmodes)" = "200" ] || [ $n -ge 150 ]; do
  sleep 2; n=$((n+1))
  if grep -q "BUILD FAILED" "$LOG"; then echo "BUILD FAILED"; grep -v "^Picked" "$LOG" | grep -E "error|FAILED|wrong" -A5 | head -60; exit 1; fi
done
echo "FTCSim up on port $PORT after $((n*2))s"
grep -v "^Picked" "$LOG" | grep -E "^[IWE]/" | tail -15
