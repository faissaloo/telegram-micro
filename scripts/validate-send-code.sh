#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/live-validation.sh"
telegrammicro_live_setup send-code phone transient
LOG_FILE="${TELEGRAM_MICRO_VALIDATE_LOG:-${VALIDATION_ARTIFACT_DIR}/emulator.log}"
SCREENSHOT_PATH="${TELEGRAM_MICRO_VALIDATE_SCREENSHOT:-${VALIDATION_ARTIFACT_DIR}/code-stage.png}"
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
    if response="$(call_mcp '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"telegrammicro-sendcode","version":"1.0"}}}' 2>/dev/null)"; then
      printf '%s\n' "${response}" >/dev/null
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for MCP on port %s.\n' "${MCP_PORT}" >&2
  return 1
}

wait_for_bootstrap_ready() {
  for _ in $(seq 1 180); do
    if [[ -f "${LOG_FILE}" ]] && grep -q 'BOOTSTRAP READY:' "${LOG_FILE}"; then
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for BOOTSTRAP READY in %s.\n' "${LOG_FILE}" >&2
  return 1
}

resolve_phone_form_controls() {
  local snapshot_json="$1"

  node -e '
    const fs = require("fs");
    const response = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const payload = JSON.parse(response.result.content[0].text);
    const controls = payload.controls || [];
    const byId = new Map(controls.map((control) => [control.id, control]));
    const visibleWindow = controls.find((control) =>
      control.kind === "window" && control.visible && Array.isArray(control.children));
    let panelId = "";
    let submitId = "";
    if (!visibleWindow)
      process.exit(2);
    for (const childId of visibleWindow.children) {
      const child = byId.get(childId);
      if (!child)
        continue;
      if (!panelId &&
          child.kind === "panel" &&
          Array.isArray(child.actions) &&
          child.actions.indexOf("send_input") >= 0) {
        panelId = child.id;
      }
      if (child.kind === "menu-bar" && Array.isArray(child.children)) {
        for (const menuId of child.children) {
          const menu = byId.get(menuId);
          if (!menu || !Array.isArray(menu.children))
            continue;
          for (const itemId of menu.children) {
            const item = byId.get(itemId);
            if (item && item.label === "Submit" &&
                Array.isArray(item.actions) &&
                item.actions.indexOf("activate") >= 0) {
              submitId = item.id;
            }
          }
        }
      }
    }
    if (!panelId || !submitId)
      process.exit(3);
    process.stdout.write(JSON.stringify({panelId, submitId}));
  ' "${snapshot_json}"
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
    let panelId = "";
    let hasSubmit = false;
    let hasBack = false;
    if (!visibleWindow)
      process.exit(2);
    for (const childId of visibleWindow.children) {
      const child = byId.get(childId);
      if (!child)
        continue;
      if (!panelId &&
          child.kind === "panel" &&
          Array.isArray(child.actions) &&
          child.actions.indexOf("send_input") >= 0) {
        panelId = child.id;
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
              hasSubmit = true;
            else if (item.label === "Back")
              hasBack = true;
          }
        }
      }
    }
    if (!panelId || !hasSubmit || !hasBack)
      process.exit(3);
    process.stdout.write(panelId);
  ' "${snapshot_json}"
}

type_text() {
  local panel_id="$1"
  local text="$2"
  local ch=''
  local code=''

  for ((i = 0; i < ${#text}; i++)); do
    ch="${text:i:1}"
    code="$(printf '%d' "'${ch}")"
    call_mcp "{\"jsonrpc\":\"2.0\",\"id\":$((100 + i)),\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.send_input\",\"arguments\":{\"id\":\"${panel_id}\",\"type\":262144,\"a\":${code}}}}" >/dev/null
  done
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

TELEGRAM_MICRO_EMULATOR_STATE_DIR="${VALIDATION_STATE_DIR}" \
SQUIRRELJME_MCP_PORT="${MCP_PORT}" \
  "${ROOT_DIR}/scripts/run.sh" >"${LOG_FILE}" 2>&1 &
RUNNER_PID=$!

wait_for_mcp
wait_for_bootstrap_ready

SNAPSHOT_JSON="$(mktemp)"
SCREENSHOT_JSON="$(mktemp)"
PHONE_FORM_JSON="$(mktemp)"

call_mcp '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
resolve_phone_form_controls "${SNAPSHOT_JSON}" >"${PHONE_FORM_JSON}"
PANEL_ID="$(node -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).panelId);' "${PHONE_FORM_JSON}")"
SUBMIT_ID="$(node -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).submitId);' "${PHONE_FORM_JSON}")"

type_text "${PANEL_ID}" "${PHONE_NUMBER}"
call_mcp "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.activate\",\"arguments\":{\"id\":\"${SUBMIT_ID}\"}}}" >/dev/null

for _ in $(seq 1 120); do
  call_mcp '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  if CODE_PANEL_ID="$(resolve_code_stage "${SNAPSHOT_JSON}" 2>/dev/null)"; then
    call_mcp "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${CODE_PANEL_ID}\"}}}" >"${SCREENSHOT_JSON}"
    write_screenshot "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}"
    printf 'phone_entered=1\n'
    printf 'panel_id=%s\n' "${PANEL_ID}"
    printf 'submit_id=%s\n' "${SUBMIT_ID}"
    printf 'code_panel_id=%s\n' "${CODE_PANEL_ID}"
    printf 'screenshot=%s\n' "${SCREENSHOT_PATH}"
    printf 'log=%s\n' "${LOG_FILE}"
    exit 0
  fi
  sleep 1
done

call_mcp '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"scritch.screenshot","arguments":{"id":"object:window"}}}' >/dev/null 2>&1 || true
printf 'Timed out waiting for the login code stage.\n' >&2
printf 'log=%s\n' "${LOG_FILE}" >&2
exit 1
