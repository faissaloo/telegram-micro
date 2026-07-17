#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/live-validation.sh"
telegrammicro_live_setup code-restore phone transient
LOG_FILE="${TELEGRAM_MICRO_VALIDATE_LOG:-${VALIDATION_ARTIFACT_DIR}/emulator.log}"
SCREENSHOT_PATH="${TELEGRAM_MICRO_VALIDATE_SCREENSHOT:-${VALIDATION_ARTIFACT_DIR}/code-restore.png}"
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
    if response="$(call_mcp '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"telegrammicro-code-restore","version":"1.0"}}}' 2>/dev/null)"; then
      printf '%s\n' "${response}" >/dev/null
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for MCP on port %s.\n' "${MCP_PORT}" >&2
  return 1
}

resolve_code_stage() {
  local snapshot_json="$1"

  node -e '
    const fs = require("fs");
    const response = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const payload = JSON.parse(response.result.content[0].text);
    const controls = payload.controls || [];
    const byId = new Map(controls.map((control) => [control.id, control]));
    const visibleWindow = controls.find((control) =>
      control.kind === "window" && control.visible && Array.isArray(control.children));
    const out = {panelId: "", hasSubmit: false, hasBack: false, hasExit: false};
    if (!visibleWindow)
      process.exit(2);
    for (const childId of visibleWindow.children) {
      const child = byId.get(childId);
      if (!child)
        continue;
      if (!out.panelId &&
          child.kind === "panel" &&
          Array.isArray(child.actions) &&
          child.actions.indexOf("send_input") >= 0) {
        out.panelId = child.id;
      }
      if (child.kind === "menu-bar" && Array.isArray(child.children)) {
        for (const menuId of child.children) {
          const menu = byId.get(menuId);
          if (!menu || !Array.isArray(menu.children))
            continue;
          for (const itemId of menu.children) {
            const item = byId.get(itemId);
            if (!item)
              continue;
            if (item.label === "Submit")
              out.hasSubmit = true;
            else if (item.label === "Back")
              out.hasBack = true;
            else if (item.label === "Exit")
              out.hasExit = true;
          }
        }
      }
    }
    if (!out.panelId || !out.hasSubmit || !out.hasBack || !out.hasExit)
      process.exit(3);
    process.stdout.write(JSON.stringify(out));
  ' "${snapshot_json}"
}

write_screenshot() {
  local screenshot_json="$1"
  local output_png="$2"

  mcp_write_screenshot "${screenshot_json}" "${output_png}"
}

mkdir -p "${VALIDATION_STATE_DIR}" "$(dirname "${LOG_FILE}")" \
  "$(dirname "${SCREENSHOT_PATH}")"
rm -f "${LOG_FILE}" "${SCREENSHOT_PATH}"
trap cleanup EXIT

TELEGRAM_MICRO_VALIDATE_STATE_DIR="${VALIDATION_STATE_DIR}" \
  TELEGRAM_MICRO_TEST_PHONE="${PHONE_NUMBER}" \
  SQUIRRELJME_MCP_PORT="${MCP_PORT}" \
  TELEGRAM_MICRO_VALIDATE_LOG="${LOG_FILE}.sendcode" \
  TELEGRAM_MICRO_VALIDATE_SCREENSHOT="${SCREENSHOT_PATH}.sendcode.png" \
  "${ROOT_DIR}/scripts/validate-send-code.sh" >/dev/null

TELEGRAM_MICRO_EMULATOR_STATE_DIR="${VALIDATION_STATE_DIR}" \
SQUIRRELJME_MCP_PORT="${MCP_PORT}" \
  "${ROOT_DIR}/scripts/run.sh" >"${LOG_FILE}" 2>&1 &
RUNNER_PID=$!

wait_for_mcp

SNAPSHOT_JSON="$(mktemp)"
STATE_JSON="$(mktemp)"
SCREENSHOT_JSON="$(mktemp)"

for _ in $(seq 1 120); do
  call_mcp '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  if resolve_code_stage "${SNAPSHOT_JSON}" >"${STATE_JSON}" 2>/dev/null; then
    PANEL_ID="$(node -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).panelId);' "${STATE_JSON}")"
    call_mcp "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${PANEL_ID}\"}}}" >"${SCREENSHOT_JSON}"
    write_screenshot "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}"
    printf 'restored_code_stage=1\n'
    printf 'state_dir=%s\n' "${VALIDATION_STATE_DIR}"
    printf 'panel_id=%s\n' "${PANEL_ID}"
    printf 'screenshot=%s\n' "${SCREENSHOT_PATH}"
    printf 'log=%s\n' "${LOG_FILE}"
    exit 0
  fi
  sleep 1
done

printf 'Timed out waiting for the restored code-entry stage.\n' >&2
printf 'log=%s\n' "${LOG_FILE}" >&2
exit 1
