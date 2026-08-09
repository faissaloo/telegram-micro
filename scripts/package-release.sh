#!/usr/bin/env bash

set -euo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_ID="${1:-v0.1.0-preview.1}"

if [[ ! "${RELEASE_ID}" =~ ^v[0-9]+\.[0-9]+\.[0-9]+[-.0-9A-Za-z]*$ ]]; then
  printf 'Usage: %s vMAJOR.MINOR.PATCH[-PRERELEASE]\n' "$0" >&2
  exit 64
fi

export TELEGRAM_MICRO_IGNORE_LOCAL_CREDENTIALS=1
unset TELEGRAM_MICRO_API_ID TELEGRAM_MICRO_API_HASH
source "${ROOT_DIR}/scripts/common.sh"

RELEASE_VERSION="${RELEASE_ID#v}"
RELEASE_VERSION="${RELEASE_VERSION%%-*}"
if [[ "${RELEASE_VERSION}" != "${APP_VERSION}" ]]; then
  printf 'Release tag version %s does not match MIDlet version %s.\n' \
    "${RELEASE_VERSION}" "${APP_VERSION}" >&2
  exit 1
fi

RELEASE_DIR="${BUILD_DIR}/release/${RELEASE_ID}"
mkdir -p "${RELEASE_DIR}"

build_app_suite >/dev/null

CONFIG_DUMP="$("${JDK_HOME}/bin/javap" -constants -classpath "${APP_JAR}" \
  config.GeneratedApiConfig)"
if ! grep -Fq 'CONFIGURED = false' <<<"${CONFIG_DUMP}" ||
   ! grep -Fq 'API_ID = 0' <<<"${CONFIG_DUMP}" ||
   ! grep -Fq 'API_HASH = ""' <<<"${CONFIG_DUMP}"; then
  printf 'Release packaging refused a credential-configured JAR.\n' >&2
  exit 1
fi

cp "${APP_JAR}" "${RELEASE_DIR}/TelegramMicro.jar"
cp "${APP_JAD}" "${RELEASE_DIR}/TelegramMicro.jad"
(
  cd "${RELEASE_DIR}"
  sha256sum TelegramMicro.jar TelegramMicro.jad > SHA256SUMS
)

printf 'Credential-free release artifacts prepared in %s\n' "${RELEASE_DIR}"
