#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_squirreljme_standalone
find_squirreljme_standalone_jar
