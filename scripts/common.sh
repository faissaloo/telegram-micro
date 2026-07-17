#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${ROOT_DIR}/build"
DIST_DIR="${BUILD_DIR}/dist"
BOOTSTRAP_DIR="${TELEGRAM_MICRO_BOOTSTRAP_DIR:-/tmp/telegrammicro-bootstrap}"
DOWNLOAD_DIR="${BOOTSTRAP_DIR}/downloads"
SQUIRRELJME_DIR="${SQUIRRELJME_DIR:-/tmp/SquirrelJME}"
SQUIRRELJME_BASE_COMMIT="${TELEGRAM_MICRO_SQUIRRELJME_BASE_COMMIT:-14b4d02b42f8ee31fa40dcf07b8104f33c2d3b24}"
SQUIRRELJME_PATCH_DIR="${ROOT_DIR}/references/squirreljme"
JDK_HOME="${TELEGRAM_MICRO_JDK_HOME:-${BOOTSTRAP_DIR}/jdk17}"
CMAKE_HOME="${TELEGRAM_MICRO_CMAKE_HOME:-${BOOTSTRAP_DIR}/cmake}"
GRADLE_CACHE_DIR="${TELEGRAM_MICRO_GRADLE_HOME:-${BOOTSTRAP_DIR}/gradle-home}"

DEFAULT_JDK_TARBALL_URL='https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.19%2B10/OpenJDK17U-jdk_x64_linux_hotspot_17.0.19_10.tar.gz'
DEFAULT_JDK_TARBALL_SHA256='d8afc263758141a66e0e3aafc321e783f7016696f4eaea067d340a269037d331'
DEFAULT_CMAKE_TARBALL_URL='https://github.com/Kitware/CMake/releases/download/v3.31.6/cmake-3.31.6-linux-x86_64.tar.gz'
DEFAULT_CMAKE_TARBALL_SHA256='5a1133ff103c71eb5120e2cc3de922733e7d8a26a98ae716397e8676adb367bf'

JDK_TARBALL_URL="${TELEGRAM_MICRO_JDK_TARBALL_URL:-${DEFAULT_JDK_TARBALL_URL}}"
JDK_TARBALL_SHA256="${TELEGRAM_MICRO_JDK_TARBALL_SHA256:-${DEFAULT_JDK_TARBALL_SHA256}}"
CMAKE_TARBALL_URL="${TELEGRAM_MICRO_CMAKE_TARBALL_URL:-${DEFAULT_CMAKE_TARBALL_URL}}"
CMAKE_TARBALL_SHA256="${TELEGRAM_MICRO_CMAKE_TARBALL_SHA256:-${DEFAULT_CMAKE_TARBALL_SHA256}}"

APP_JAR="${DIST_DIR}/TelegramMicro.jar"
APP_JAD="${DIST_DIR}/TelegramMicro.jad"
TEST_JAR="${DIST_DIR}/TelegramMicroTests.jar"
PATCH_FINGERPRINT_FILE="${SQUIRRELJME_DIR}/.telegrammicro-patch-fingerprint"

BUILD_WORK_DIR=''
APP_CLASSES_DIR=''
TEST_CLASSES_DIR=''
GENERATED_SRC_DIR=''
COMPILE_CLASSPATH=''

CREDENTIAL_FILES=(
  "${ROOT_DIR}/.telegrammicro.env"
  "${ROOT_DIR}/.telegrammicro.local.env"
)

load_local_credentials() {
  local env_file

  for env_file in "${CREDENTIAL_FILES[@]}"; do
    [[ -f "${env_file}" ]] || continue

    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
  done
}

load_local_credentials

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

escape_java_string() {
  local value="$1"

  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  printf '%s' "${value}"
}

verify_sha256() {
  local path="$1"
  local expected="$2"

  printf '%s  %s\n' "${expected}" "${path}" | sha256sum --check --status
}

