#!/usr/bin/env bash

set -euo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/common.sh"

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/telegrammicro-profiles.XXXXXX")"

cleanup() {
  case "${WORK_DIR}" in
    "${TMPDIR:-/tmp}"/telegrammicro-profiles.*) rm -rf "${WORK_DIR}" ;;
  esac
}
trap cleanup EXIT

build_test_suite >/dev/null

run_profile() {
  local requested="$1"
  local expected="$2"
  local profile_arg="$3"
  local log_file="${WORK_DIR}/${requested}.log"
  local state_dir="${WORK_DIR}/${requested}-state"
  local vm_args='-Xemulator:hosted'
  local run_status=0

  mkdir -p "${state_dir}"
  if [[ -n "${profile_arg}" ]]; then
    vm_args="${vm_args} -Xprofile:${profile_arg}"
  fi
  vm_args="${vm_args} -Dtelegrammicro.expected.runtime.profile=${expected}"

  TELEGRAM_MICRO_EMULATOR_STATE_DIR="${state_dir}" \
  SQUIRRELJME_VM_ARGS="${vm_args}" \
    run_built_suite RuntimeProfileProbe "${TEST_JAR}" \
    >"${log_file}" 2>&1 || run_status=$?

  if [[ "${run_status}" -ne 0 ]] || \
     ! grep -Fq "RUNTIME PROFILE PROBE PASSED: ${expected}" "${log_file}"; then
    printf 'Runtime profile probe failed for %s with status %s.\n' \
      "${requested}" "${run_status}" >&2
    sed -n '1,120p' "${log_file}" >&2
    return 1
  fi
}

run_profile default ideal ''
run_profile ideal ideal ideal
run_profile nokia-kvm nokia-kvm nokia-kvm
run_profile nokia-jrt nokia-jrt nokia-jrt
run_profile myriad-jbed myriad-jbed myriad-jbed
run_profile aplix-jblend aplix-jblend aplix-jblend
run_profile ibm-j9 ibm-j9 ibm-j9
run_profile insignia-jeode insignia-jeode insignia-jeode
run_profile sony-ericsson-jp sony-ericsson-jp sony-ericsson-jp
run_profile jecode-alias insignia-jeode jecode

UNKNOWN_LOG="${WORK_DIR}/unknown.log"
UNKNOWN_STATE="${WORK_DIR}/unknown-state"
mkdir -p "${UNKNOWN_STATE}"
unknown_status=0
TELEGRAM_MICRO_EMULATOR_STATE_DIR="${UNKNOWN_STATE}" \
SQUIRRELJME_VM_ARGS='-Xemulator:hosted -Xprofile:not-a-runtime' \
  run_built_suite RuntimeProfileProbe "${TEST_JAR}" \
  >"${UNKNOWN_LOG}" 2>&1 || unknown_status=$?
if [[ "${unknown_status}" -eq 0 ]]; then
  printf 'Unknown runtime profile was accepted.\n' >&2
  exit 1
fi
if ! grep -Fq 'Unknown runtime profile: not-a-runtime' "${UNKNOWN_LOG}"; then
  printf 'Unknown runtime profile did not fail with a bounded diagnostic.\n' >&2
  sed -n '1,120p' "${UNKNOWN_LOG}" >&2
  exit 1
fi

printf 'Runtime profile validation and target-VM propagation probes passed.\n'
