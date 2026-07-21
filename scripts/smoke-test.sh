#!/usr/bin/env bash

set -euo pipefail

FRONT_URL="${FRONT_URL:-http://127.0.0.1:8080}"
ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:9090}"
BATCH_URL="${BATCH_URL:-http://127.0.0.1:9091}"
FRONT_DETAIL_PRODUCT_ID="${FRONT_DETAIL_PRODUCT_ID:-12}"

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

check_body_contains() {
  local name="$1"
  local expected_status="$2"
  local expected_body="$3"
  local url="$4"
  local response
  local status
  local body

  response="$(curl --silent --show-error --write-out $'\n%{http_code}' "$url")"
  status="${response##*$'\n'}"
  body="${response%$'\n'*}"
  if [[ "$status" != "$expected_status" || "$body" != *"$expected_body"* ]]; then
    printf 'FAIL %-28s expected_status=%s actual_status=%s body_token=%s url=%s\n' \
      "$name" "$expected_status" "$status" "$expected_body" "$url" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s body_token=%s\n' "$name" "$status" "$expected_body"
}

check_header_contains() {
  local name="$1"
  local header_name="$2"
  local expected_value="$3"
  local url="$4"
  local headers

  headers="$(curl --silent --show-error --dump-header - --output /dev/null "$url" | tr -d '\r')"
  if ! printf '%s\n' "$headers" | grep --ignore-case --fixed-strings --quiet "${header_name}: ${expected_value}"; then
    printf 'FAIL %-28s missing_header=%s expected_value=%s url=%s\n' \
      "$name" "$header_name" "$expected_value" "$url" >&2
    return 1
  fi
  printf 'PASS %-28s header=%s\n' "$name" "$header_name"
}

check_header_present() {
  local name="$1"
  local header_name="$2"
  local url="$3"
  local headers

  headers="$(curl --silent --show-error --dump-header - --output /dev/null "$url" | tr -d '\r')"
  if ! printf '%s\n' "$headers" | grep --ignore-case --extended-regexp --quiet "^${header_name}: .+"; then
    printf 'FAIL %-28s missing_header=%s url=%s\n' "$name" "$header_name" "$url" >&2
    return 1
  fi
  printf 'PASS %-28s header=%s\n' "$name" "$header_name"
}

check_status "front liveness" 200 "${FRONT_URL}/health/live"
check_status "front readiness" 200 "${FRONT_URL}/health/ready"
check_status "front storefront" 200 "${FRONT_URL}/"
check_status "front catalog api" 200 "${FRONT_URL}/api/front/catalog/bootstrap"
check_status "front detail page" 200 "${FRONT_URL}/front/products/${FRONT_DETAIL_PRODUCT_ID}"
check_status "front detail api" 200 "${FRONT_URL}/api/front/products/${FRONT_DETAIL_PRODUCT_ID}"
check_body_contains "front missing product" 404 '"code":"F002"' \
  "${FRONT_URL}/api/front/products/9223372036854775807"
check_header_present "front request tracing" "X-Request-Id" "${FRONT_URL}/"
check_header_contains "front dynamic no-store" "Cache-Control" "no-store, max-age=0" "${FRONT_URL}/"
check_header_contains "front context isolation" "Cross-Origin-Opener-Policy" "same-origin" "${FRONT_URL}/"
check_status "admin liveness" 200 "${ADMIN_URL}/health/live"
check_status "admin readiness" 200 "${ADMIN_URL}/health/ready"
check_status "admin login page" 200 "${ADMIN_URL}/admin/login"
check_status "admin page access guard" 302 "${ADMIN_URL}/admin/dashboard"
check_status "admin api access guard" 401 "${ADMIN_URL}/api/admin/dashboard/stats"
check_header_present "admin request tracing" "X-Request-Id" "${ADMIN_URL}/admin/login"
check_header_contains "admin dynamic no-store" "Cache-Control" "no-store, max-age=0" "${ADMIN_URL}/admin/login"
check_header_contains "admin context isolation" "Cross-Origin-Opener-Policy" "same-origin" "${ADMIN_URL}/admin/login"
check_status "batch liveness" 200 "${BATCH_URL}/health/live"
check_status "batch readiness" 200 "${BATCH_URL}/health/ready"
check_header_present "batch request tracing" "X-Request-Id" "${BATCH_URL}/health/live"
check_header_contains "batch response no-store" "Cache-Control" "no-store, max-age=0" "${BATCH_URL}/health/live"
check_header_contains "batch context isolation" "Cross-Origin-Resource-Policy" "same-origin" "${BATCH_URL}/health/live"

printf 'Smoke test completed successfully.\n'