download_and_extract_tarball() {
  local url="$1"
  local expected_sha256="$2"
  local destination="$3"
  local archive_name="$4"
  local archive_path="${DOWNLOAD_DIR}/${archive_name}"
  local partial_path="${archive_path}.part.$$"
  local extract_dir

  require_command curl
  require_command sha256sum
  require_command tar
  mkdir -p "${DOWNLOAD_DIR}"

  if [[ -f "${archive_path}" ]] && ! verify_sha256 "${archive_path}" "${expected_sha256}"; then
    printf 'Cached tool archive failed checksum verification: %s\n' "${archive_path}" >&2
    return 1
  fi

  if [[ ! -f "${archive_path}" ]]; then
    curl --fail --location --show-error --silent "${url}" --output "${partial_path}"
    if ! verify_sha256 "${partial_path}" "${expected_sha256}"; then
      rm -f "${partial_path}"
      printf 'Downloaded tool archive failed checksum verification: %s\n' "${url}" >&2
      return 1
    fi
    mv "${partial_path}" "${archive_path}"
  fi

  if [[ -e "${destination}" ]]; then
    printf 'Tool destination exists but is incomplete: %s\n' "${destination}" >&2
    printf 'Remove that exact directory or provide a valid TELEGRAM_MICRO_*_HOME override.\n' >&2
    return 1
  fi

  extract_dir="$(mktemp -d "${BOOTSTRAP_DIR}/extract.XXXXXX")"
  if ! tar -xzf "${archive_path}" -C "${extract_dir}" --strip-components=1; then
    rm -rf "${extract_dir}"
    return 1
  fi
  mv "${extract_dir}" "${destination}"
}

ensure_jdk() {
  if [[ -x "${JDK_HOME}/bin/javac" && -x "${JDK_HOME}/bin/jar" ]]; then
    return
  fi

  if [[ -n "${TELEGRAM_MICRO_JDK_HOME:-}" ]]; then
    printf 'TELEGRAM_MICRO_JDK_HOME does not contain a usable JDK: %s\n' "${JDK_HOME}" >&2
    exit 1
  fi

  download_and_extract_tarball "${JDK_TARBALL_URL}" "${JDK_TARBALL_SHA256}" \
    "${JDK_HOME}" 'jdk17.tar.gz'
}

ensure_cmake() {
  if [[ -x "${CMAKE_HOME}/bin/cmake" ]]; then
    return
  fi

  if [[ -n "${TELEGRAM_MICRO_CMAKE_HOME:-}" ]]; then
    printf 'TELEGRAM_MICRO_CMAKE_HOME does not contain CMake: %s\n' "${CMAKE_HOME}" >&2
    exit 1
  fi

  download_and_extract_tarball "${CMAKE_TARBALL_URL}" "${CMAKE_TARBALL_SHA256}" \
    "${CMAKE_HOME}" 'cmake.tar.gz'
}

ensure_squirreljme_checkout() {
  if [[ -d "${SQUIRRELJME_DIR}/.git" ]]; then
    return
  fi

  if [[ -e "${SQUIRRELJME_DIR}" ]]; then
    printf 'SquirrelJME destination exists but is not a Git checkout: %s\n' "${SQUIRRELJME_DIR}" >&2
    exit 1
  fi

  require_command git
  git clone --no-checkout https://github.com/SquirrelJME/SquirrelJME "${SQUIRRELJME_DIR}"
  git -C "${SQUIRRELJME_DIR}" checkout --detach "${SQUIRRELJME_BASE_COMMIT}"
}

squirreljme_patch_files() {
  if [[ ! -d "${SQUIRRELJME_PATCH_DIR}" ]]; then
    return
  fi

  find "${SQUIRRELJME_PATCH_DIR}" -maxdepth 1 -type f -name '*.patch' | sort
}

squirreljme_patch_fingerprint() {
  local patch

  {
    printf '%s\n' "${SQUIRRELJME_BASE_COMMIT}"
    while IFS= read -r patch; do
      [[ -n "${patch}" ]] || continue
      sha256sum "${patch}"
    done < <(squirreljme_patch_files)
  } | sha256sum | cut -d ' ' -f 1
}

squirreljme_patches_applied() {
  local patch
  local found=0

  while IFS= read -r patch; do
    [[ -n "${patch}" ]] || continue
    found=1

    if ! git -C "${SQUIRRELJME_DIR}" apply --reverse --check "${patch}" >/dev/null 2>&1; then
      return 1
    fi
  done < <(squirreljme_patch_files)

  [[ "${found}" -eq 1 ]]
}

