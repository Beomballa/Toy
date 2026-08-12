#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-new_toy}"
DB_USERNAME="${DB_USERNAME:-root}"

if [[ ! "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "DB_NAME에는 영문, 숫자와 밑줄만 사용할 수 있습니다." >&2
  exit 1
fi

MYSQL=(mysql --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USERNAME}" --database="${DB_NAME}" --batch --skip-column-names)
export MYSQL_PWD="${DB_PASSWORD:-}"

checksum() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

query() {
  "${MYSQL[@]}" --execute="$1"
}

apply_file() {
  "${MYSQL[@]}" < "$1"
}

BASELINE_TABLES=(
  admin_activity_log
  admin_operation_notice
  admin_operation_task
  admin_operation_task_comment
  admin_system_setting
  admin_system_setting_history
  admin_user
  brand
  category
  ct_document
  display_banner
  document_daily_stats
  front_cart
  front_cart_item
  front_content_reaction
  front_content_view_event
  front_product_display
  order_delivery
  order_item
  order_status_history
  orders
  product
  product_change_history
  product_option
  sy_account
  sy_approval_document
)

validate_tables() {
  for required_table in "$@"; do
    table_exists="$(query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND table_name='${required_table}';")"
    if [[ "${table_exists}" != "1" ]]; then
      echo "Required table is missing after migration: ${required_table}" >&2
      return 1
    fi
  done
}

query "
CREATE TABLE IF NOT EXISTS schema_migration (
    version_no VARCHAR(40) NOT NULL,
    description VARCHAR(200) NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    applied_dtm DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
"

BASELINE_FILE="${ROOT_DIR}/db/baseline_schema.sql"
BASELINE_VERSION="2026072800"
BASELINE_CHECKSUM="$(checksum "${BASELINE_FILE}")"
CORE_TABLE_EXISTS="$(query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND table_name='product';")"

if [[ "${CORE_TABLE_EXISTS}" == "0" ]]; then
  echo "Applying baseline schema"
  apply_file "${BASELINE_FILE}"
fi

# 기존 DB도 전체 기준 스키마를 갖춘 경우에만 기준 버전을 등록합니다.
validate_tables "${BASELINE_TABLES[@]}"

query "
INSERT INTO schema_migration (version_no, description, checksum_sha256)
VALUES ('${BASELINE_VERSION}', 'baseline schema', '${BASELINE_CHECKSUM}')
ON DUPLICATE KEY UPDATE version_no = version_no;
"
APPLIED_BASELINE_CHECKSUM="$(query "SELECT checksum_sha256 FROM schema_migration WHERE version_no='${BASELINE_VERSION}' LIMIT 1;")"
if [[ "${APPLIED_BASELINE_CHECKSUM}" != "${BASELINE_CHECKSUM}" ]]; then
  echo "Checksum mismatch for baseline schema." >&2
  exit 1
fi

MIGRATIONS=(
  "2026072801|shared request rate limit|db/request_rate_limit_bucket.sql"
  "2026080401|customer authentication constraints|db/front_auth.sql"
  "2026080501|customer product activity|db/front_member_product_activity.sql"
  "2026080601|customer order history|db/front_member_order.sql"
  "2026080602|customer delivery addresses|db/front_member_delivery_address.sql"
  "2026081201|customer product reviews|db/front_product_review.sql"
  "2026081202|customer product review visibility|db/front_product_review_status.sql"
)

for migration in "${MIGRATIONS[@]}"; do
  IFS='|' read -r version description relative_path <<< "${migration}"
  file="${ROOT_DIR}/${relative_path}"
  expected_checksum="$(checksum "${file}")"
  applied_checksum="$(query "SELECT checksum_sha256 FROM schema_migration WHERE version_no='${version}' LIMIT 1;")"

  if [[ -n "${applied_checksum}" ]]; then
    if [[ "${applied_checksum}" != "${expected_checksum}" ]]; then
      echo "Checksum mismatch for migration ${version}: ${relative_path}" >&2
      exit 1
    fi
    continue
  fi

  echo "Applying ${version} ${description}"
  apply_file "${file}"
  query "
  INSERT INTO schema_migration (version_no, description, checksum_sha256)
  VALUES ('${version}', '${description}', '${expected_checksum}');
  "
done

validate_tables "${BASELINE_TABLES[@]}" request_rate_limit_bucket front_member_product_activity front_product_review

echo "Database schema is up to date."
