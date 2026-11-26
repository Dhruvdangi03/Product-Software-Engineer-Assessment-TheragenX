#!/usr/bin/env bash
# Starts the backend and the review UI from the committed outputs. No API key or network needed
# (except for installing dependencies the first time).
#   Backend: http://localhost:8080/api      UI: http://localhost:5173
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -d ui/node_modules ]]; then
  npm --prefix ui ci
fi
./mvnw -q -DskipTests package

java -jar target/case-intake.jar &
BACKEND_PID=$!
trap 'kill "$BACKEND_PID" 2>/dev/null' EXIT

echo "Waiting for the backend..."
for _ in $(seq 1 60); do
  if curl -sf http://localhost:8080/api/catalog > /dev/null; then
    break
  fi
  sleep 1
done

npm --prefix ui run dev
