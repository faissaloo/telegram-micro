#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

TEST_LANE='full'
RUN_UI=0

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --quick) TEST_LANE='quick' ;;
    --full) TEST_LANE='full' ;;
    --ui) RUN_UI=1 ;;
    *)
      printf 'Usage: %s [--quick|--full] [--ui]\n' "$0" >&2
      exit 64
      ;;
  esac
  shift
done

printf '[verify] shell syntax\n'
while IFS= read -r shell_file; do
  bash -n "${shell_file}"
done < <(find scripts .agents -type f -name '*.sh' 2>/dev/null | sort)
if command -v shellcheck >/dev/null 2>&1; then
  while IFS= read -r shell_file; do
    shellcheck --severity=warning "${shell_file}"
  done < <(find scripts .agents -type f -name '*.sh' 2>/dev/null | sort)
fi

printf '[verify] source policy\n'
./scripts/check-source-policy.sh
./scripts/test-automation.sh
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git diff --check
fi

printf '[verify] production build\n'
./scripts/build.sh
./scripts/check-device-budget.sh

printf '[verify] %s tests\n' "${TEST_LANE}"
if [[ "${TEST_LANE}" == 'quick' ]]; then
  ./scripts/test.sh
else
  ./scripts/test-full.sh
fi

if [[ "${RUN_UI}" == '1' ]]; then
  printf '[verify] offline first-install UI smoke\n'
  ./scripts/verify-ui.sh
  printf '[verify] offline history UI smoke\n'
  ./scripts/verify-history-ui.sh
  printf '[verify] offline search UI smoke\n'
  ./scripts/verify-search-ui.sh
fi

printf '[verify] all requested gates passed\n'
