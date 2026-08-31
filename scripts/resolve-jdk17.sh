#!/usr/bin/env bash
# 供仓库脚本 source：只选择同时包含 java/javac 的完整 JDK 17，不修改系统环境。

termuxpro_project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
termuxpro_candidates=(
    "$termuxpro_project_dir/.tooling/jdk-deb/full/usr/lib/jvm/java-17-openjdk-amd64"
    "$termuxpro_project_dir/.tooling/jdk17"
)
if [[ -n "${JAVA_HOME:-}" ]]; then
    termuxpro_candidates+=("$JAVA_HOME")
fi
if command -v javac >/dev/null 2>&1; then
    termuxpro_candidates+=("$(cd "$(dirname "$(command -v javac)")/.." && pwd)")
fi

termuxpro_selected_jdk=""
for termuxpro_candidate in "${termuxpro_candidates[@]}"; do
    if [[ -x "$termuxpro_candidate/bin/java" && -x "$termuxpro_candidate/bin/javac" ]] &&
       "$termuxpro_candidate/bin/java" -version 2>&1 | head -n 1 | grep -q '"17\.' &&
       "$termuxpro_candidate/bin/javac" -version 2>&1 | grep -q '^javac 17\.'; then
        termuxpro_selected_jdk="$termuxpro_candidate"
        break
    fi
done

if [[ -z "$termuxpro_selected_jdk" ]]; then
    echo "需要同时包含 java 和 javac 的完整 JDK 17，请阅读 docs/DEVELOPMENT.md。" >&2
    return 2 2>/dev/null || exit 2
fi

export JAVA_HOME="$termuxpro_selected_jdk"
export PATH="$JAVA_HOME/bin:$PATH"
unset termuxpro_project_dir termuxpro_candidates termuxpro_candidate termuxpro_selected_jdk
