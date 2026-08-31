#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

source "$project_dir/scripts/resolve-jdk17.sh"

./test/version-metadata-test.sh
./test/select-stable-baseline-tag-test.sh
./test/github-cli-wrapper-test.sh
./test/workflow-trigger-policy-test.sh
./test/skill-validator-bootstrap-test.sh
./test/ui-screenshot-manifest-test.sh
./test/release-apk-device-smoke-test.sh
./scripts/validate-skills.sh

gradle_args=(--no-daemon --max-workers=2)
if [[ "${TERMUXPRO_OFFLINE:-0}" == "1" ]]; then
    gradle_args+=(--offline)
fi
./gradlew "${gradle_args[@]}" test
