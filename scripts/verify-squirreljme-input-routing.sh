#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/common.sh"

ensure_squirreljme_standalone

SOURCE_FILE="${SQUIRRELJME_DIR}/nanocoat/lib/scritchui/gtk2/gtk2Component.c"
if [[ ! -s "${SOURCE_FILE}" ]]; then
  printf 'Patched GTK2 input source is missing: %s\n' "${SOURCE_FILE}" >&2
  exit 1
fi

FUNCTION_BODY="$(awk '
  /static sjme_errorCode sjme_scritchui_gtk2_eventInputKey\(/ { capture = 1 }
  capture { print }
  capture && /static sjme_errorCode sjme_scritchui_gtk2_eventInputMotion\(/ { exit }
' "${SOURCE_FILE}")"

require_route() {
  local expected="$1"
  local label="$2"

  if ! grep -Fq "${expected}" <<<"${FUNCTION_BODY}"; then
    printf 'GTK2 input routing regression: %s.\n' "${label}" >&2
    exit 1
  fi
}

require_route \
  'event->type == GDK_KEY_PRESS && unicode != 0' \
  'printable character emission is not guarded'
require_route \
  'fill->type = SJME_SCRITCHINPUT_TYPE_KEY_CHAR_PRESSED;' \
  'the character event does not use the first event slot'
require_route \
  'fill->data.key.code = (sjme_jint)unicode;' \
  'the character event does not carry the mapped Unicode code point'
require_route \
  'fillTwo->type = SJME_SCRITCHINPUT_TYPE_KEY_PRESSED;' \
  'the physical key event does not use the second event slot'

if grep -Fq \
    'fillTwo->type = SJME_SCRITCHINPUT_TYPE_KEY_CHAR_PRESSED;' \
    <<<"${FUNCTION_BODY}"; then
  printf 'GTK2 input routing regression: character event is overwritten by the physical key event.\n' >&2
  exit 1
fi

printf 'Patched GTK2 printable-character and physical-key event routing is distinct.\n'
