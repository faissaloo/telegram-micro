#!/usr/bin/env bash

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  printf 'This file is a shell library and must be sourced.\n' >&2
  exit 64
fi

LIVE_VALIDATION_LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${LIVE_VALIDATION_LIB_DIR}/mcp.sh"

telegrammicro_live_setup() {
  local validation_name="$1"
  local phone_policy="$2"
  local state_policy="$3"
  local requested_state="${TELEGRAM_MICRO_VALIDATE_STATE_DIR:-}"

  umask 077

  if [[ "${TELEGRAM_MICRO_LIVE:-}" != '1' ]] ||
     [[ "${TELEGRAM_MICRO_LIVE_ACK:-}" != 'telegram-production' ]]; then
    printf '%s\n' \
      'Live Telegram validation is disabled by default.' \
      'Set TELEGRAM_MICRO_LIVE=1 and TELEGRAM_MICRO_LIVE_ACK=telegram-production only after reviewing the requested production action.' >&2
    return 64
  fi
  if [[ -n "${TELEGRAM_MICRO_VALIDATE_HOME:-}" ]]; then
    printf '%s\n' \
      'TELEGRAM_MICRO_VALIDATE_HOME is retired.' \
      'Use TELEGRAM_MICRO_VALIDATE_STATE_DIR; validation must not repurpose HOME.' >&2
    return 64
  fi

  case "${phone_policy}" in
    phone)
      if [[ -z "${TELEGRAM_MICRO_TEST_PHONE:-}" ]] ||
         [[ ! "${TELEGRAM_MICRO_TEST_PHONE}" =~ ^\+[0-9]{5,15}$ ]]; then
        printf 'Set TELEGRAM_MICRO_TEST_PHONE to an explicit E.164-style number.\n' >&2
        return 64
      fi
      # shellcheck disable=SC2034 # Assigned for the sourcing validation script.
      PHONE_NUMBER="${TELEGRAM_MICRO_TEST_PHONE}"
      unset TELEGRAM_MICRO_TEST_PHONE
      ;;
    none) ;;
    *)
      printf 'Unknown phone policy: %s\n' "${phone_policy}" >&2
      return 64
      ;;
  esac

  mcp_require_tools || return
  # shellcheck disable=SC2034 # Assigned for the sourcing validation script.
  MCP_PORT="$(mcp_pick_port)" || return
  VALIDATION_TEMP_ROOT="${TMPDIR:-/tmp}"
  VALIDATION_WORK_DIR="$(mktemp -d "${VALIDATION_TEMP_ROOT%/}/telegrammicro-live-${validation_name}.XXXXXX")"
  export TMPDIR="${VALIDATION_WORK_DIR}"
  VALIDATION_ARTIFACT_DIR="${TELEGRAM_MICRO_VALIDATE_ARTIFACT_DIR:-${ROOT_DIR}/artifacts/live/${validation_name}/$(date -u +%Y%m%dT%H%M%SZ)-$$}"

  case "${state_policy}" in
    transient)
      VALIDATION_STATE_DIR="${requested_state:-${VALIDATION_WORK_DIR}/state}"
      ;;
    persistent)
      VALIDATION_STATE_DIR="${requested_state:-${VALIDATION_ARTIFACT_DIR}/state}"
      ;;
    required)
      if [[ -z "${requested_state}" || ! -d "${requested_state}" ]]; then
        printf 'Set TELEGRAM_MICRO_VALIDATE_STATE_DIR to an existing, previously authorized emulator state directory.\n' >&2
        telegrammicro_live_cleanup
        return 64
      fi
      VALIDATION_STATE_DIR="${requested_state}"
      ;;
    *)
      printf 'Unknown state policy: %s\n' "${state_policy}" >&2
      telegrammicro_live_cleanup
      return 64
      ;;
  esac

  mkdir -p "${VALIDATION_ARTIFACT_DIR}" "${VALIDATION_STATE_DIR}"
}

telegrammicro_live_cleanup() {
  if [[ -z "${VALIDATION_WORK_DIR:-}" || -z "${VALIDATION_TEMP_ROOT:-}" ]]; then
    return
  fi
  case "${VALIDATION_WORK_DIR}" in
    "${VALIDATION_TEMP_ROOT%/}"/telegrammicro-live-*)
      rm -rf -- "${VALIDATION_WORK_DIR}"
      ;;
    *)
      printf 'Refusing to remove unexpected validation work directory: %s\n' \
        "${VALIDATION_WORK_DIR}" >&2
      return 1
      ;;
  esac
}
