#!/bin/bash
# Web Dashboard 개발 서버 실행

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DASHBOARD_DIR="$SCRIPT_DIR/../web-dashboard"

echo "=== Web Dashboard 시작 ==="
echo "http://localhost:5173"
echo ""

cd "$DASHBOARD_DIR"
npm run dev
