#!/usr/bin/env bash

set -euo pipefail

FRONT_URL="${FRONT_URL:-http://127.0.0.1:8080}"
ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:9090}"
BATCH_URL="${BATCH_URL:-http://127.0.0.1:9091}"
FRONT_DETAIL_PRODUCT_ID="${FRONT_DETAIL_PRODUCT_ID:-12}"
FRONT_CONTENT_ID="${FRONT_CONTENT_ID:-1}"
ADMIN_SMOKE_LOGIN_ID="${ADMIN_SMOKE_LOGIN_ID:-}"
ADMIN_SMOKE_PASSWORD="${ADMIN_SMOKE_PASSWORD:-}"
ADMIN_COOKIE_JAR=""

cleanup() {
  if [[ -n "$ADMIN_COOKIE_JAR" ]]; then
    rm -f "$ADMIN_COOKIE_JAR"
  fi
}

trap cleanup EXIT

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

check_admin_authenticated_flow() {
  local login_status
  local dashboard_status
  local content_stats_status
  local content_view_export_response
  local content_view_export_status
  local content_view_export_headers
  local content_view_quality_response
  local content_view_quality_status
  local content_view_quality_body
  local content_reaction_response
  local content_reaction_status
  local content_reaction_body
  local content_reaction_export_response
  local content_reaction_export_status
  local content_reaction_export_headers
  local logout_response
  local logout_status
  local logout_headers

  if [[ -z "$ADMIN_SMOKE_LOGIN_ID" || -z "$ADMIN_SMOKE_PASSWORD" ]]; then
    printf 'SKIP %-28s reason=credentials-not-provided\n' "admin authenticated flow"
    return
  fi

  ADMIN_COOKIE_JAR="$(mktemp)"
  login_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --cookie-jar "$ADMIN_COOKIE_JAR" \
    --header "Origin: ${ADMIN_URL}" \
    --data-urlencode "loginId=${ADMIN_SMOKE_LOGIN_ID}" \
    --data-urlencode "password=${ADMIN_SMOKE_PASSWORD}" \
    "${ADMIN_URL}/admin/login")"
  if [[ "$login_status" != "302" ]]; then
    printf 'FAIL %-28s expected=302 actual=%s\n' "admin authenticated login" "$login_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin authenticated login" "$login_status"

  dashboard_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --cookie "$ADMIN_COOKIE_JAR" "${ADMIN_URL}/admin/dashboard")"
  if [[ "$dashboard_status" != "200" ]]; then
    printf 'FAIL %-28s expected=200 actual=%s\n' "admin authenticated page" "$dashboard_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin authenticated page" "$dashboard_status"

  content_stats_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --cookie "$ADMIN_COOKIE_JAR" "${ADMIN_URL}/api/admin/content/stats/daily")"
  if [[ "$content_stats_status" != "200" ]]; then
    printf 'FAIL %-28s expected=200 actual=%s\n' "admin content daily stats" "$content_stats_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin content daily stats" "$content_stats_status"

  content_view_analytics_response="$(curl --silent --show-error --write-out $'\n%{http_code}' \
    --cookie "$ADMIN_COOKIE_JAR" \
    "${ADMIN_URL}/api/admin/content/stats/views?boardType=NOTICE&days=7")"
  content_view_analytics_status="${content_view_analytics_response##*$'\n'}"
  content_view_analytics_body="${content_view_analytics_response%$'\n'*}"
  if [[ "$content_view_analytics_status" != "200" ]] \
    || ! printf '%s\n' "$content_view_analytics_body" | grep --fixed-strings --quiet '"rangeDays":7' \
    || ! printf '%s\n' "$content_view_analytics_body" | grep --fixed-strings --quiet '"summary":'; then
    printf 'FAIL %-28s expected=200 rangeDays=7 summary=required actual=%s\n' \
      "admin content view stats" "$content_view_analytics_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin content view stats" "$content_view_analytics_status"

  content_view_quality_response="$(curl --silent --show-error --write-out $'\n%{http_code}' \
    --cookie "$ADMIN_COOKIE_JAR" \
    "${ADMIN_URL}/api/admin/content/stats/views/quality")"
  content_view_quality_status="${content_view_quality_response##*$'\n'}"
  content_view_quality_body="${content_view_quality_response%$'\n'*}"
  if [[ "$content_view_quality_status" != "200" ]] \
    || ! printf '%s\n' "$content_view_quality_body" | grep --fixed-strings --quiet '"validEventCount":' \
    || ! printf '%s\n' "$content_view_quality_body" | grep --fixed-strings --quiet '"orphanEventCount":' \
    || ! printf '%s\n' "$content_view_quality_body" | grep --fixed-strings --quiet '"status":'; then
    printf 'FAIL %-28s expected=200 quality_fields=required actual=%s\n' \
      "admin content view quality" "$content_view_quality_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin content view quality" "$content_view_quality_status"

  content_view_export_response="$(curl --silent --show-error --dump-header - --output /dev/null \
    --write-out $'\n%{http_code}' --cookie "$ADMIN_COOKIE_JAR" \
    "${ADMIN_URL}/api/admin/content/stats/views/export?boardType=NOTICE&days=7" | tr -d '\r')"
  content_view_export_status="${content_view_export_response##*$'\n'}"
  content_view_export_headers="${content_view_export_response%$'\n'*}"
  if [[ "$content_view_export_status" != "200" ]] \
    || ! printf '%s\n' "$content_view_export_headers" | grep --ignore-case --fixed-strings --quiet 'Content-Type: text/csv' \
    || ! printf '%s\n' "$content_view_export_headers" | grep --ignore-case --fixed-strings --quiet 'Content-Disposition: attachment; filename="content-view-analytics-'; then
    printf 'FAIL %-28s expected=200 csv_headers=required actual=%s\n' \
      "admin content view export" "$content_view_export_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin content view export" "$content_view_export_status"

  content_reaction_response="$(curl --silent --show-error --write-out $'\n%{http_code}' \
    --cookie "$ADMIN_COOKIE_JAR" \
    "${ADMIN_URL}/api/admin/content/stats/reactions?boardType=NOTICE&days=7")"
  content_reaction_status="${content_reaction_response##*$'\n'}"
  content_reaction_body="${content_reaction_response%$'\n'*}"
  if [[ "$content_reaction_status" != "200" ]] \
    || ! printf '%s\n' "$content_reaction_body" | grep --fixed-strings --quiet '"metricBasis":' \
    || ! printf '%s\n' "$content_reaction_body" | grep --fixed-strings --quiet '"helpfulRate":' \
    || ! printf '%s\n' "$content_reaction_body" | grep --fixed-strings --quiet '"improvementContents":'; then
    printf 'FAIL %-28s expected=200 reaction_fields=required actual=%s\n' \
      "admin content reactions" "$content_reaction_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin content reactions" "$content_reaction_status"

  content_reaction_export_response="$(curl --silent --show-error --dump-header - --output /dev/null \
    --write-out $'\n%{http_code}' --cookie "$ADMIN_COOKIE_JAR" \
    "${ADMIN_URL}/api/admin/content/stats/reactions/export?boardType=NOTICE&days=7" | tr -d '\r')"
  content_reaction_export_status="${content_reaction_export_response##*$'\n'}"
  content_reaction_export_headers="${content_reaction_export_response%$'\n'*}"
  if [[ "$content_reaction_export_status" != "200" ]] \
    || ! printf '%s\n' "$content_reaction_export_headers" | grep --ignore-case --fixed-strings --quiet 'Content-Type: text/csv' \
    || ! printf '%s\n' "$content_reaction_export_headers" | grep --ignore-case --fixed-strings --quiet 'Content-Disposition: attachment; filename="content-reaction-analytics-'; then
    printf 'FAIL %-28s expected=200 csv_headers=required actual=%s\n' \
      "admin reaction export" "$content_reaction_export_status" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s\n' "admin reaction export" "$content_reaction_export_status"

  logout_response="$(curl --silent --show-error --dump-header - --output /dev/null --write-out $'\n%{http_code}' \
    --cookie "$ADMIN_COOKIE_JAR" --request POST --header "Origin: ${ADMIN_URL}" \
    "${ADMIN_URL}/admin/logout" | tr -d '\r')"
  logout_status="${logout_response##*$'\n'}"
  logout_headers="${logout_response%$'\n'*}"
  if [[ "$logout_status" != "302" ]] \
    || ! printf '%s\n' "$logout_headers" | grep --ignore-case --fixed-strings --quiet 'Clear-Site-Data: "cache", "cookies", "storage"'; then
    printf 'FAIL %-28s expected_status=302 clear_site_data=required\n' "admin authenticated logout" >&2
    return 1
  fi
  printf 'PASS %-28s status=%s header=Clear-Site-Data\n' "admin authenticated logout" "$logout_status"
}

