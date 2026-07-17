#!/usr/bin/env bash

set -euo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/mcp.sh"

MCP_PORT="$(mcp_pick_port)"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/telegrammicro-ui-smoke.XXXXXX")"
ARTIFACT_DIR="${TELEGRAM_MICRO_UI_ARTIFACT_DIR:-${ROOT_DIR}/artifacts/ui-smoke/$(date -u +%Y%m%dT%H%M%SZ)-$$}"
STATE_DIR="${WORK_DIR}/state"
LOG_FILE="${ARTIFACT_DIR}/emulator.log"
SNAPSHOT_FILE="${WORK_DIR}/snapshot.json"
SCREENSHOT_RESPONSE_FILE="${WORK_DIR}/screenshot.json"
SCREENSHOT_FILE="${ARTIFACT_DIR}/first-install.png"
RUNNER_PID=''

cleanup() {
  if [[ -n "${RUNNER_PID}" ]] && kill -0 "${RUNNER_PID}" >/dev/null 2>&1; then
    kill "${RUNNER_PID}" >/dev/null 2>&1 || true
    wait "${RUNNER_PID}" >/dev/null 2>&1 || true
  fi
  "${ROOT_DIR}/scripts/stop-emulator.sh" "${MCP_PORT}" >/dev/null 2>&1 || true
  case "${WORK_DIR}" in
    "${TMPDIR:-/tmp}"/telegrammicro-ui-smoke.*) rm -rf "${WORK_DIR}" ;;
  esac
}
trap cleanup EXIT

mkdir -p "${ARTIFACT_DIR}" "${STATE_DIR}"

SQUIRRELJME_MCP_PORT="${MCP_PORT}" \
TELEGRAM_MICRO_EMULATOR_STATE_DIR="${STATE_DIR}" \
SQUIRRELJME_VM_ARGS='-Xemulator:hosted' \
  "${ROOT_DIR}/scripts/run.sh" >"${LOG_FILE}" 2>&1 &
RUNNER_PID=$!

mcp_wait_until_ready "${MCP_PORT}" telegrammicro-ui-smoke 120

for _ in 1 2 3 4 5 6 7 8 9 10; do
  if grep -q 'ENTROPY REQUIRED: network startup deferred.' "${LOG_FILE}"; then
    break
  fi
  sleep 1
done
if ! grep -q 'ENTROPY REQUIRED: network startup deferred.' "${LOG_FILE}"; then
  printf 'The first-install entropy gate was not observed. See %s.\n' "${LOG_FILE}" >&2
  exit 1
fi
if grep -Eq 'BOOTSTRAP READY:|RECONNECT START:' "${LOG_FILE}"; then
  printf 'Network startup occurred before first-install entropy collection.\n' >&2
  exit 1
fi

mcp_call "${MCP_PORT}" \
  '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' \
  >"${SNAPSHOT_FILE}"
CAPTURE_ID="$(mcp_capture_id_from_snapshot "${SNAPSHOT_FILE}")"
mcp_call "${MCP_PORT}" \
  "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${CAPTURE_ID}\"}}}" \
  >"${SCREENSHOT_RESPONSE_FILE}"
mcp_write_screenshot "${SCREENSHOT_RESPONSE_FILE}" "${SCREENSHOT_FILE}"
if ! mcp_assert_png_has_content "${SCREENSHOT_FILE}"; then
  printf 'The MCP capture did not contain a rendered application screen. See %s.\n' \
    "${LOG_FILE}" >&2
  exit 1
fi

printf 'Offline first-install UI smoke passed.\n'
printf 'screenshot=%s\nlog=%s\n' "${SCREENSHOT_FILE}" "${LOG_FILE}"
