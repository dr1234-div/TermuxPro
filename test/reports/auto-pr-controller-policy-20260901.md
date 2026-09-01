# 自动 PR 单一控制器规则补录（2026-09-01）

## 背景

`dev_090StableEvidence_20260901` 推送后，`auto-dev-pr.yml` 已自动复用 PR #148，并在同一 job 中等待：

- 预合并完整 CI：`success`
- PR 合并
- `dev` 合并后 `workflow_dispatch` 收尾 CI：`success`

维护会话同时手工创建/合并了同一 PR，并在自动 job 即将自然完成前取消了 run `33508486858`，
导致该 run 最终显示为 `cancelled`。这是流程接管噪声，不是项目代码、GitHub 权限或 Release 门禁失败。

## 决议

- `dev_*` / `hotfix_*` 分支推送后，默认由 `auto-dev-pr.yml` 作为单一控制器负责创建 PR、等待检查、
  自动合并和 dev 收尾 CI。
- 自动工作流仍在运行时，不再手工抢跑创建、合并或取消同一分支 PR。
- 只有自动工作流未在限定时间内创建 PR、已失败退出或 GitHub 明确不可用时，才允许人工接管。
- 人工接管必须在报告或 PR 说明中记录原因，避免把人工取消、重复控制器和 pending 检查误判成项目失败。

## 回归

- `test/workflow-trigger-policy-test.sh` 已增加规则校验，要求 AGENTS 与 TermuxPro skill 同步写入单一控制器
  和人工接管条件。
