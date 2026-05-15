#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

INCLUDE_WEB=false
BUILD_FLAG="--build"
QUIET=false

for arg in "$@"; do
  case "$arg" in
    --web)
      INCLUDE_WEB=true
      ;;
    --no-build)
      BUILD_FLAG=""
      ;;
    --quiet)
      QUIET=true
      ;;
    -h|--help)
      cat <<'HELP'
Usage: bash scripts/start-hybrid-env.sh [--web] [--no-build] [--quiet]

Starts the heavy Apex Gestor services inside GitHub Codespaces:
  - MySQL
  - RabbitMQ
  - Spring Boot backend
  - optional Angular web container with --web

The local Electron client can then connect to the Codespace API.
HELP
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 2
      ;;
  esac
done

export DB_PORT="${DB_PORT:-2705}"
export API_PORT="${API_PORT:-8080}"
export WEB_PORT="${WEB_PORT:-4200}"
export DB_PASSWORD="${DB_PASSWORD:-apex_dev_2026}"
export DB_NAME="${DB_NAME:-apex_db}"
export RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-apex}"
export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-apex_dev_2026}"
export APEX_RABBIT_ENABLED="${APEX_RABBIT_ENABLED:-true}"
export APEX_EMAIL_ENABLED="${APEX_EMAIL_ENABLED:-false}"

log() {
  if [[ "$QUIET" != "true" ]]; then
    echo "$@"
  fi
}

wait_container_healthy() {
  local container="$1"
  local deadline=$((SECONDS + 240))

  log "Waiting for $container to become healthy..."
  while (( SECONDS < deadline )); do
    local status
    status="$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      log "$container is healthy."
      return 0
    fi
    sleep 3
  done

  echo "$container did not become healthy in time." >&2
  docker logs "$container" --tail 80 >&2 || true
  return 1
}

wait_http_health() {
  local url="$1"
  local deadline=$((SECONDS + 240))

  log "Waiting for API health at $url..."
  while (( SECONDS < deadline )); do
    if curl -fsS "$url" | jq -e '.status == "UP"' >/dev/null 2>&1; then
      log "API is healthy."
      return 0
    fi
    sleep 3
  done

  echo "API did not become healthy at $url in time." >&2
  docker compose logs --tail 120 apex-backend >&2 || true
  return 1
}

codespace_api_url() {
  if [[ -n "${CODESPACE_NAME:-}" ]]; then
    local domain="${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-app.github.dev}"
    echo "https://${CODESPACE_NAME}-${API_PORT}.${domain}"
  else
    echo "http://localhost:${API_PORT}"
  fi
}

log "Starting Apex cloud services..."
docker compose up -d apex-mysql apex-rabbitmq
wait_container_healthy apex-gestor-mysql
wait_container_healthy apex-gestor-rabbitmq

if [[ -n "$BUILD_FLAG" ]]; then
  docker compose up -d "$BUILD_FLAG" apex-backend
else
  docker compose up -d apex-backend
fi

wait_http_health "http://127.0.0.1:${API_PORT}/actuator/health"

if [[ "$INCLUDE_WEB" == "true" ]]; then
  if [[ -n "$BUILD_FLAG" ]]; then
    docker compose up -d "$BUILD_FLAG" apex-web
  else
    docker compose up -d apex-web
  fi
fi

API_URL="$(codespace_api_url)"
mkdir -p .codespaces
cat > .codespaces/hybrid-api.env <<EOF
APEX_API_BASE_URL=${API_URL}
ELECTRON_START_URL=http://localhost:4200
EOF

if [[ -n "${CODESPACE_NAME:-}" ]] && command -v gh >/dev/null 2>&1; then
  gh codespace ports visibility "${API_PORT}:public" -c "$CODESPACE_NAME" >/dev/null 2>&1 || true
fi

cat <<EOF

Apex Gestor hybrid environment is ready.

Cloud API health:
  http://127.0.0.1:${API_PORT}/actuator/health

Codespaces API URL for local Electron:
  ${API_URL}

Create this file on your local checkout:
  Apex_Gestor/.env.local

With this content:
  APEX_API_BASE_URL=${API_URL}
  ELECTRON_START_URL=http://localhost:4200

Then run locally:
  cd Apex_Gestor
  npm run start:electron

For the private tunnel mode, keep port 8080 private and run locally:
  gh codespace ports forward ${API_PORT}:${API_PORT} -c ${CODESPACE_NAME:-<codespace-name>}
  set APEX_API_BASE_URL=http://localhost:${API_PORT}

EOF
