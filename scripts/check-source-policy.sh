#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if ! command -v rg >/dev/null 2>&1; then
  printf 'ripgrep is required for source policy checks.\n' >&2
  exit 127
fi

failures=0

require_absent() {
  local label="$1"
  local expression="$2"
  shift 2
  local matches=''

  matches="$(rg -l --glob '!references/**' --glob '!build/**' \
    --glob '!scripts/check-source-policy.sh' \
    "${expression}" "$@" 2>/dev/null || true)"
  if [[ -n "${matches}" ]]; then
    printf 'Policy violation: %s\n%s\n' "${label}" "${matches}" >&2
    failures=$((failures + 1))
  fi
}

require_absent \
  'documentation or automation contains a literal international phone number' \
  '\+[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]+' \
  AGENTS.md README.md docs scripts .agents .github
require_absent \
  'active configuration contains a developer-specific home path' \
  '/home/[^/[:space:]]+/' \
  AGENTS.md README.md docs scripts .agents .github TelegramMicro.pro
require_absent \
  'automation repurposes HOME instead of TELEGRAM_MICRO_EMULATOR_STATE_DIR' \
  '(^|[[:space:]])HOME=' \
  scripts .agents .github
require_absent \
  'routine validation depends on xvfb-run' \
  'xvfb-run' \
  AGENTS.md README.md docs scripts .agents .github
require_absent \
  'active instructions reference the retired .codex-skills path' \
  '\.codex-skills' \
  AGENTS.md README.md docs scripts .agents .github
require_absent \
  'MTProto or crypto code imports java.util.Random directly' \
  'import[[:space:]]+java\.util\.Random|new[[:space:]]+Random\(' \
  src/app/MTProto src/app/Crypto
require_absent \
  'protocol code constructs SecureRandomPlus without an explicit seed' \
  'new[[:space:]]+SecureRandomPlus\([[:space:]]*\)' \
  src/app
require_absent \
  'a test uses an unclassified SkippedTest' \
  'extends[[:space:]]+SkippedTest' \
  src/test/tests
require_absent \
  'live validation provides an implicit phone-number default' \
  'TELEGRAM_MICRO_TEST_PHONE:-[^}]' \
  scripts
require_absent \
  'live validation prints credential material' \
  'printf[^\n]*(PHONE_NUMBER|LOGIN_CODE|LOGIN_PASSWORD)' \
  scripts
require_absent \
  'MCP payload data is exposed through process arguments' \
  'data[[:space:]]+"\$\{payload\}"' \
  scripts

while IFS= read -r validation_script; do
  if ! rg -q 'telegrammicro_live_setup' "${validation_script}"; then
    printf 'Policy violation: %s does not use the fail-closed live validation gate.\n' \
      "${validation_script}" >&2
    failures=$((failures + 1))
  fi
done < <(find scripts -maxdepth 1 -type f -name 'validate-*.sh' | sort)

if (( failures > 0 )); then
  exit 1
fi

printf 'Source policy checks passed.\n'
