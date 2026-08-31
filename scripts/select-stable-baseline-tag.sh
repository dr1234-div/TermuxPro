#!/usr/bin/env bash
set -euo pipefail

candidate_version="${1:?用法：select-stable-baseline-tag.sh <candidate-version>}"
candidate_base="${candidate_version%%-*}"

if [[ ! "$candidate_base" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "候选版本不是语义版本：$candidate_version" >&2
    exit 2
fi

version_key() {
    local version="${1#v}"
    IFS=. read -r major minor patch <<<"$version"
    printf '%06d.%06d.%06d\n' "$((10#$major))" "$((10#$minor))" "$((10#$patch))"
}

candidate_key="$(version_key "$candidate_base")"
best_tag=""
best_key=""

while IFS= read -r tag; do
    [[ "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || continue
    tag_version="${tag#v}"
    tag_key="$(version_key "$tag_version")"
    if [[ "$tag_key" < "$candidate_key" && ( -z "$best_key" || "$tag_key" > "$best_key" ) ]]; then
        best_tag="$tag"
        best_key="$tag_key"
    fi
done

printf '%s\n' "$best_tag"
