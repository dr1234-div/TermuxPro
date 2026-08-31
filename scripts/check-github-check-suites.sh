#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
repo="${GH_REPO:-heydarey/TermuxPro}"
target_branch="${TERMUXPRO_CHECK_BRANCH:-dev}"
target_sha="${TERMUXPRO_CHECK_SHA:-}"
json_file=""
fail_on_external_empty=0

usage() {
    cat <<'EOF'
用法：scripts/check-github-check-suites.sh [--branch dev|master] [--sha <commit>] [--json-file <file>] [--fail-on-external-empty]

用途：
  诊断 GitHub check suite 状态，区分仓库自身 GitHub Actions 门禁失败和外部 App 注入的空检查噪声。

退出码：
  0  未发现仓库自身 Actions 风险；外部空 check suite 只作为噪声报告。
  1  GitHub Actions check suite 失败、卡住或没有 check runs。
  2  使用 --fail-on-external-empty 时发现外部空 check suite。
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --branch)
            target_branch="${2:?缺少 --branch 参数值}"
            shift 2
            ;;
        --sha)
            target_sha="${2:?缺少 --sha 参数值}"
            shift 2
            ;;
        --json-file)
            json_file="${2:?缺少 --json-file 参数值}"
            shift 2
            ;;
        --fail-on-external-empty)
            fail_on_external_empty=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "未知参数：$1" >&2
            usage >&2
            exit 64
            ;;
    esac
done

if [[ -n "$json_file" ]]; then
    suites_json="$(cat "$json_file")"
else
    if [[ -z "$target_sha" ]]; then
        target_sha="$(git -C "$project_dir" ls-remote origin "refs/heads/$target_branch" | awk '{print $1}')"
    fi
    if [[ -z "$target_sha" ]]; then
        echo "无法解析分支提交：$target_branch" >&2
        exit 64
    fi
    suites_json="$("$project_dir/scripts/github-cli.sh" api "repos/$repo/commits/$target_sha/check-suites")"
fi

summary="$(python3 -c '
import json
import sys

payload = json.load(sys.stdin)
suites = []
for suite in payload.get("check_suites", []):
    app = suite.get("app") or {}
    suites.append({
        "app": app.get("slug") or "unknown",
        "status": suite.get("status") or "unknown",
        "conclusion": suite.get("conclusion") or "",
        "runs": suite.get("latest_check_runs_count") or 0,
        "url": suite.get("html_url") or suite.get("url") or "",
    })

allowed = {"success", "neutral", "skipped"}
github_actions_risk = [
    suite for suite in suites
    if suite["app"] == "github-actions"
    and (suite["runs"] == 0 or suite["status"] != "completed" or suite["conclusion"] not in allowed)
]
external_empty = [
    suite for suite in suites
    if suite["app"] != "github-actions" and suite["runs"] == 0
]
external_failures = [
    suite for suite in suites
    if suite["app"] != "github-actions"
    and suite["status"] == "completed"
    and suite["conclusion"] not in allowed
]
print(json.dumps({
    "githubActionsRisk": github_actions_risk,
    "externalEmptySuites": external_empty,
    "externalCompletedFailures": external_failures,
    "all": suites,
}, ensure_ascii=False, indent=2))
' <<<"$suites_json")"

github_actions_risk_count="$(python3 -c 'import json,sys; print(len(json.load(sys.stdin)["githubActionsRisk"]))' <<<"$summary")"
external_empty_count="$(python3 -c 'import json,sys; print(len(json.load(sys.stdin)["externalEmptySuites"]))' <<<"$summary")"
external_failure_count="$(python3 -c 'import json,sys; print(len(json.load(sys.stdin)["externalCompletedFailures"]))' <<<"$summary")"

echo "$summary"

if [[ "$github_actions_risk_count" -gt 0 ]]; then
    echo "发现 GitHub Actions 门禁风险：$github_actions_risk_count 项。" >&2
    exit 1
fi

if [[ "$external_empty_count" -gt 0 ]]; then
    echo "发现外部 App 空 check suite：$external_empty_count 项。它们通常不是仓库 Actions 失败；若被设为必需检查才会阻断合并。" >&2
fi

if [[ "$external_failure_count" -gt 0 ]]; then
    echo "发现外部 App 完成态失败：$external_failure_count 项。请确认是否为本项目必需集成。" >&2
fi

if [[ "$fail_on_external_empty" -eq 1 && "$external_empty_count" -gt 0 ]]; then
    exit 2
fi
