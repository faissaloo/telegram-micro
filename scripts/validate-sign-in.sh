#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/live-validation.sh"
telegrammicro_live_setup sign-in phone persistent
LOGIN_CODE="${TELEGRAM_MICRO_TEST_CODE:-}"
LOGIN_PASSWORD="${TELEGRAM_MICRO_TEST_PASSWORD:-}"
unset TELEGRAM_MICRO_TEST_CODE TELEGRAM_MICRO_TEST_PASSWORD
LOG_FILE="${TELEGRAM_MICRO_VALIDATE_LOG:-${VALIDATION_ARTIFACT_DIR}/emulator.log}"
SCREENSHOT_PATH="${TELEGRAM_MICRO_VALIDATE_SCREENSHOT:-${VALIDATION_ARTIFACT_DIR}/sign-in-result.png}"
PROMPT_MODE="${TELEGRAM_MICRO_PROMPT_MODE:-auto}"
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
    if response="$(call_mcp '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"telegrammicro-signin","version":"1.0"}}}' 2>/dev/null)"; then
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
    const ids = {};
    let panelId = "";
    let captureId = "";
    let windowId = "";
    if (!visibleWindow)
      process.exit(2);
    windowId = visibleWindow.id || "";
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
            if (!item || !item.label)
              continue;
            ids[item.label] = item.id;
          }
        }
      }
    }
    if (!captureId)
      captureId = panelId || windowId;
    process.stdout.write(JSON.stringify({panelId, captureId, ids}));
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

