#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/mcp.sh"

expect_status() {
  local expected="$1"
  local label="$2"
  shift 2
  local actual=0

  "$@" >/dev/null 2>&1 || actual=$?
  if [[ "${actual}" != "${expected}" ]]; then
    printf '%s: expected exit %s, got %s.\n' \
      "${label}" "${expected}" "${actual}" >&2
    return 1
  fi
}

for validation_script in "${ROOT_DIR}"/scripts/validate-*.sh; do
  expect_status 64 "$(basename "${validation_script}") production gate" \
    env -u TELEGRAM_MICRO_LIVE \
      -u TELEGRAM_MICRO_LIVE_ACK \
      -u TELEGRAM_MICRO_TEST_PHONE \
      -u TELEGRAM_MICRO_VALIDATE_STATE_DIR \
      -u SQUIRRELJME_MCP_PORT \
      bash "${validation_script}"
done

expect_status 64 'explicit live gate still requires a phone' \
  env -u TELEGRAM_MICRO_TEST_PHONE \
    TELEGRAM_MICRO_LIVE=1 \
    TELEGRAM_MICRO_LIVE_ACK=telegram-production \
    bash "${ROOT_DIR}/scripts/validate-login-input.sh"

expect_status 64 'restore gate requires explicit state' \
  env -u TELEGRAM_MICRO_VALIDATE_STATE_DIR \
    TELEGRAM_MICRO_LIVE=1 \
    TELEGRAM_MICRO_LIVE_ACK=telegram-production \
    bash "${ROOT_DIR}/scripts/validate-restore-session.sh"

expect_status 64 'broad emulator cleanup requires an explicit flag' \
  env -u SQUIRRELJME_MCP_PORT bash "${ROOT_DIR}/scripts/stop-emulator.sh"

expect_status 1 'release tag must match the MIDlet version' \
  bash "${ROOT_DIR}/scripts/package-release.sh" v99.0.0-preview.1

if [[ "$(uname -s)" == 'Linux' ]]; then
  expect_status 69 'displayless UI verification fails before emulator startup' \
    env -u DISPLAY bash "${ROOT_DIR}/scripts/verify-ui.sh"
  expect_status 69 'displayless history UI verification fails before emulator startup' \
    env -u DISPLAY bash "${ROOT_DIR}/scripts/verify-history-ui.sh"
  expect_status 69 'displayless search UI verification fails before emulator startup' \
    env -u DISPLAY bash "${ROOT_DIR}/scripts/verify-search-ui.sh"
fi

dynamic_port="$(unset SQUIRRELJME_MCP_PORT; mcp_pick_port)"
mcp_validate_port "${dynamic_port}"
if SQUIRRELJME_MCP_PORT=invalid mcp_pick_port >/dev/null 2>&1; then
  printf 'Invalid configured MCP port was accepted.\n' >&2
  exit 1
fi

node --check "${ROOT_DIR}/scripts/support/gcf-loopback-server.js"

printf 'Automation safety checks passed.\n'
