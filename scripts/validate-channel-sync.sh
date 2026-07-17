#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/lib/live-validation.sh"
telegrammicro_live_setup channel-sync none required
LOG_FILE="${TELEGRAM_MICRO_VALIDATE_LOG:-${VALIDATION_ARTIFACT_DIR}/emulator.log}"
SCREENSHOT_PATH="${TELEGRAM_MICRO_VALIDATE_SCREENSHOT:-${VALIDATION_ARTIFACT_DIR}/channel-sync.png}"
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
    if response="$(call_mcp '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"telegrammicro-channel-sync","version":"1.0"}}}' 2>/dev/null)"; then
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
    if [[ -f "${LOG_FILE}" ]] && grep -qF "${marker}" "${LOG_FILE}"; then
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for %s in %s.\n' "${marker}" "${LOG_FILE}" >&2
  return 1
}

count_log_marker() {
  local marker="$1"

  if [[ ! -f "${LOG_FILE}" ]]; then
    printf '0'
    return
  fi
  grep -cF "${marker}" "${LOG_FILE}" || true
}

wait_for_log_increment() {
  local marker="$1"
  local previous_count="$2"
  local seconds="${3:-20}"
  local current=0

  for _ in $(seq 1 "${seconds}"); do
    current="$(count_log_marker "${marker}")"
    if [[ "${current}" -gt "${previous_count}" ]]; then
      return 0
    fi
    sleep 1
  done

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
    let windowLabel = "";
    let listId = "";
    let inputId = "";
    let choices = [];
    const labels = {};

    if (!visibleWindow)
      process.exit(2);

    windowLabel = visibleWindow.label || "";
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
      if (!inputId &&
          Array.isArray(child.actions) &&
          child.actions.indexOf("send_input") >= 0) {
        inputId = child.id;
      }
      if (!listId) {
        const queue = [child];
        while (queue.length > 0) {
          const current = queue.shift();
          if (!current)
            continue;
          if (!inputId &&
              Array.isArray(current.actions) &&
              current.actions.indexOf("send_input") >= 0)
            inputId = current.id;
          if (current.kind === "list" && Array.isArray(current.actions) &&
              current.actions.indexOf("select_choice") >= 0) {
            listId = current.id;
            choices = current.choices || [];
            break;
          }
          if (Array.isArray(current.children)) {
            for (const nestedId of current.children) {
              const nested = byId.get(nestedId);
              if (nested)
                queue.push(nested);
            }
          }
        }
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
      captureId = visibleWindow.id || "";
    process.stdout.write(JSON.stringify({
      windowLabel,
      captureId,
      listId,
      inputId,
      choiceCount: choices.length,
      choices,
      labels
    }));
  ' "${snapshot_json}"
}

json_get() {
  local json_file="$1"
  local expr="$2"

  node -e '
    const fs = require("fs");
    let current = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const parts = process.argv[2].split(".");
    for (const part of parts) {
      if (!part.length)
        continue;
      if (/^[0-9]+$/.test(part))
        current = Array.isArray(current) ? current[Number(part)] : undefined;
      else
        current = current ? current[part] : undefined;
    }
    if (current === undefined || current === null)
      process.exit(0);
    if (typeof current === "object")
      process.stdout.write(JSON.stringify(current));
    else
      process.stdout.write(String(current));
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

  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":90,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.screenshot\",\"arguments\":{\"id\":\"${target_id}\"}}}" >"${screenshot_json}" ||
    return 1
  write_screenshot "${screenshot_json}" "${output_png}" || return 1
}

activate_control() {
  local control_id="$1"

  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":91,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.activate\",\"arguments\":{\"id\":\"${control_id}\"}}}" >/dev/null
}

select_choice() {
  local control_id="$1"
  local index="$2"

  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":92,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.select_choice\",\"arguments\":{\"id\":\"${control_id}\",\"index\":${index}}}}" >/dev/null
}

send_key() {
  local control_id="$1"
  local key_code="$2"

  call_mcp "{\"jsonrpc\":\"2.0\",\"id\":94,\"method\":\"tools/call\",\"params\":{\"name\":\"scritch.send_input\",\"arguments\":{\"id\":\"${control_id}\",\"type\":262144,\"a\":${key_code}}}}" >/dev/null
}

wait_for_window_label() {
  local expected_label="$1"
  local seconds="${2:-20}"
  local snapshot_json state_json window_label

  for _ in $(seq 1 "${seconds}"); do
    snapshot_json="$(mktemp)"
    state_json="$(mktemp)"
    call_mcp '{"jsonrpc":"2.0","id":93,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${snapshot_json}"
    resolve_visible_state "${snapshot_json}" >"${state_json}"
    window_label="$(json_get "${state_json}" "windowLabel")"
    rm -f "${snapshot_json}" "${state_json}"
    if [[ "${window_label}" == "${expected_label}" ]]; then
      return 0
    fi
    sleep 1
  done

  return 1
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

LIST_ID="$(json_get "${STATE_JSON}" "listId")"
INPUT_ID="$(json_get "${STATE_JSON}" "inputId")"
CHOICE_COUNT="$(json_get "${STATE_JSON}" "choiceCount")"

if [[ -z "${LIST_ID}" && -z "${INPUT_ID}" ]]; then
  printf 'Could not resolve the dialogs list.\n' >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  exit 65
fi

CHANNEL_INDEX=''
CHANNEL_LABEL=''
if [[ -n "${LIST_ID}" && -n "${CHOICE_COUNT}" && "${CHOICE_COUNT}" -gt 0 ]]; then
for i in $(seq 0 $((CHOICE_COUNT - 1))); do
  BEFORE_COUNT="$(count_log_marker 'CHANNEL DIFF:')"
  CHANNEL_LABEL="$(json_get "${STATE_JSON}" "choices.${i}.label")"

  select_choice "${LIST_ID}" "${i}"
  activate_control "${LIST_ID}"

  if wait_for_log_increment 'CHANNEL DIFF:' "${BEFORE_COUNT}" 20; then
    CHANNEL_INDEX="${i}"
    break
  fi

  if wait_for_window_label 'History' 20; then
    call_mcp '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
    resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
    DIALOGS_ID="$(json_get "${STATE_JSON}" "labels.Dialogs")"
    if [[ -n "${DIALOGS_ID}" ]]; then
      activate_control "${DIALOGS_ID}"
      wait_for_window_label 'Dialogs' 20 || true
    fi
  fi

  call_mcp '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
  resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
done
else
  DIALOG_COUNT="$(grep -F 'DIALOGS READY:' "${LOG_FILE}" | tail -n 1 | sed -n 's/.*count=\([0-9][0-9]*\).*/\1/p')"
  if [[ -z "${DIALOG_COUNT}" || "${DIALOG_COUNT}" -le 0 ]]; then
    DIALOG_COUNT=12
  fi
  for i in $(seq 0 $((DIALOG_COUNT - 1))); do
    BEFORE_COUNT="$(count_log_marker 'CHANNEL DIFF:')"
    CHANNEL_LABEL="dialog-${i}"

    if [[ "${i}" -gt 0 ]]; then
      send_key "${INPUT_ID}" 56
      sleep 1
    fi
    send_key "${INPUT_ID}" 53

    if wait_for_log_increment 'CHANNEL DIFF:' "${BEFORE_COUNT}" 20; then
      CHANNEL_INDEX="${i}"
      break
    fi

    if wait_for_window_label 'History' 20; then
      call_mcp '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
      resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
      DIALOGS_ID="$(json_get "${STATE_JSON}" "labels.Dialogs")"
      if [[ -n "${DIALOGS_ID}" ]]; then
        activate_control "${DIALOGS_ID}"
        wait_for_window_label 'Dialogs' 20 || true
        call_mcp '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
        resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
        INPUT_ID="$(json_get "${STATE_JSON}" "inputId")"
      fi
    fi
  done
fi

if [[ -z "${CHANNEL_INDEX}" ]]; then
  printf 'No dialog in the bounded list triggered CHANNEL DIFF during validation.\n' >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  exit 66
fi

call_mcp '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"scritch.snapshot","arguments":{}}}' >"${SNAPSHOT_JSON}"
resolve_visible_state "${SNAPSHOT_JSON}" >"${STATE_JSON}"
CAPTURE_ID="$(json_get "${STATE_JSON}" "captureId")"

if [[ -z "${CAPTURE_ID}" ]]; then
  printf 'Could not resolve a capture target for the channel screen.\n' >&2
  printf 'log=%s\n' "${LOG_FILE}" >&2
  exit 67
fi

if ! capture_screenshot "${CAPTURE_ID}" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}"; then
  if ! capture_screenshot "object:window" "${SCREENSHOT_JSON}" "${SCREENSHOT_PATH}"; then
    printf 'Could not capture the channel screen.\n' >&2
    printf 'log=%s\n' "${LOG_FILE}" >&2
    exit 68
  fi
fi

CHANNEL_MARKER="$(grep -F 'CHANNEL DIFF:' "${LOG_FILE}" | tail -n 1 || true)"

printf 'channel_sync=1\n'
printf 'state_dir=%s\n' "${VALIDATION_STATE_DIR}"
printf 'index=%s\n' "${CHANNEL_INDEX}"
printf 'label=%s\n' "${CHANNEL_LABEL}"
printf 'marker=%s\n' "${CHANNEL_MARKER}"
printf 'screenshot=%s\n' "${SCREENSHOT_PATH}"
printf 'log=%s\n' "${LOG_FILE}"
