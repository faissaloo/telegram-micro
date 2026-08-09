#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${ROOT_DIR}/scripts/common.sh"

ensure_squirreljme_standalone

TREE_SOURCE="${SQUIRRELJME_DIR}/modules/midp-lcdui/src/main/java/cc/squirreljme/runtime/lcdui/scritchui/MenuActionTree.java"
MENU_SOURCE="${SQUIRRELJME_DIR}/nanocoat/lib/scritchui/gtk2/gtk2Menu.c"
WINDOW_SOURCE="${SQUIRRELJME_DIR}/nanocoat/lib/scritchui/gtk2/gtk2Window.c"
CONTAINER_SOURCE="${SQUIRRELJME_DIR}/nanocoat/lib/scritchui/gtk2/gtk2Container.c"

for source_file in "${TREE_SOURCE}" "${MENU_SOURCE}" "${WINDOW_SOURCE}" "${CONTAINER_SOURCE}"; do
  if [[ ! -s "${source_file}" ]]; then
    printf 'Patched semantic-softkey source is missing: %s\n' "${source_file}" >&2
    exit 1
  fi
done

require_source() {
  local source_file="$1"
  local expected="$2"
  local label="$3"

  if ! grep -Fq "${expected}" "${source_file}"; then
    printf 'Hosted softkey regression: %s.\n' "${label}" >&2
    exit 1
  fi
}

require_source "${TREE_SOURCE}" 'command.getCommandType()' \
  'placement no longer uses MIDP command semantics'
require_source "${TREE_SOURCE}" 'command.getPriority()' \
  'placement no longer honors MIDP command priority'
require_source "${TREE_SOURCE}" 'NonStandardKey.VGAME_COMMAND_LEFT' \
  'the selected affirmative action is not marked as the left softkey'
require_source "${TREE_SOURCE}" 'NonStandardKey.VGAME_COMMAND_RIGHT' \
  'the selected negative action is not marked as the right softkey'
require_source "${MENU_SOURCE}" 'gtk_button_new()' \
  'hosted commands are not exposed as clickable buttons'
require_source "${MENU_SOURCE}" 'gtk_menu_popup(GTK_MENU(menuWidget)' \
  'overflow commands are not exposed through Options'
require_source "${WINDOW_SOURCE}" '0, 1, 1, 2,' \
  'the softkey footer is not attached below the LCD'
require_source "${CONTAINER_SOURCE}" '0, 1, 0, 1,' \
  'the LCD content is not attached above the footer'

if grep -Eq 'strcmp|strcasecmp|strstr' "${MENU_SOURCE}"; then
  printf 'Hosted softkey regression: command placement guesses from labels.\n' >&2
  exit 1
fi

printf 'Patched GTK2 commands use semantic two-softkey footer placement.\n'
