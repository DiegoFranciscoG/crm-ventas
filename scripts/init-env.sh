#!/usr/bin/env bash
# Genera .env a partir de .env.example con secretos aleatorios (openssl). No sobrescribe un .env existente.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -f .env ]]; then
  echo ".env ya existe; no se modifica." >&2
  exit 0
fi
command -v openssl >/dev/null || { echo "Se necesita openssl." >&2; exit 1; }

random() { openssl rand -base64 "$1" | tr -d '\n'; }

sed \
  -e "s|^DB_PASSWORD=.*|DB_PASSWORD=$(random 24 | tr -d '/+=')|" \
  -e "s|^JWT_SECRET=.*|JWT_SECRET=$(random 48)|" \
  -e "s|^DATA_ENCRYPTION_KEY=.*|DATA_ENCRYPTION_KEY=$(random 32)|" \
  -e "s|^CRON_TOKEN=.*|CRON_TOKEN=$(random 48 | tr -d '/+=')|" \
  .env.example > .env

echo ".env creado con secretos aleatorios. Ejecute: docker compose up --build"
