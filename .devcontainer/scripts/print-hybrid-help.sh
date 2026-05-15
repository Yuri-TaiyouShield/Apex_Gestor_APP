#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${CODESPACE_NAME:-}" ]]; then
  DOMAIN="${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-app.github.dev}"
  echo
  echo "Apex hybrid API URL:"
  echo "  https://${CODESPACE_NAME}-8080.${DOMAIN}"
  echo
  echo "Start the cloud stack:"
  echo "  bash scripts/start-hybrid-env.sh"
  echo
fi
