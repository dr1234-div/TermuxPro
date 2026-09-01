# 自动 PR workflow_dispatch 等待逻辑修复记录（2026-09-01）

## 问题

PR #134 的功能分支 CI 和模拟器 UI 均成功，PR 已合入 `dev`，合并后的 dev CI run `33491735313`
也成功。但自动 PR workflow run `33491134842` 仍显示 `in_progress`，停留在同一个 shell step。

## 根因

自动 PR 合并后通过 `gh workflow run` 触发 `dev` 上的 `workflow_dispatch` CI，随后用
`gh run list --commit <merge_sha>` 等待结果。GitHub CLI 对 `workflow_dispatch` run 的 `--commit`
过滤不稳定，可能查不到已触发的 dev CI，导致脚本持续等待直到 workflow 超时，最终制造失败邮件和
无效告警。

## 修复

- `wait_for_workflow` 改为读取 `headSha/headBranch`，按目标 `headSha` 精确匹配。
- 预合并分支 CI/UI 继续按当前功能分支过滤。
- 合并后 dev CI 显式按 `merge_sha + dev` 查询。
- 候选 Release 按 `merge_sha` 查询，不强制使用分支过滤。
- 静态测试禁止恢复 `--commit "$target_sha"` 等待方式。

## 验收

- `test/workflow-trigger-policy-test.sh` 必须通过。
- 文档/证据类快速通道和自动 PR 逻辑不得回退。
- 后续自动 PR run 应在 dev 收尾 CI 完成后正常结束，而不是空等到 60 分钟。
