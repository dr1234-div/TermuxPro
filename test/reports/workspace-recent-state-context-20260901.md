# 工作区最近状态上下文验收记录

## 用户问题

手机端主要用于 SSH 到远程服务器继续 Codex CLI / Claude Code 工作。多个服务器或项目并存时，用户需要在
连接前快速判断当前选中的工作区是否就是刚才使用、最近验证或已经过期的上下文；否则容易误连到错误项目，
甚至污染共享 Claude/tmux 环境。

## 产品决策

- 多工作区选择器继续只在数量大于 1 时出现。
- 选择器显示工作区名称、服务器、项目目录和最近状态。
- 最近状态来自已有 `WorkspaceConnectionStateStore`，不新增主机、用户名、路径或凭据存储。
- “打开过终端”和“SSH 已验证”严格区分，避免把启动终端误报为连接成功。
- 不改变 SSH 命令、tmux 策略、工作区持久化结构和 AI CLI 启动模式。

## 状态映射

| 连接事实 | 选择器文案 |
|---|---|
| 无历史状态 | 尚未验证 |
| 只打开过终端 | 最近打开 |
| 24 小时内验证成功 | 最近验证 |
| 验证成功但已过期 | 验证已过期 |
| 需要用户处理 | 需处理 |
| 检查失败 | 检查失败 |
| 未知结果 | 状态未知 |

## 验收标准

1. 多工作区切换时，用户不需要进入编辑页即可看到服务器、目录和最近状态。
2. 单工作区继续隐藏选择器。
3. 状态文案不声称 SSH 已连接成功，除非诊断事实为新鲜的 `VERIFIED`。
4. 选择器最多 3 行显示，避免在深色主题下硬裁切关键信息。
5. 本轮不触碰真实远端服务器，不启动真实 tmux，不修改 Release 版本。

## 本轮验证

- `WorkspaceActivitySmokeTest.workspaceSelectorShowsRecentConnectionState` 覆盖最近验证状态。
- `WorkspaceActivitySmokeTest.copyWorkspaceKeepsEditedConnectionMetadataAndPersistsIt` 覆盖未验证状态。
- 本地已通过：`./scripts/validate-skills.sh`、`git diff --check`、`./test/version-metadata-test.sh`。
- 共享服务器本地 `./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests
  com.termux.app.WorkspaceActivitySmokeTest` 在 240 秒内无有效输出并被 `timeout` 停止；不在远程共享环境
  继续重试，完整编译、Robolectric 与模拟器证据交由 GitHub CI / Emulator UI 门禁验证。
