#!/usr/bin/env bash
# Logical PostgreSQL backups for Smart Dine production stacks (docker compose).
# See BACKUPS.md for cron, paths, and restore.

set -euo pipefail

SMART_DINE_ROOT="${SMART_DINE_ROOT:-$(cd "$(dirname "$0")/../.." && pwd)}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/smart-dine}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
SKIP_KEYCLOAK="${SKIP_KEYCLOAK:-0}"

COMPOSE_PROD="${COMPOSE_PROD:-$SMART_DINE_ROOT/docker-compose.prod.yml}"
COMPOSE_KEYCLOAK="${COMPOSE_KEYCLOAK:-$SMART_DINE_ROOT/docker-compose.keycloak.yml}"

log() {
  echo "[$(date '+%Y-%m-%dT%H:%M:%S')] $*" >&2
}

require_file() {
  if [[ ! -f "$1" ]]; then
    log "ERROR: missing file: $1"
    exit 1
  fi
}

rotate() {
  local pattern=$1
  find "$BACKUP_DIR" -maxdepth 1 -type f -name "$pattern" -mtime "+${RETENTION_DAYS}" -delete 2>/dev/null || true
}

mkdir -p "$BACKUP_DIR"

ts="$(date +%Y%m%d_%H%M%S)"

require_file "$COMPOSE_PROD"
log "Dumping restaurant_db via $(basename "$COMPOSE_PROD") (service: postgres)..."
restaurant_dump="$BACKUP_DIR/restaurant_db_${ts}.dump"
if ! docker compose -f "$COMPOSE_PROD" --project-directory "$SMART_DINE_ROOT" exec -T postgres \
  pg_dump -U postgres -d restaurant_db -Fc >"$restaurant_dump"; then
  log "ERROR: pg_dump failed for restaurant_db"
  rm -f "$restaurant_dump"
  exit 1
fi
if [[ ! -s "$restaurant_dump" ]]; then
  log "ERROR: empty dump: $restaurant_dump"
  rm -f "$restaurant_dump"
  exit 1
fi
log "OK: $restaurant_dump ($(wc -c <"$restaurant_dump" | tr -d ' ') bytes)"

if [[ "$SKIP_KEYCLOAK" != "1" ]]; then
  if [[ ! -f "$COMPOSE_KEYCLOAK" ]]; then
    log "ERROR: Keycloak compose not found: $COMPOSE_KEYCLOAK (set SKIP_KEYCLOAK=1 if unused)"
    exit 1
  fi
  log "Dumping keycloak via $(basename "$COMPOSE_KEYCLOAK") (service: keycloak-db)..."
  keycloak_dump="$BACKUP_DIR/keycloak_${ts}.dump"
  if ! docker compose -f "$COMPOSE_KEYCLOAK" --project-directory "$SMART_DINE_ROOT" exec -T keycloak-db \
    pg_dump -U keycloak -d keycloak -Fc >"$keycloak_dump"; then
    log "ERROR: pg_dump failed for keycloak"
    rm -f "$keycloak_dump"
    exit 1
  fi
  if [[ ! -s "$keycloak_dump" ]]; then
    log "ERROR: empty dump: $keycloak_dump"
    rm -f "$keycloak_dump"
    exit 1
  fi
  log "OK: $keycloak_dump ($(wc -c <"$keycloak_dump" | tr -d ' ') bytes)"
else
  log "Skipping Keycloak (SKIP_KEYCLOAK=1)"
fi

rotate 'restaurant_db_*.dump'
rotate 'keycloak_*.dump'
log "Rotation: removed dumps older than ${RETENTION_DAYS} days in $BACKUP_DIR"

log "All backups finished successfully."
exit 0
