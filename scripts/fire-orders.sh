#!/usr/bin/env bash
# Fires N takeout orders at the running backend; feeds the KDS latency measurement in docs/perf/kds-latency.md.
# Usage: scripts/fire-orders.sh [N=20]   env: BASE, PIN, SLEEP, MENU_ITEM
set -euo pipefail

N=${1:-20}
BASE=${BASE:-http://localhost:8090}
PIN=${PIN:-9012}
SLEEP=${SLEEP:-3}

# curl wrapper: prints the body, fails loudly (stderr + exit 1) on a non-2xx status
call() {
  local out status
  out=$(curl -sS -w '\n%{http_code}' "$@")
  status=${out##*$'\n'}
  out=${out%$'\n'*}
  [[ $status == 2* ]] || { echo "HTTP $status from ${*: -1}: $out" >&2; exit 1; }
  printf '%s' "$out"
}

TOKEN=$(call -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"pin\":\"$PIN\"}" | jq -er .token)
AUTH="Authorization: Bearer $TOKEN"

MENU_ITEM=${MENU_ITEM:-$(call "$BASE/api/menu/items/active" | jq -er '.[0].id')}

for ((i = 1; i <= N; i++)); do
  order=$(call -X POST "$BASE/api/orders" -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"orderType\":\"TAKEOUT\",\"items\":[{\"menuItemId\":\"$MENU_ITEM\",\"quantity\":1}]}")
  id=$(jq -er .id <<<"$order")
  status=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/orders/$id/fire" -H "$AUTH")
  [[ $status == 2* ]] || { echo "fire $id failed: HTTP $status" >&2; exit 1; }
  echo "$(jq -r .orderNumber <<<"$order") fired (HTTP $status)"
  sleep "$SLEEP"
done
