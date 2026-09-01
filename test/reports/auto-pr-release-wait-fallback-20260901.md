# 自动 PR 候选 Release 等待兜底修复记录（2026-09-01）

## 现象

`0.9.0-rc.1` 候选 Release 已经成功发布，GitHub Release 页面和附件均可查询，但自动 PR workflow 仍停留在
单个“创建或复用 dev PR”步骤内继续等待。该现象如果持续到 job 超时，会制造“实际发布成功但自动 PR 失败”
的邮件噪声。

## 根因判断

自动 PR 原先只通过 `gh run list --workflow .github/workflows/release.yml` 查询候选 Release run；当 GitHub
Actions run 查询存在延迟或可见性差异时，即使 Release 页面已经创建，等待逻辑也没有第二事实源可以提前
成功退出。

## 修复

- 新增 `wait_for_candidate_release`。
- 候选 Release 等待同时接受两个成功证据：
  1. 匹配 merge commit 的 Release workflow run 完成且结论为 `success`。
  2. GitHub Release 页面非 Draft、标记为 Pre-release，并且包含 APK、`SHA256SUMS`、`APK_SIGNATURE.txt`。
- 保留失败行为：若匹配的 Release run 完成但结论不是 `success`，仍返回失败，不掩盖真实发布问题。

## 回归

- `test/workflow-trigger-policy-test.sh` 已增加静态门禁，要求候选 Release 等待必须核验 Release 页面和附件。
