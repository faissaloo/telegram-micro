#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

TELEGRAM_MICRO_TEST_MODE=full
export TELEGRAM_MICRO_TEST_MODE

printf '%s\n' \
  'Running all tests inside the target VM. The large crypto vectors are a long-running soak.'
run_test_suite "$@"
