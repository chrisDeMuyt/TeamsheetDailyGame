#!/usr/bin/env bash
# Teamsheet beta stats from the live Fly database. Read-only (the SQL runs in a READ ONLY transaction).
# Usage (from the repo root, flyctl logged in):  bash deploy/beta-stats.sh
set -euo pipefail

FLY="$(command -v flyctl || command -v fly || echo "$HOME/.fly/bin/flyctl.exe")"
DIR="$(cd "$(dirname "$0")" && pwd)"

# The remote terminal echoes the SQL and psql prompts; keep only psql's results after its banner.
"$FLY" postgres connect -a teamsheet-daily-db -d teamsheet_daily < "$DIR/beta-stats.sql" 2>/dev/null \
  | tr -d '\r' \
  | sed -E 's/\x1b\[[?0-9;]*[a-zA-Z]//g' \
  | awk '/^Type "help" for help\./ { on = 1; next }
         on && !/^teamsheet_daily[=-]/ && !/^(BEGIN|CREATE VIEW|ROLLBACK)$/ { print; fflush() }'
