#!/usr/bin/env bash

set -euo pipefail

FRONT_URL="${FRONT_URL:-http://127.0.0.1:8080}"
ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:9090}"
BATCH_URL="${BATCH_URL:-http://127.0.0.1:9091}"

check_status() {
  local name="$1"
  local expected="$2"
  local url="$3"
  local actual

  actual="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' "$url")"
  if [[ "$actual" != "$expected" ]]; then
    printf 'FAIL %-28s expected=%s actual=%s url=%s\n' "$name" "$expected" "$actual" "$url" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "$name" "$actual"
}

check_status "front liveness" 200 "${FRONT_URL}/health/live"
check_status "front readiness" 200 "${FRONT_URL}/health/ready"
check_status "admin liveness" 200 "${ADMIN_URL}/health/live"
check_status "admin readiness" 200 "${ADMIN_URL}/health/ready"
check_status "admin login page" 200 "${ADMIN_URL}/admin/login"
check_status "admin page access guard" 302 "${ADMIN_URL}/admin/dashboard"
check_status "admin api access guard" 401 "${ADMIN_URL}/api/admin/dashboard/stats"
check_status "batch liveness" 200 "${BATCH_URL}/health/live"
check_status "batch readiness" 200 "${BATCH_URL}/health/ready"

printf 'Smoke test completed successfully.\n'
