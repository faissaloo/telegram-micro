#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/live-validation.sh"
telegrammicro_live_setup restore-session none required
LOG_FILE="${TELEGRAM_MICRO_VALIDATE_LOG:-${VALIDATION_ARTIFACT_DIR}/emulator.log}"
SCREENSHOT_PATH="${TELEGRAM_MICRO_VALIDATE_SCREENSHOT:-${VALIDATION_ARTIFACT_DIR}/restore-dialogs.png}"
RUNNER_PID=''

cleanup() {
  if [[ -n "${RUNNER_PID}" ]] && kill -0 "${RUNNER_PID}" >/dev/null 2>&1; then
    kill "${RUNNER_PID}" >/dev/null 2>&1 || true
    wait "${RUNNER_PID}" >/dev/null 2>&1 || true
  fi
  "${ROOT_DIR}/scripts/stop-emulator.sh" "${MCP_PORT}" >/dev/null 2>&1 || true
  telegrammicro_live_cleanup
}

call_mcp() {
  local payload="$1"

  mcp_call "${MCP_PORT}" "${payload}"
}

wait_for_mcp() {
  local response=''

  for _ in $(seq 1 120); do
    if response="$(call_mcp '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"telegrammicro-restore","version":"1.0"}}}' 2>/dev/null)"; then
      printf '%s\n' "${response}" >/dev/null
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for MCP on port %s.\n' "${MCP_PORT}" >&2
  return 1
}

wait_for_log_marker() {
  local marker="$1"
  local seconds="${2:-180}"

  for _ in $(seq 1 "${seconds}"); do
    if [[ -f "${LOG_FILE}" ]] && grep -q "${marker}" "${LOG_FILE}"; then
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for %s in %s.\n' "${marker}" "${LOG_FILE}" >&2
  return 1
}

resolve_visible_state() {
  local snapshot_json="$1"

  node -e '
    const fs = require("fs");
    const response = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const payload = JSON.parse(response.result.content[0].text);
    const controls = payload.controls || [];
    const byId = new Map(controls.map((control) => [control.id, control]));
    const visibleWindow = controls.find((control) =>
      control.kind === "window" && control.visible && Array.isArray(control.children));
    let captureId = "";
    let windowId = "";
    const labels = {};
    if (!visibleWindow)
      process.exit(2);
    windowId = visibleWindow.id || "";
    for (const childId of visibleWindow.children) {
      const child = byId.get(childId);
      if (!child)
        continue;
      if (!captureId &&
          Array.isArray(child.actions) &&
          child.actions.indexOf("screenshot") >= 0 &&
          child.kind !== "menu-bar" &&
          child.kind !== "menu" &&
          child.kind !== "menu-item") {
        captureId = child.id;
      }
      if (child.kind === "menu-bar" && Array.isArray(child.children)) {
        for (const menuId of child.children) {
          const menu = byId.get(menuId);
          if (!menu || !Array.isArray(menu.children))
            continue;
          for (const itemId of menu.children) {
            const item = byId.get(itemId);
            if (item && item.label)
              labels[item.label] = item.id;
          }
        }
      }
    }
    if (!captureId)
      captureId = windowId;
    process.stdout.write(JSON.stringify({captureId, labels}));
  ' "${snapshot_json}"
}

json_get() {
  local json_file="$1"
  local expr="$2"

  node -e '
    const fs = require("fs");
    const value = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const parts = process.argv[2].split(".");
    let current = value;
    for (const part of parts) {
      if (!part.length)
        continue;
      current = current ? current[part] : "";
    }
    if (typeof current === "string")
      process.stdout.write(current);
  ' "${json_file}" "${expr}"
}

write_screenshot() {
  local screenshot_json="$1"
  local output_png="$2"

  mcp_write_screenshot "${screenshot_json}" "${output_png}"
}

capture_screenshot() {
  local target_id="$1"
  local screenshot_json="$2"
  local output_png="$3"

  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${target_id}\"}}}" >"${screenshot_json}" ||
    return 1
  write_screenshot "${screenshot_json}" "${output_png}" || return 1
}

mkdir -p "$(dirname "${LOG_FILE}")" \
  "$(dirname "${SCREENSHOT_PATH}")"
rm -f "${LOG_FILE}" "${SCREENSHOT_PATH}"
trap cleanup EXIT

TELEGRAM_MICRO_EMULATOR_STATE_DIR="${VALIDATION_STATE_DIR}" \
SQUIRRELJME_MCP_PORT="${MCP_PORT}" \
  "${ROOT_DIR}/scripts/run.sh" >"${LOG_FILE}" 2>&1 &
RUNNER_PID=$!

wait_for_mcp
wait_for_log_marker 'RESTORE READY:' 180
wait_for_log_marker 'DIALOGS READY:' 180

SNAPSHOT_JSON="$(mktemp)"
STATE_JSON="$(mktemp)"
SCREENSHOT_JSON="$(mktemp)"

call_mcp '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
CAPTURE_ID="$(json_get "${STATE_JSON}" "captureId")"

if [[ -z "${CAPTURE_ID}" ]]; then
  printf 'Could not resolve a capture target for the restored dialogs shell.\n' >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  exit 65
fi

if ! capture_screenshot "${CAPTURE_ID}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}"; then
  if ! capture_screenshot "object:window" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}"; then
    printf 'Could not capture the restored dialogs shell.\n' >&2
    printf 'log=%s\n' "${LOG_FILE}" >&2
    exit 66
  fi
fi

printf 'restored=1\n'
printf 'state_dir=%s\n' "${VALIDATION_STATE_DIR}"
printf 'capture_id=%s\n' "${CAPTURE_ID}"
printf 'screenshot=%s\n' "${SCREENSHOT_PATH}"
printf 'log=%s\n' "${LOG_FILE}"
