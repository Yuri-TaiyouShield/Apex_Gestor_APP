#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

mkdir -p "$HOME/.m2/repository" "$HOME/.npm" "$HOME/.gradle"

if command -v sudo >/dev/null 2>&1; then
  sudo chown -R "$(id -u):$(id -g)" "$HOME/.m2" "$HOME/.npm" "$HOME/.gradle" || true
fi

npm config set fund false >/dev/null
npm config set audit false >/dev/null
corepack enable >/dev/null 2>&1 || true

if ! command -v ng >/dev/null 2>&1; then
  npm install --global @angular/cli@19
fi

if ! command -v ionic >/dev/null 2>&1; then
  npm install --global @ionic/cli
fi

echo "Apex CDE base initialized at $ROOT_DIR"