check_status "front liveness" 200 "${FRONT_URL}/health/live"
check_status "front readiness" 200 "${FRONT_URL}/health/ready"
check_status "front storefront" 200 "${FRONT_URL}/"
check_status "front catalog api" 200 "${FRONT_URL}/api/front/catalog/bootstrap"
check_body_contains "front content highlights" 200 '"popular":' \
  "${FRONT_URL}/api/front/content/highlights?limit=4"
check_body_contains "front content popular sort" 200 '"sort":"POPULAR"' \
  "${FRONT_URL}/api/front/content?sort=POPULAR&size=4"
check_status "front detail page" 200 "${FRONT_URL}/front/products/${FRONT_DETAIL_PRODUCT_ID}"
check_status "front detail api" 200 "${FRONT_URL}/api/front/products/${FRONT_DETAIL_PRODUCT_ID}"
check_body_contains "front content reading data" 200 '"estimatedReadMinutes":' \
  "${FRONT_URL}/api/front/content/${FRONT_CONTENT_ID}"
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
check_admin_authenticated_flow
check_status "batch liveness" 200 "${BATCH_URL}/health/live"
check_status "batch readiness" 200 "${BATCH_URL}/health/ready"
check_header_present "batch request tracing" "X-Request-Id" "${BATCH_URL}/health/live"
check_header_contains "batch response no-store" "Cache-Control" "no-store, max-age=0" "${BATCH_URL}/health/live"
check_header_contains "batch context isolation" "Cross-Origin-Resource-Policy" "same-origin" "${BATCH_URL}/health/live"

printf 'Smoke test completed successfully.\n'
