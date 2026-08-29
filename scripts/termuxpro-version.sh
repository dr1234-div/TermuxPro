#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version_file="$project_dir/termuxpro-version.properties"

read_property() {
    local key="$1"
    awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; found=1 } END { if (!found) exit 1 }' \
        "$version_file"
}

version_name="$(read_property versionName)"
version_code="$(read_property versionCode)"
release_notes="$(read_property releaseNotes)"

if [[ ! "$version_name" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z.-]+)?(\+[0-9A-Za-z.-]+)?$ ]]; then
    echo "termuxpro-version.properties 中的 versionName 不是有效语义版本：$version_name" >&2
    exit 2
fi
if [[ ! "$version_code" =~ ^[1-9][0-9]*$ || "$version_code" -gt 2100000000 ]]; then
    echo "termuxpro-version.properties 中的 versionCode 无效：$version_code" >&2
    exit 3
fi
if [[ "$release_notes" = /* || "$release_notes" == *".."* || ! -f "$project_dir/$release_notes" ]]; then
    echo "版本配置引用的发布说明不存在或路径不安全：$release_notes" >&2
    exit 4
fi

case "${1:-all}" in
    name) printf '%s\n' "$version_name" ;;
    code) printf '%s\n' "$version_code" ;;
    notes) printf '%s\n' "$release_notes" ;;
    dist) printf 'dist/%s\n' "$version_name" ;;
    tag)
        expected="v$version_name"
        if [[ -n "${2:-}" && "$2" != "$expected" ]]; then
            echo "发布标签 $2 与版本源 $expected 不一致。" >&2
            exit 5
        fi
        printf '%s\n' "$expected"
        ;;
    all)
        printf 'versionName=%s\nversionCode=%s\nreleaseNotes=%s\ndistDir=dist/%s\n' \
            "$version_name" "$version_code" "$release_notes" "$version_name"
        ;;
    *)
        echo "用法：$0 {name|code|notes|dist|tag [实际标签]|all}" >&2
        exit 64
        ;;
esac
