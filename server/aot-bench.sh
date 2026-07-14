#!/usr/bin/env bash
# Measures cold-start of the packaged jar, optionally with a JDK AOT cache.
#
#   ./aot-bench.sh "baseline"
#   ./aot-bench.sh "aot" -XX:AOTCache=target/app.aot
#
# Every run gets a throwaway data dir and an ephemeral port, so the numbers are cold-start numbers
# and not "H2 already had a database here".
set -euo pipefail
cd "$(dirname "$0")"

LABEL="${1:?usage: aot-bench.sh <label> [java opts...]}"
shift || true

JAR=$(ls target/server-*.jar | grep -v plugin-api | head -1)
DATA=$(mktemp -d)
LOG=$(mktemp)
trap 'rm -rf "$DATA" "$LOG"' EXIT

java "$@" \
  -Djava.awt.headless=true \
  -jar "$JAR" \
  --server.port=0 \
  --app.base-path="$DATA" \
  --app.start-browser=false \
  --app.system-tray=false \
  --app.mail.smtp.port=0 \
  --app.mail.imap.port=0 \
  > "$LOG" 2>&1 &
PID=$!

# Wait for the boot line, but never hang forever on a failed start.
for _ in $(seq 1 120); do
  grep -q "Started MatrosSpringbootApplication" "$LOG" && break
  kill -0 "$PID" 2>/dev/null || break
  sleep 0.5
done
kill "$PID" 2>/dev/null || true
wait "$PID" 2>/dev/null || true

STARTED=$(grep -o "Started MatrosSpringbootApplication in [0-9.]* seconds" "$LOG" | head -1 || true)
AOT=$(grep -o "\[AOT\].*\|AOT cache.*" "$LOG" | head -1 || true)
printf "%-24s %s  %s\n" "$LABEL" "${STARTED:-<did not start — see log>}" "$AOT"
[ -n "$STARTED" ] || { echo "--- tail ---"; tail -15 "$LOG"; }
