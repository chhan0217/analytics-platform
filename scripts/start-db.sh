#!/bin/bash
# PostgreSQL Docker 컨테이너 기동

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/../infra"

echo "=== PostgreSQL 기동 ==="
docker compose -f "$INFRA_DIR/docker-compose.yml" up -d

echo ""
echo "=== 상태 확인 ==="
docker compose -f "$INFRA_DIR/docker-compose.yml" ps

echo ""
echo "PostgreSQL: localhost:5432"
echo "DB: analytics / User: analytics / Password: analytics_local"