ensure_squirreljme_base_checkout() {
  require_command git

  if squirreljme_patches_applied; then
    return
  fi

  if [[ -n "$(git -C "${SQUIRRELJME_DIR}" status --porcelain --untracked-files=all)" ]]; then
    printf 'SquirrelJME checkout is dirty and does not exactly match the patch set: %s\n' "${SQUIRRELJME_DIR}" >&2
    exit 1
  fi

  if ! git -C "${SQUIRRELJME_DIR}" cat-file -e "${SQUIRRELJME_BASE_COMMIT}^{commit}" >/dev/null 2>&1; then
    git -C "${SQUIRRELJME_DIR}" fetch origin "${SQUIRRELJME_BASE_COMMIT}"
  fi

  if [[ "$(git -C "${SQUIRRELJME_DIR}" rev-parse HEAD)" != "${SQUIRRELJME_BASE_COMMIT}" ]]; then
    git -C "${SQUIRRELJME_DIR}" checkout --detach "${SQUIRRELJME_BASE_COMMIT}"
  fi
}

verify_squirreljme_patch_sources() {
  local required
  local -a required_sources=(
    'emulators/emulator-base/src/main/java/cc/squirreljme/emulator/scritchui/dylib/DylibMcpServer.java'
    'modules/midp-lcdui/src/main/java/javax/microedition/lcdui/__ExecFormInput__.java'
    'modules/midp-lcdui/src/main/java/javax/microedition/lcdui/__ExecFormPaint__.java'
  )

  for required in "${required_sources[@]}"; do
    if [[ ! -s "${SQUIRRELJME_DIR}/${required}" ]]; then
      printf 'SquirrelJME patch is incomplete; missing required source: %s\n' "${required}" >&2
      return 1
    fi
  done
}

apply_squirreljme_patches() {
  require_command git

  local patch
  local found=0
  while IFS= read -r patch; do
    [[ -n "${patch}" ]] || continue
    found=1

    if git -C "${SQUIRRELJME_DIR}" apply --reverse --check "${patch}" >/dev/null 2>&1; then
      continue
    fi

    if ! git -C "${SQUIRRELJME_DIR}" apply --check "${patch}" >/dev/null 2>&1; then
      printf 'Could not apply %s to SquirrelJME checkout at %s.\n' "${patch}" "${SQUIRRELJME_DIR}" >&2
      printf 'Expected base commit: %s\n' "${SQUIRRELJME_BASE_COMMIT}" >&2
      exit 1
    fi

    git -C "${SQUIRRELJME_DIR}" apply "${patch}"
  done < <(squirreljme_patch_files)

  if [[ "${found}" -ne 1 ]]; then
    printf 'No SquirrelJME patch files were found in %s.\n' "${SQUIRRELJME_PATCH_DIR}" >&2
    exit 1
  fi

  verify_squirreljme_patch_sources
}

export_bootstrap_environment() {
  ensure_jdk
  ensure_cmake
  export JAVA_HOME="${JDK_HOME}"
  export GRADLE_USER_HOME="${GRADLE_CACHE_DIR}"
  export PATH="${JDK_HOME}/bin:${CMAKE_HOME}/bin:${PATH}"
}

find_squirreljme_standalone_jar() {
  local libs_dir="${SQUIRRELJME_DIR}/emulators/standalone/build/libs"

  [[ -d "${libs_dir}" ]] || return 0
  find "${libs_dir}" -maxdepth 1 -type f -name 'squirreljme-standalone*.jar' \
    | sort | tail -n 1
}

verify_squirreljme_standalone_jar() {
  local standalone_jar="$1"
  local listing
  local required
  local -a required_classes=(
    'cc/squirreljme/emulator/scritchui/dylib/DylibMcpServer.class'
    'javax/microedition/lcdui/__ExecFormInput__.class'
    'javax/microedition/lcdui/__ExecFormPaint__.class'
  )

  [[ -s "${standalone_jar}" ]] || return 1
  listing="$("${JDK_HOME}/bin/jar" tf "${standalone_jar}")"
  for required in "${required_classes[@]}"; do
    if ! grep -Fqx "${required}" <<<"${listing}"; then
      printf 'Patched SquirrelJME JAR is missing required class: %s\n' "${required}" >&2
      return 1
    fi
  done
}

