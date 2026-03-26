#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="Fhir Server/docker-compose.yml"
SERVICE="hapi-fhir-postgres"

usage() {
  cat <<'EOF'
Usage: ./scripts/check_postgres.sh [table|shell]
- table  : list the patient table definition inside the postgres container
- shell  : open an interactive psql shell (requires a tty)
EOF
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

cmd=""
case "$1" in
  table)
    cmd="psql -U admin -d hapi -c '\\dt patient'"
    ;;
  shell)
    cmd="psql -U admin -d hapi"
    ;;
  *)
    usage
    exit 1
    ;;
esac

docker compose -f "$COMPOSE_FILE" exec "$SERVICE" $cmd
