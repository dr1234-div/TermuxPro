#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

bundled_jdk="$project_dir/.tooling/jdk-deb/full/usr/lib/jvm/java-17-openjdk-amd64"
if [[ -x "$bundled_jdk/bin/javac" ]]; then
    export JAVA_HOME="$bundled_jdk"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v javac >/dev/null || ! javac -version 2>&1 | grep -q '^javac 17\.'; then
    echo "需要包含 javac 的完整 JDK 17，请阅读 docs/DEVELOPMENT.md。" >&2
    exit 2
fi

./test/version-metadata-test.sh

gradle_args=(--no-daemon --max-workers=2)
if [[ "${TERMUXPRO_OFFLINE:-0}" == "1" ]]; then
    gradle_args+=(--offline)
fi
./gradlew "${gradle_args[@]}" test