standalone_needs_rebuild() {
  local standalone_jar="$1"
  local expected_fingerprint

  if [[ -z "${standalone_jar}" || ! -f "${standalone_jar}" ]]; then
    return 0
  fi

  expected_fingerprint="$(squirreljme_patch_fingerprint)"
  if [[ ! -f "${PATCH_FINGERPRINT_FILE}" ]] || \
     [[ "$(<"${PATCH_FINGERPRINT_FILE}")" != "${expected_fingerprint}" ]]; then
    return 0
  fi

  if ! verify_squirreljme_standalone_jar "${standalone_jar}"; then
    return 0
  fi

  return 1
}

ensure_squirreljme_standalone() {
  ensure_squirreljme_checkout
  ensure_squirreljme_base_checkout
  apply_squirreljme_patches
  export_bootstrap_environment

  local standalone_jar
  local expected_fingerprint
  standalone_jar="$(find_squirreljme_standalone_jar)"
  if [[ -n "${standalone_jar}" ]] && ! standalone_needs_rebuild "${standalone_jar}"; then
    return
  fi

  (
    cd "${SQUIRRELJME_DIR}"
    ./gradlew --console plain :emulators:standalone:shadowJar
  )

  standalone_jar="$(find_squirreljme_standalone_jar)"
  if [[ -z "${standalone_jar}" ]] || ! verify_squirreljme_standalone_jar "${standalone_jar}"; then
    printf 'SquirrelJME build completed without a valid patched standalone JAR.\n' >&2
    exit 1
  fi

  expected_fingerprint="$(squirreljme_patch_fingerprint)"
  printf '%s\n' "${expected_fingerprint}" > "${PATCH_FINGERPRINT_FILE}"
}

cleanup_build_workspace() {
  if [[ -n "${BUILD_WORK_DIR}" ]]; then
    case "${BUILD_WORK_DIR}" in
      "${BUILD_DIR}"/work.*) rm -rf "${BUILD_WORK_DIR}" ;;
    esac
  fi
}

prepare_build_workspace() {
  mkdir -p "${BUILD_DIR}" "${DIST_DIR}"
  BUILD_WORK_DIR="$(mktemp -d "${BUILD_DIR}/work.XXXXXX")"
  APP_CLASSES_DIR="${BUILD_WORK_DIR}/app-classes"
  TEST_CLASSES_DIR="${BUILD_WORK_DIR}/test-classes"
  GENERATED_SRC_DIR="${BUILD_WORK_DIR}/generated-src"
  mkdir -p "${APP_CLASSES_DIR}" "${TEST_CLASSES_DIR}" "${GENERATED_SRC_DIR}"
  trap cleanup_build_workspace EXIT
}

write_generated_api_config() {
  local configured='false'
  local api_id='0'
  local api_hash=''
  local api_hash_escaped

  if [[ -n "${TELEGRAM_MICRO_API_ID:-}" || -n "${TELEGRAM_MICRO_API_HASH:-}" ]]; then
    if [[ -z "${TELEGRAM_MICRO_API_ID:-}" || -z "${TELEGRAM_MICRO_API_HASH:-}" ]]; then
      printf 'TELEGRAM_MICRO_API_ID and TELEGRAM_MICRO_API_HASH must both be set together.\n' >&2
      exit 1
    fi
    if [[ ! "${TELEGRAM_MICRO_API_ID}" =~ ^[0-9]+$ ]]; then
      printf 'TELEGRAM_MICRO_API_ID must be numeric.\n' >&2
      exit 1
    fi

    configured='true'
    api_id="${TELEGRAM_MICRO_API_ID}"
    api_hash="${TELEGRAM_MICRO_API_HASH}"
  fi

  api_hash_escaped="$(escape_java_string "${api_hash}")"
  mkdir -p "${GENERATED_SRC_DIR}/config"
  cat > "${GENERATED_SRC_DIR}/config/GeneratedApiConfig.java" <<EOF
package config;

public final class GeneratedApiConfig {
  public static final boolean CONFIGURED = ${configured};
  public static final int API_ID = ${api_id};
  public static final String API_HASH = "${api_hash_escaped}";

  private GeneratedApiConfig() {
  }
}
EOF
}

