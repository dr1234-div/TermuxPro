# CI 文档变更快速通道回归记录

## 问题

纯文档/验收证据分支也会触发完整 CI 和模拟器 UI，导致 GitHub Runner 资源浪费、自动 PR 等待时间变长，
并增加无意义失败/邮件噪声。该问题不提升 TermuxPro 用户价值，且与远程共享环境资源红线冲突。

## 决策

- CI 保留秘密边界与静态门禁。
- 仅文档、规则和验收证据变更跳过 Android 全量测试、Lint 和 APK 构建。
- UI 模拟器只在 UI/Android 运行时相关路径变化时触发。
- 自动 PR 根据 PR 文件列表判断是否等待模拟器 UI；文档/证据类合并后的 `dev` 收尾 CI 显式关闭
  Android 运行时门禁。

## 验收

- 代码、资源、UI、终端、Git、tmux、脚本和 workflow 变更仍进入完整门禁。
- 文档/规则/验收证据变更仍有静态校验和自动 PR 闭环。
- 不再因为不存在的 UI run 卡住自动 PR。

## 验证

- `./test/workflow-trigger-policy-test.sh` 通过。
- `.tooling/skill-validator-venv/bin/python` 解析 `.github/workflows/ci.yml`、`.github/workflows/ui-emulator.yml`
  和 `.github/workflows/auto-dev-pr.yml` 通过；未依赖系统全局 PyYAML。
- 抽取 16 段 GitHub Actions 内嵌 bash 并执行 `bash -n` 通过。
- `git diff --check` 通过。
- `./scripts/validate-skills.sh` 通过。
- `./scripts/pre-push-smoke.sh origin/dev` 通过；本轮仅涉及 workflow、规则和文档，静态门禁完成。
- PR #130 已合入 `dev`，merge commit `559665d516bab7734defac021016818867cfbf68`。
- 分支 CI run `33487758405` 通过；模拟器 UI run `33487758421` 通过；`dev` 收尾 CI run
  `33488274687` 通过；自动 PR run `33487758418` 成功结束。
- docs-only 快速通道自验证 PR #131 已合入 `dev`，merge commit
  `9b7a012f4ffc381a5ffa677bf3cfcac4bb357f20`；分支 CI run `33488818609` 仅用 17 秒完成静态门禁，
  未触发模拟器 UI；自动 PR run `33488818652` 未等待不存在的 UI run 并在 52 秒内成功；`dev` 收尾
  CI run `33488865509` 成功。
