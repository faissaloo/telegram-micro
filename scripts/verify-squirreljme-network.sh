#!/usr/bin/env bash

set -euo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/common.sh"

require_command node

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/telegrammicro-gcf.XXXXXX")"
READY_FILE="${WORK_DIR}/ready"
RESULT_FILE="${WORK_DIR}/result"
SERVER_LOG="${WORK_DIR}/server.log"
CLIENT_LOG="${WORK_DIR}/client.log"
STATE_DIR="${WORK_DIR}/state"
SERVER_PID=''
client_status=0
server_status=0

cleanup() {
  if [[ -n "${SERVER_PID}" ]] && kill -0 "${SERVER_PID}" >/dev/null 2>&1; then
    kill "${SERVER_PID}" >/dev/null 2>&1 || true
    wait "${SERVER_PID}" >/dev/null 2>&1 || true
  fi
  case "${WORK_DIR}" in
    "${TMPDIR:-/tmp}"/telegrammicro-gcf.*) rm -rf "${WORK_DIR}" ;;
  esac
}
trap cleanup EXIT

mkdir -p "${STATE_DIR}"
build_test_suite >/dev/null

node "${ROOT_DIR}/scripts/support/gcf-loopback-server.js" \
  "${READY_FILE}" "${RESULT_FILE}" >"${SERVER_LOG}" 2>&1 &
SERVER_PID=$!

for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  [[ -s "${READY_FILE}" ]] && break
  if ! kill -0 "${SERVER_PID}" >/dev/null 2>&1; then
    printf 'Loopback server stopped before publishing its port.\n' >&2
    sed -n '1,80p' "${SERVER_LOG}" >&2
    exit 1
  fi
  sleep 0.1
done

if [[ ! -s "${READY_FILE}" ]]; then
  printf 'Timed out waiting for the loopback server port.\n' >&2
  exit 1
fi
PORT="$(sed -n '1p' "${READY_FILE}")"
if [[ ! "${PORT}" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
  printf 'Loopback server published an invalid port.\n' >&2
  exit 1
fi

TELEGRAM_MICRO_EMULATOR_STATE_DIR="${STATE_DIR}" \
SQUIRRELJME_VM_ARGS="-Xemulator:hosted -Dtelegrammicro.gcf.port=${PORT}" \
  run_built_suite GcfSocketProbe "${TEST_JAR}" \
  >"${CLIENT_LOG}" 2>&1 || client_status=$?
if [[ "${client_status}" -ne 0 ]]; then
  printf 'The target-VM GCF socket probe exited with status %s.\n' \
    "${client_status}" >&2
  sed -n '1,160p' "${CLIENT_LOG}" >&2
  exit 1
fi

wait "${SERVER_PID}" || server_status=$?
SERVER_PID=''
if [[ "${server_status}" -ne 0 ]]; then
  printf 'The loopback server exited with status %s.\n' \
    "${server_status}" >&2
  sed -n '1,80p' "${SERVER_LOG}" >&2
  exit 1
fi

if ! grep -Fq 'GCF SOCKET PROBE PASSED' "${CLIENT_LOG}"; then
  printf 'The target-VM GCF socket probe did not pass.\n' >&2
  sed -n '1,160p' "${CLIENT_LOG}" >&2
  exit 1
fi
if grep -Eq 'EC0i|EC0j|GCF SOCKET PROBE FAILED' "${CLIENT_LOG}"; then
  printf 'The target-VM GCF socket probe logged an IP provider failure.\n' >&2
  sed -n '1,160p' "${CLIENT_LOG}" >&2
  exit 1
fi
if ! grep -Fq 'GCF LOOPBACK SERVER PASSED' "${RESULT_FILE}"; then
  printf 'The loopback server did not observe the complete bounded exchange.\n' >&2
  sed -n '1,80p' "${SERVER_LOG}" >&2
  exit 1
fi

printf 'Hosted target-VM GCF DNS, TCP, modes, options, and lifecycle probe passed.\n'