build_compile_classpath() {
  local standalone_jar
  local module_jar

  standalone_jar="$(find_squirreljme_standalone_jar)"
  if [[ -z "${standalone_jar}" ]]; then
    printf 'Could not locate the SquirrelJME standalone JAR.\n' >&2
    exit 1
  fi

  COMPILE_CLASSPATH="${standalone_jar}"
  while IFS= read -r module_jar; do
    [[ -n "${module_jar}" ]] || continue
    COMPILE_CLASSPATH="${COMPILE_CLASSPATH}:${module_jar}"
  done < <(find "${SQUIRRELJME_DIR}/modules" -path '*/build/libs/*.jar' -type f | sort)
}

compile_app_classes() {
  local sources_file="${BUILD_WORK_DIR}/app-sources.list"

  find "${ROOT_DIR}/src/app" "${GENERATED_SRC_DIR}" -type f -name '*.java' \
    | sort > "${sources_file}"
  "${JDK_HOME}/bin/javac" -encoding UTF-8 --release 8 -proc:none \
    -classpath "${COMPILE_CLASSPATH}" -d "${APP_CLASSES_DIR}" @"${sources_file}"
}

compile_test_classes() {
  local sources_file="${BUILD_WORK_DIR}/test-sources.list"

  find "${ROOT_DIR}/src/test" -type f -name '*.java' | sort > "${sources_file}"
  "${JDK_HOME}/bin/javac" -encoding UTF-8 --release 8 -proc:none \
    -classpath "${COMPILE_CLASSPATH}:${APP_CLASSES_DIR}" \
    -d "${TEST_CLASSES_DIR}" @"${sources_file}"
}

write_app_manifest() {
  local target="$1"
  cat > "${target}" <<'EOF'
MIDlet-1: TelegramMicro, , TelegramMicro
MIDlet-Info-URL: https://github.com/faissaloo/telegram-lite
MIDlet-Name: TelegramMicro
MIDlet-Vendor: Faissaloo
MIDlet-Version: 1.0
Nokia-MIDlet-Original-Display-Size: 240,320
X-SquirrelJME-Resolution: 240x320
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.1
EOF
}

write_test_manifest() {
  local target="$1"
  local test_mode="${TELEGRAM_MICRO_TEST_MODE:-quick}"

  if [[ "${test_mode}" != 'quick' && "${test_mode}" != 'full' ]]; then
    printf 'TELEGRAM_MICRO_TEST_MODE must be quick or full.\n' >&2
    exit 1
  fi

  cat > "${target}" <<EOF
MIDlet-1: TestRunner, , test.TestRunner
MIDlet-Name: TelegramMicroTests
MIDlet-Vendor: Faissaloo
MIDlet-Version: 1.0
TelegramMicro-Test-Mode: ${test_mode}
Nokia-MIDlet-Original-Display-Size: 240,320
X-SquirrelJME-Resolution: 240x320
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.1
EOF
}

write_app_jad() {
  local target="$1"
  local jar_size="$2"
  cat > "${target}" <<EOF
MIDlet-1: TelegramMicro, , TelegramMicro
MIDlet-Info-URL: https://github.com/faissaloo/telegram-lite
MIDlet-Jar-Size: ${jar_size}
MIDlet-Jar-URL: TelegramMicro.jar
MIDlet-Name: TelegramMicro
MIDlet-Vendor: Faissaloo
MIDlet-Version: 1.0
Nokia-MIDlet-Original-Display-Size: 240,320
X-SquirrelJME-Resolution: 240x320
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.1
EOF
}

verify_suite_jar() {
  local jar_path="$1"
  local suite_kind="$2"
  local listing

  [[ -s "${jar_path}" ]] || return 1
  listing="$("${JDK_HOME}/bin/jar" tf "${jar_path}")"
  if ! grep -Fqx 'TelegramMicro.class' <<<"${listing}"; then
    printf 'Suite JAR is missing TelegramMicro.class: %s\n' "${jar_path}" >&2
    return 1
  fi

  if [[ "${suite_kind}" == 'app' ]]; then
    if grep -q '^test/' <<<"${listing}"; then
      printf 'Production JAR unexpectedly contains test classes: %s\n' "${jar_path}" >&2
      return 1
    fi
  elif ! grep -Fqx 'test/TestRunner.class' <<<"${listing}"; then
    printf 'Test JAR is missing test/TestRunner.class: %s\n' "${jar_path}" >&2
    return 1
  fi
}

