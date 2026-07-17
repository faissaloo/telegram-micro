#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

: "${SQUIRRELJME_VM_ARGS:=-Xemulator:hosted}"
export SQUIRRELJME_VM_ARGS

run_app_suite "$@"
