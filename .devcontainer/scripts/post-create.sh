#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "Apex Gestor CDE ready."
echo
echo "Toolchain:"
java -version 2>&1 | head -n 1 || true
node --version || true
npm --version || true
docker --version || true
docker compose version || true
gh --version | head -n 1 || true
echo
echo "Start cloud backend with:"
echo "  bash scripts/start-hybrid-env.sh"
