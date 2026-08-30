#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

source "$project_dir/scripts/resolve-jdk17.sh"

./test/version-metadata-test.sh
./scripts/validate-skills.sh

gradle_args=(--no-daemon --max-workers=2)
if [[ "${TERMUXPRO_OFFLINE:-0}" == "1" ]]; then
    gradle_args+=(--offline)
fi
./gradlew "${gradle_args[@]}" test
