#!/usr/bin/env bash

set -euo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/common.sh"
source "${ROOT_DIR}/scripts/lib/mcp.sh"

if [[ "$(uname -s)" == 'Linux' && -z "${DISPLAY:-}" ]]; then
  printf '%s\n' \
    'Offline history UI verification requires a real X display for the pinned GTK2 ScritchUI backend.' \
    'No emulator was started. Run this lane in a graphical session; do not use a virtual X server.' >&2
  exit 69
fi

MCP_PORT="$(mcp_pick_port)"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/telegrammicro-history-ui.XXXXXX")"
ARTIFACT_DIR="${TELEGRAM_MICRO_UI_ARTIFACT_DIR:-${ROOT_DIR}/artifacts/ui-smoke/$(date -u +%Y%m%dT%H%M%SZ)-history-$$}"
STATE_DIR="${WORK_DIR}/state"
LOG_FILE="${ARTIFACT_DIR}/emulator.log"
SNAPSHOT_FILE="${WORK_DIR}/snapshot.json"
SCREENSHOT_RESPONSE_FILE="${WORK_DIR}/screenshot.json"
SCREENSHOT_FILE="${ARTIFACT_DIR}/history-message-state.png"
RUNNER_PID=''

cleanup() {
  if [[ -n "${RUNNER_PID}" ]] && kill -0 "${RUNNER_PID}" >/dev/null 2>&1; then
    kill "${RUNNER_PID}" >/dev/null 2>&1 || true
    wait "${RUNNER_PID}" >/dev/null 2>&1 || true
  fi
  "${ROOT_DIR}/scripts/stop-emulator.sh" "${MCP_PORT}" >/dev/null 2>&1 || true
  case "${WORK_DIR}" in
    "${TMPDIR:-/tmp}"/telegrammicro-history-ui.*) rm -rf "${WORK_DIR}" ;;
  esac
}
trap cleanup EXIT

mkdir -p "${ARTIFACT_DIR}" "${STATE_DIR}"
build_test_suite >/dev/null

SQUIRRELJME_MCP_PORT="${MCP_PORT}" \
TELEGRAM_MICRO_EMULATOR_STATE_DIR="${STATE_DIR}" \
SQUIRRELJME_VM_ARGS='-Xemulator:hosted' \
  run_built_suite HistoryFixture "${TEST_JAR}" >"${LOG_FILE}" 2>&1 &
RUNNER_PID=$!

mcp_wait_until_ready "${MCP_PORT}" telegrammicro-history-ui 120

for _ in 1 2 3 4 5 6 7 8 9 10; do
  if grep -q 'HISTORY FIXTURE READY:' "${LOG_FILE}"; then
    break
  fi
  sleep 1
done
if ! grep -q 'HISTORY FIXTURE READY: timestamps 22:13Z through 22:15Z; read then sent.' "${LOG_FILE}"; then
  printf 'The deterministic history fixture was not observed. See %s.\n' "${LOG_FILE}" >&2
  exit 1
fi

if ! mcp_call "${MCP_PORT}" \
  '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' \
  >"${SNAPSHOT_FILE}"; then
  printf 'The history fixture stopped before MCP inspection. See %s.\n' "${LOG_FILE}" >&2
  exit 1
fi
CAPTURE_ID="$(mcp_capture_id_from_snapshot "${SNAPSHOT_FILE}")"
if ! mcp_call "${MCP_PORT}" \
  "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${CAPTURE_ID}\"}}}" \
  >"${SCREENSHOT_RESPONSE_FILE}"; then
  printf 'The history fixture stopped before its screenshot. See %s.\n' "${LOG_FILE}" >&2
  exit 1
fi
mcp_write_screenshot "${SCREENSHOT_RESPONSE_FILE}" "${SCREENSHOT_FILE}"
if grep -Eq 'INCOMPLETE CODE|Exception in thread|VMException' "${LOG_FILE}"; then
  printf 'The history fixture logged an emulator failure. See %s.\n' "${LOG_FILE}" >&2
  exit 1
fi
if ! mcp_assert_png_has_content "${SCREENSHOT_FILE}"; then
  printf 'The history fixture capture did not contain a rendered screen. See %s.\n' \
    "${LOG_FILE}" >&2
  exit 1
fi

printf 'Offline history timestamp and delivery-state UI smoke passed.\n'
printf 'screenshot=%s\nlog=%s\n' "${SCREENSHOT_FILE}" "${LOG_FILE}"