type_text() {
  local panel_id="$1"
  local text="$2"
  local ch=''
  local code=''

  for ((i = 0; i < ${#text}; i++)); do
    ch="${text:i:1}"
    code="$(printf '%d' "'${ch}")"
    call_mcp "{\"jsonrpc\":\"2.0\",\"id\":$((100 + i)),\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.send_input\",\"arguments\":{\"id\":\"${panel_id}\",\"type\":262144,\"a\":${code}}}}" >/dev/null
    sleep 0.03
  done
}

write_screenshot() {
  local screenshot_json="$1"
  local output_png="$2"

  mcp_write_screenshot "${screenshot_json}" "${output_png}"
}

capture_visible_panel() {
  local state_json="$1"
  local screenshot_json="$2"
  local output_png="$3"
  local capture_id=''

  capture_id="$(json_get "${state_json}" "captureId")"
  if [[ -z "${capture_id}" ]]; then
    return 1
  fi

  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":90,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${capture_id}\"}}}" >"${screenshot_json}"
  write_screenshot "${screenshot_json}" "${output_png}"
}

can_prompt_with_zenity() {
  if [[ "${PROMPT_MODE}" == "none" ]]; then
    return 1
  fi
  if ! command -v zenity >/dev/null 2>&1; then
    return 1
  fi
  if [[ "${PROMPT_MODE}" == "zenity" ]]; then
    return 0
  fi
  [[ -n "${DISPLAY:-}" ]]
}

prompt_login_code() {
  if [[ -n "${LOGIN_CODE}" ]]; then
    return 0
  fi
  if ! can_prompt_with_zenity; then
    printf 'Set TELEGRAM_MICRO_TEST_CODE or run with TELEGRAM_MICRO_PROMPT_MODE=zenity in a desktop session.\n' >&2
    return 1
  fi

  LOGIN_CODE="$(
    zenity --entry \
      --title="Telegram Micro Verification Code" \
      --text="Telegram Micro requested a fresh login code.\n\nEnter the verification code to continue the MCP-driven sign-in flow." \
      --entry-text=""
  )" || return 1

  LOGIN_CODE="${LOGIN_CODE//$'\r'/}"
  LOGIN_CODE="${LOGIN_CODE//$'\n'/}"
  [[ -n "${LOGIN_CODE}" ]]
}

prompt_password() {
  if [[ -n "${LOGIN_PASSWORD}" ]]; then
    return 0
  fi
  if ! can_prompt_with_zenity; then
    printf 'Set TELEGRAM_MICRO_TEST_PASSWORD or run with TELEGRAM_MICRO_PROMPT_MODE=zenity in a desktop session.\n' >&2
    return 1
  fi

  LOGIN_PASSWORD="$(
    zenity --password \
      --title="Telegram Micro Two-Factor Password" \
      --text="Telegram requested the account two-factor password.\n\nEnter it to continue the MCP-driven sign-in flow."
  )" || return 1

  LOGIN_PASSWORD="${LOGIN_PASSWORD//$'\r'/}"
  LOGIN_PASSWORD="${LOGIN_PASSWORD//$'\n'/}"
  [[ -n "${LOGIN_PASSWORD}" ]]
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
STATE_JSON="$(mktemp)"
SCREENSHOT_JSON="$(mktemp)"

call_mcp '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
PANEL_ID="$(json_get "${STATE_JSON}" "panelId")"
SUBMIT_ID="$(json_get "${STATE_JSON}" "ids.Submit")"

if [[ -z "${PANEL_ID}" || -z "${SUBMIT_ID}" ]]; then
  printf 'Could not resolve the phone-entry controls.\n' >&2
  exit 65
fi

type_text "${PANEL_ID}" "${PHONE_NUMBER}"
call_mcp "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.activate\",\"arguments\":{\"id\":\"${SUBMIT_ID}\"}}}" >/dev/null

for _ in $(seq 1 180); do
  call_mcp '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
  PANEL_ID="$(json_get "${STATE_JSON}" "panelId")"
  SUBMIT_ID="$(json_get "${STATE_JSON}" "ids.Submit")"
  BACK_ID="$(json_get "${STATE_JSON}" "ids.Back")"
  if [[ -n "${PANEL_ID}" && -n "${SUBMIT_ID}" && -n "${BACK_ID}" ]]; then
    break
  fi
  if [[ -f "${LOG_FILE}" ]] && grep -q 'ERROR: RPC ' "${LOG_FILE}"; then
    LAST_RPC_ERROR="$(grep 'ERROR: RPC ' "${LOG_FILE}" | tail -n 1 | sed 's/^.*ERROR: RPC //')"
    if [[ "${LAST_RPC_ERROR}" != *"PHONE_MIGRATE_"* ]]; then
      capture_visible_panel "${STATE_JSON}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}" || true
      printf 'Telegram returned an RPC error before code entry.\n' >&2
      printf 'rpc_error=%s\n' "${LAST_RPC_ERROR}" >&2
      printf 'log=%s\n' "${LOG_FILE}" >&2
      printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
      exit 66
    fi
  fi
  sleep 1
done

if ! prompt_login_code; then
  capture_visible_panel "${STATE_JSON}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}" || true
  printf 'Verification code entry was cancelled or unavailable.\n' >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
  exit 64
fi

type_text "${PANEL_ID}" "${LOGIN_CODE}"
call_mcp "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.activate\",\"arguments\":{\"id\":\"${SUBMIT_ID}\"}}}" >/dev/null

PASSWORD_REQUIRED=0
AUTHORIZED=0
PHONE_CODE_ERROR=''
PASSWORD_ERROR=''
OTHER_RPC_ERROR=''

for _ in $(seq 1 180); do
  if [[ -f "${LOG_FILE}" ]]; then
    if grep -q 'ERROR: RPC 400: PHONE_CODE_' "${LOG_FILE}"; then
      PHONE_CODE_ERROR="$(grep 'ERROR: RPC 400: PHONE_CODE_' "${LOG_FILE}" | tail -n 1 | sed 's/^.*ERROR: RPC 400: //')"
      break
    fi
    if grep -q 'ERROR: RPC 401: SESSION_PASSWORD_NEEDED' "${LOG_FILE}"; then
      PASSWORD_REQUIRED=1
      break
    fi
    if grep -q 'ERROR: RPC 400: PASSWORD_HASH_INVALID' "${LOG_FILE}"; then
      PASSWORD_ERROR='PASSWORD_HASH_INVALID'
      break
    fi
    if grep -q 'ERROR: RPC ' "${LOG_FILE}" && ! grep -q 'PHONE_MIGRATE_' "${LOG_FILE}"; then
      OTHER_RPC_ERROR="$(grep 'ERROR: RPC ' "${LOG_FILE}" | tail -n 1 | sed 's/^.*ERROR: RPC //')"
      if [[ "${OTHER_RPC_ERROR}" != "401: SESSION_PASSWORD_NEEDED" ]]; then
        break
      fi
      OTHER_RPC_ERROR=''
    fi
  fi

  call_mcp '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
  if [[ -n "$(json_get "${STATE_JSON}" "ids.Open")" && -n "$(json_get "${STATE_JSON}" "ids.Status")" ]]; then
    AUTHORIZED=1
    break
  fi
  sleep 1
done

if [[ -n "${PHONE_CODE_ERROR}" ]]; then
  call_mcp '{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
  capture_visible_panel "${STATE_JSON}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}" || true
  printf 'sign_in_error=%s\n' "${PHONE_CODE_ERROR}" >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
  exit 67
fi

if [[ -n "${OTHER_RPC_ERROR}" ]]; then
  call_mcp '{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
  capture_visible_panel "${STATE_JSON}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}" || true
  printf 'rpc_error=%s\n' "${OTHER_RPC_ERROR}" >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
  exit 68
fi

if [[ "${PASSWORD_REQUIRED}" == "1" ]]; then
  if ! prompt_password; then
    call_mcp '{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
    resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
    capture_visible_panel "${STATE_JSON}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}" || true
    printf 'Password prompt was cancelled or unavailable.\n' >&2
    printf 'log=%s\n' "${LOG_FILE}" >&2
    printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
    exit 69
  fi

  for _ in $(seq 1 120); do
    call_mcp '{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
    resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
    PANEL_ID="$(json_get "${STATE_JSON}" "panelId")"
    SUBMIT_ID="$(json_get "${STATE_JSON}" "ids.Submit")"
    BACK_ID="$(json_get "${STATE_JSON}" "ids.Back")"
    if [[ -n "${PANEL_ID}" && -n "${SUBMIT_ID}" && -n "${BACK_ID}" ]]; then
      break
    fi
    sleep 1
  done

  type_text "${PANEL_ID}" "${LOGIN_PASSWORD}"
  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.activate\",\"arguments\":{\"id\":\"${SUBMIT_ID}\"}}}" >/dev/null

  for _ in $(seq 1 180); do
    if [[ -f "${LOG_FILE}" ]] && grep -q 'ERROR: RPC 400: PASSWORD_HASH_INVALID' "${LOG_FILE}"; then
      PASSWORD_ERROR='PASSWORD_HASH_INVALID'
      break
    fi
    if [[ -f "${LOG_FILE}" ]] && grep -q 'ERROR: RPC ' "${LOG_FILE}" && ! grep -q 'SESSION_PASSWORD_NEEDED' "${LOG_FILE}"; then
      OTHER_RPC_ERROR="$(grep 'ERROR: RPC ' "${LOG_FILE}" | tail -n 1 | sed 's/^.*ERROR: RPC //')"
      if [[ "${OTHER_RPC_ERROR}" != "401: SESSION_PASSWORD_NEEDED" ]]; then
        break
      fi
      OTHER_RPC_ERROR=''
    fi

    call_mcp '{"jsonrpc":"2.0","id":12,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
    resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
    if [[ -n "$(json_get "${STATE_JSON}" "ids.Open")" && -n "$(json_get "${STATE_JSON}" "ids.Status")" ]]; then
      AUTHORIZED=1
      break
    fi
    sleep 1
  done
fi

call_mcp '{"jsonrpc":"2.0","id":13,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
capture_visible_panel "${STATE_JSON}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}" || true

if [[ -n "${PASSWORD_ERROR}" ]]; then
  printf 'password_error=%s\n' "${PASSWORD_ERROR}" >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
  exit 70
fi

if [[ -n "${OTHER_RPC_ERROR}" ]]; then
  printf 'rpc_error=%s\n' "${OTHER_RPC_ERROR}" >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
  exit 71
fi

if [[ "${AUTHORIZED}" != "1" ]]; then
  printf 'Timed out waiting for the authorized shell.\n' >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  printf 'screenshot=%s\n' "${SCREENSHOT_PATH}" >&2
  exit 72
fi

printf 'phone_entered=1\n'
printf 'authorized=1\n'
printf 'state_dir=%s\n' "${VALIDATION_STATE_DIR}"
printf 'screenshot=%s\n' "${SCREENSHOT_PATH}"
printf 'log=%s\n' "${LOG_FILE}"