publish_artifact() {
  local source_path="$1"
  local destination_path="$2"
  local staged_path="${destination_path}.tmp.$$"

  mv "${source_path}" "${staged_path}"
  mv -f "${staged_path}" "${destination_path}"
}

prepare_suite_build() {
  ensure_squirreljme_standalone
  prepare_build_workspace
  write_generated_api_config
  build_compile_classpath
  compile_app_classes
}

build_app_suite() {
  local manifest
  local staged_jar
  local staged_jad

  prepare_suite_build
  manifest="${BUILD_WORK_DIR}/APP.MF"
  staged_jar="${BUILD_WORK_DIR}/TelegramMicro.jar"
  staged_jad="${BUILD_WORK_DIR}/TelegramMicro.jad"
  write_app_manifest "${manifest}"
  "${JDK_HOME}/bin/jar" cfm "${staged_jar}" "${manifest}" -C "${APP_CLASSES_DIR}" .
  verify_suite_jar "${staged_jar}" app
  write_app_jad "${staged_jad}" "$(wc -c < "${staged_jar}" | tr -d ' ')"
  publish_artifact "${staged_jar}" "${APP_JAR}"
  publish_artifact "${staged_jad}" "${APP_JAD}"
  printf '%s\n' "${APP_JAR}"
}

build_test_suite() {
  local manifest
  local staged_jar

  prepare_suite_build
  manifest="${BUILD_WORK_DIR}/TEST.MF"
  staged_jar="${BUILD_WORK_DIR}/TelegramMicroTests.jar"
  compile_test_classes
  write_test_manifest "${manifest}"
  "${JDK_HOME}/bin/jar" cfm "${staged_jar}" "${manifest}" \
    -C "${APP_CLASSES_DIR}" . -C "${TEST_CLASSES_DIR}" .
  verify_suite_jar "${staged_jar}" test
  publish_artifact "${staged_jar}" "${TEST_JAR}"
  printf '%s\n' "${TEST_JAR}"
}

run_built_suite() {
  local entry="$1"
  local suite_jar="$2"
  shift 2

  local standalone_jar
  local emulator_state_dir
  local -a host_java_args=()
  local -a vm_args=()
  standalone_jar="$(find_squirreljme_standalone_jar)"
  emulator_state_dir="${TELEGRAM_MICRO_EMULATOR_STATE_DIR:-${BUILD_DIR}/emulator-state/${entry}}"
  mkdir -p "${emulator_state_dir}"

  if [[ -n "${SQUIRRELJME_HOST_JAVA_ARGS:-}" ]]; then
    read -r -a host_java_args <<< "${SQUIRRELJME_HOST_JAVA_ARGS}"
  fi
  if [[ -n "${SQUIRRELJME_VM_ARGS:-}" ]]; then
    read -r -a vm_args <<< "${SQUIRRELJME_VM_ARGS}"
  fi
  if [[ -n "${SQUIRRELJME_MCP_PORT:-}" ]]; then
    if [[ ! "${SQUIRRELJME_MCP_PORT}" =~ ^[0-9]+$ ]] || \
       (( SQUIRRELJME_MCP_PORT < 1 || SQUIRRELJME_MCP_PORT > 65535 )); then
      printf 'SQUIRRELJME_MCP_PORT must be between 1 and 65535.\n' >&2
      exit 1
    fi
    host_java_args+=("-Dcc.squirreljme.mcp.port=${SQUIRRELJME_MCP_PORT}")
  fi

  SQUIRRELJME_STATE_HOME="${emulator_state_dir}" \
    "${JDK_HOME}/bin/java" "${host_java_args[@]}" -jar "${standalone_jar}" \
    "${vm_args[@]}" "-Xentry:${entry}" -jar "${suite_jar}" "$@"
}

run_app_suite() {
  build_app_suite
  run_built_suite TelegramMicro "${APP_JAR}" "$@"
}

run_test_suite() {
  build_test_suite
  run_built_suite TestRunner "${TEST_JAR}" "$@"
}

run_full_test_suite() {
  TELEGRAM_MICRO_TEST_MODE=quick
  export TELEGRAM_MICRO_TEST_MODE

  build_test_suite
  run_built_suite TestRunner "${TEST_JAR}" "$@"
  "${JDK_HOME}/bin/java" -classpath "${TEST_JAR}:${COMPILE_CLASSPATH}" \
    test.SlowCryptoRunner
}
