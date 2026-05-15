#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "[prebuild] Installing root workspace dependencies..."
npm ci --prefer-offline

echo "[prebuild] Installing Angular/Ionic/Electron dependencies..."
npm --prefix Apex_Gestor ci --legacy-peer-deps --prefer-offline

echo "[prebuild] Warming Maven dependency cache..."
chmod +x Apex-Gestordemo/mvnw
(cd Apex-Gestordemo && ./mvnw -q -DskipTests dependency:go-offline)

echo "[prebuild] Pulling database and messaging images..."
docker compose pull apex-mysql apex-rabbitmq

echo "[prebuild] Building backend image cache..."
docker compose build apex-backend

echo "[prebuild] Apex Gestor prebuild finished."
