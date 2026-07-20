#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_dir="$(cd "${script_dir}/.." && pwd)"
env_file="${TQL_STORE_ENV_FILE:-${backend_dir}/.env.local}"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing environment file: ${env_file}" >&2
  echo "Copy ${backend_dir}/.env.example to ${backend_dir}/.env.local first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${env_file}"
set +a

if [[ $# -eq 0 ]]; then
  echo "Usage: scripts/with-env.sh <command> [args...]" >&2
  exit 1
fi

exec "$@"
