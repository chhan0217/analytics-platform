#!/bin/bash
# PostgreSQL Docker 컨테이너 종료

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/../infra"

echo "=== PostgreSQL 종료 ==="
docker compose -f "$INFRA_DIR/docker-compose.yml" down

echo "완료"
