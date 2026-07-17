#!/usr/bin/env bash

set -euo pipefail

TARGET_PORT=''
STOP_ALL=0

case "$#" in
  0)
    TARGET_PORT="${SQUIRRELJME_MCP_PORT:-}"
    if [[ -z "${TARGET_PORT}" ]]; then
      printf 'Usage: %s MCP_PORT | --all\n' "$0" >&2
      exit 64
    fi
    ;;
  1)
    if [[ "$1" == '--all' ]]; then
      STOP_ALL=1
    else
      TARGET_PORT="$1"
    fi
    ;;
  *)
    printf 'Usage: %s MCP_PORT | --all\n' "$0" >&2
    exit 64
    ;;
esac

if [[ "${STOP_ALL}" == '0' ]] &&
   { [[ ! "${TARGET_PORT}" =~ ^[0-9]+$ ]] ||
     (( TARGET_PORT < 1024 || TARGET_PORT > 65535 )); }; then
  printf 'MCP port must be an integer between 1024 and 65535.\n' >&2
  exit 64
fi

PS_OUTPUT="$(ps -ww -eo pid=,ppid=,args=)"

contains_pid() {
  local wanted="$1"
  shift || true
  local existing=''

  for existing in "$@"; do
    if [[ "${existing}" == "${wanted}" ]]; then
      return 0
    fi
  done
  return 1
}

find_root_pids() {
  if [[ -n "${TARGET_PORT}" ]]; then
    printf '%s\n' "${PS_OUTPUT}" | awk -v port="${TARGET_PORT}" '
      index($0, "cc.squirreljme.mcp.port=" port) > 0 { print $1 }
    '
    return
  fi

  printf '%s\n' "${PS_OUTPUT}" | awk '
    /scripts\/run\.sh/ && !/stop-emulator\.sh/ { print $1; next }
    /validate-(login-input|send-code|sign-in|code-restore|restore-session|channel-sync|reconnect-session)\.sh/ && !/stop-emulator\.sh/ { print $1; next }
    /[j]ava/ && /squirreljme-standalone/ { print $1; next }
    /[j]ava/ && /-Xentry:TelegramMicro/ { print $1; next }
    /[j]ava/ && /cc\.squirreljme\.runtime\.launcher\.ui/ { print $1; next }
    /[j]ava/ && /cc\.squirreljme\.runtime\.nativebinding/ { print $1; next }
  '
}

collect_tree() {
  local root="$1"
  local changed=1
  local -a pending=("${root}")
  local pid=''
  local ppid=''
  local _args=''
  local known=''

  while [[ "${changed}" == "1" ]]; do
    changed=0
    while read -r pid ppid _args; do
      for known in "${pending[@]}"; do
        if [[ "${ppid}" == "${known}" ]] && ! contains_pid "${pid}" "${pending[@]}"; then
          pending+=("${pid}")
          changed=1
          break
        fi
      done
    done <<< "${PS_OUTPUT}"
  done

  printf '%s\n' "${pending[@]}"
}

main() {
  local -a roots=()
  local -a targets=()
  local -a remaining=()
  local root=''
  local pid=''

  while read -r root; do
    [[ -n "${root}" ]] || continue
    if ! contains_pid "${root}" "${roots[@]}"; then
      roots+=("${root}")
    fi
  done < <(find_root_pids)

  if [[ "${#roots[@]}" -eq 0 ]]; then
    exit 0
  fi

  for root in "${roots[@]}"; do
    while read -r pid; do
      [[ -n "${pid}" ]] || continue
      if ! contains_pid "${pid}" "${targets[@]}"; then
        targets+=("${pid}")
      fi
    done < <(collect_tree "${root}")
  done

  if [[ "${#targets[@]}" -eq 0 ]]; then
    exit 0
  fi

  kill "${targets[@]}" >/dev/null 2>&1 || true

  for _ in 1 2 3 4 5; do
    remaining=()
    for pid in "${targets[@]}"; do
      if kill -0 "${pid}" >/dev/null 2>&1; then
        remaining+=("${pid}")
      fi
    done
    if [[ "${#remaining[@]}" -eq 0 ]]; then
      break
    fi
    sleep 1
  done

  if [[ "${#remaining[@]}" -gt 0 ]]; then
    kill -KILL "${remaining[@]}" >/dev/null 2>&1 || true
  fi

  printf 'stopped_pids=%s\n' "${targets[*]}"
}

main "$@"
