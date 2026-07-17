#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/common.sh"

MAX_APP_JAR_BYTES="${TELEGRAM_MICRO_MAX_APP_JAR_BYTES:-524288}"
MAX_APP_CLASSES="${TELEGRAM_MICRO_MAX_APP_CLASSES:-256}"

if [[ ! -s "${APP_JAR}" || ! -s "${APP_JAD}" ]]; then
  printf 'Build the production suite before checking device budgets.\n' >&2
  exit 64
fi
if [[ ! "${MAX_APP_JAR_BYTES}" =~ ^[0-9]+$ ]] ||
   [[ ! "${MAX_APP_CLASSES}" =~ ^[0-9]+$ ]]; then
  printf 'Device budget overrides must be positive integers.\n' >&2
  exit 64
fi

ensure_jdk
require_command unzip
listing="$("${JDK_HOME}/bin/jar" tf "${APP_JAR}")"
jar_bytes="$(wc -c < "${APP_JAR}" | tr -d ' ')"
class_count="$(printf '%s\n' "${listing}" | awk '/\.class$/ {count++} END {print count + 0}')"
jad_bytes="$(awk -F ': ' '/^MIDlet-Jar-Size:/ {print $2}' "${APP_JAD}" | tr -d '\r')"

if (( jar_bytes > MAX_APP_JAR_BYTES )); then
  printf 'Production JAR is %s bytes; device budget is %s.\n' \
    "${jar_bytes}" "${MAX_APP_JAR_BYTES}" >&2
  exit 1
fi
if (( class_count > MAX_APP_CLASSES )); then
  printf 'Production JAR has %s classes; device budget is %s.\n' \
    "${class_count}" "${MAX_APP_CLASSES}" >&2
  exit 1
fi
if [[ "${jad_bytes}" != "${jar_bytes}" ]]; then
  printf 'JAD size %s does not match production JAR size %s.\n' \
    "${jad_bytes:-missing}" "${jar_bytes}" >&2
  exit 1
fi
if printf '%s\n' "${listing}" | grep -q '^test/'; then
  printf 'Production JAR contains test classes.\n' >&2
  exit 1
fi
for required_class in \
  TelegramMicro.class \
  config/GeneratedApiConfig.class \
  mtproto/ProtocolLimits.class; do
  if ! printf '%s\n' "${listing}" | grep -Fqx "${required_class}"; then
    printf 'Production JAR is missing %s.\n' "${required_class}" >&2
    exit 1
  fi
done
if ! unzip -p "${APP_JAR}" META-INF/MANIFEST.MF |
    grep -Fq 'MIDlet-Info-URL: https://github.com/faissaloo/telegram-micro'; then
  printf 'Production manifest contains a stale project URL.\n' >&2
  exit 1
fi

printf 'Device artifact budget passed: jar_bytes=%s/%s classes=%s/%s\n' \
  "${jar_bytes}" "${MAX_APP_JAR_BYTES}" "${class_count}" "${MAX_APP_CLASSES}"
