#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_E2E="${RUN_E2E:-false}"

cd "${ROOT_DIR}"

if [[ "${RUN_E2E}" != "true" && "${RUN_E2E}" != "false" ]]; then
  echo "RUN_E2E는 true 또는 false여야 합니다." >&2
  exit 1
fi

git diff --check
npm ci
npm run test:admin-js
./gradlew clean test bootJar --no-daemon

if [[ "${RUN_E2E}" == "true" ]]; then
  npx playwright test
fi

echo "Release verification completed successfully."
