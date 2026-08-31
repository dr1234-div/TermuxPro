#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_ROOT="$(mktemp -d)"
trap 'rm -rf -- "${TEST_ROOT}"' EXIT

THREAD_ID="11111111-1111-1111-1111-111111111111"
SESSION_FILE="${TEST_ROOT}/rollout-${THREAD_ID}.jsonl"

write_sample() {
  local used_percent="$1"
  printf '{"timestamp":"2026-08-30T12:00:00.000Z","type":"token_count","payload":{"rate_limits":{"primary":{"used_percent":%s,"resets_at":1788656449,"window_minutes":10080}}}}\n' \
    "${used_percent}" > "${SESSION_FILE}"
}

write_sample 85
CODEX_SESSIONS_ROOT="${TEST_ROOT}" CODEX_THREAD_ID="${THREAD_ID}" \
  "${PROJECT_ROOT}/scripts/codex-quota-guard.sh" >/dev/null

write_sample 86
set +e
CODEX_SESSIONS_ROOT="${TEST_ROOT}" CODEX_THREAD_ID="${THREAD_ID}" \
  "${PROJECT_ROOT}/scripts/codex-quota-guard.sh" >/dev/null
status=$?
set -e
if [[ "${status}" -ne 10 ]]; then
  echo "总剩余额度低于 15% 时应返回 10，实际为 ${status}" >&2
  exit 1
fi

echo "Codex 总额度门禁测试通过。"
