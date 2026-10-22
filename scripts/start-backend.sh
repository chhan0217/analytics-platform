#!/bin/bash
# Ktor Backend 서버 실행

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "=== Backend 서버 시작 ==="
echo "http://localhost:8080"
echo ""

cd "$ROOT_DIR"
./gradlew :backend:run
