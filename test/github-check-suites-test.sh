#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fixture_dir="$(mktemp -d)"
trap 'rm -rf "$fixture_dir"' EXIT

healthy_with_external_noise="$fixture_dir/healthy-with-external-noise.json"
cat >"$healthy_with_external_noise" <<'JSON'
{
  "check_suites": [
    {
      "app": {"slug": "vercel"},
      "status": "queued",
      "conclusion": null,
      "latest_check_runs_count": 0,
      "url": "https://api.github.com/example/vercel"
    },
    {
      "app": {"slug": "github-actions"},
      "status": "completed",
      "conclusion": "success",
      "latest_check_runs_count": 1,
      "url": "https://api.github.com/example/actions"
    }
  ]
}
JSON

actions_risk="$fixture_dir/actions-risk.json"
cat >"$actions_risk" <<'JSON'
{
  "check_suites": [
    {
      "app": {"slug": "github-actions"},
      "status": "queued",
      "conclusion": null,
      "latest_check_runs_count": 0,
      "url": "https://api.github.com/example/actions"
    }
  ]
}
JSON

"$project_dir/scripts/check-github-check-suites.sh" --json-file "$healthy_with_external_noise" >/tmp/termuxpro-check-suites-ok.log 2>&1

set +e
"$project_dir/scripts/check-github-check-suites.sh" --json-file "$healthy_with_external_noise" --fail-on-external-empty >/tmp/termuxpro-check-suites-external.log 2>&1
status=$?
set -e
if [[ "$status" -ne 2 ]]; then
    echo "启用 --fail-on-external-empty 时应返回 2，实际为 $status" >&2
    exit 1
fi

set +e
"$project_dir/scripts/check-github-check-suites.sh" --json-file "$actions_risk" >/tmp/termuxpro-check-suites-risk.log 2>&1
status=$?
set -e
if [[ "$status" -ne 1 ]]; then
    echo "GitHub Actions 风险应返回 1，实际为 $status" >&2
    exit 1
fi

echo "GitHub check suite 诊断脚本测试通过。"
