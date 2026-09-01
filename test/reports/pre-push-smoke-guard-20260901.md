# 推送前冒烟门禁补强记录（2026-09-01）

## 背景

用户反馈 GitHub 邮件里经常看到 PR、CLI 或 CI 失败，感知不到稳定闭环。复核发现项目确实有成功发布和
成功合并记录，但研发中间提交一旦失败，GitHub 已发送的失败邮件不会撤回。

## 根因

`dev_aiLaunchDecisionCopy_20260901` 首次推送前只执行了静态检查和窄编译，未运行受影响的
`WorkspaceActivitySmokeTest`。远端 CI 才发现 AI 启动弹窗文案变为多行后，Robolectric 断言仍是旧文案。

这说明流程缺口是：UI、资源、中文文案、终端反馈、Git 或 tmux 入口变更缺少“按变更类型自动补跑相关
Robolectric 冒烟测试”的本地门禁。

## 修复

新增 `scripts/pre-push-smoke.sh`：

- 同时收集相对 `origin/dev` 的已提交变更、已暂存变更、未暂存变更和未跟踪新文件。
- 先执行 `git diff --check`、Skill、版本、workflow、GitHub CLI 和 check suite 静态门禁。
- 根据变更文件映射到相关 Robolectric 测试：
  - 工作台、资源、AI 弹窗：`WorkspaceActivitySmokeTest`、`CustomLayoutsSmokeTest`
  - Manifest/桌面身份：`ManifestProductIdentityTest`
  - Git 工作台：`GitDiffActivityTest`、`GitRepositoryOverviewTest`、`WorkspaceCommandBuilderTest`
  - tmux 会话中心：`TaskSessionsActivityTest`、`TmuxSessionParserTest`、`TmuxSessionNameValidatorTest`
  - 终端反馈/触摸：`TerminalProjectToolsMenuTest`、`TerminalTouchScrollPolicyTest`、
    `TermuxTerminalSessionActivityClientTest`
  - AI/远端命令：`AiCliLaunchCommandTest`、`AiTerminalActionTest`、`RemoteToolRecoveryTest`、
    `WorkspaceCommandBuilderTest`
- 真实 Gradle 前执行 `scripts/resource-guard.sh normal` 并 source `scripts/resolve-jdk17.sh`。
- 支持 `TERMUXPRO_DRY_RUN=1`，用于校验映射而不启动 Gradle。

## 验收

- `test/pre-push-smoke-test.sh` 固化脚本必须覆盖的映射和资源守卫要求。
- 本轮自测发现初版脚本漏收未跟踪文件，已补充 `git ls-files --others --exclude-standard` 并加入静态断言。
- `scripts/test-all.sh` 已纳入 `test/pre-push-smoke-test.sh`。
- `AGENTS.md` 与 `.agents/skills/termuxpro-development/SKILL.md` 已同步规则。

## 预期效果

后续推送前能更早发现常见 UI/文案/入口断言不匹配，减少“远端 CI 首次发现 → GitHub 失败邮件”的噪声。
它不替代 GitHub CI；权限失败、真实编译失败、模拟器失败和 Release 失败仍应保留为失败并处理。
