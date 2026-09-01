# tmux 会话归属分层体验修复记录（2026-09-01）

## 增值服务准入

本轮不研究或重做 Termux 原始终端、PTY、本地 shell 或基础会话能力，只改 TermuxPro 远程会话中心的
共享 tmux 元数据展示与安全操作层级。原始终端仍可由用户手动使用 tmux 命令，TermuxPro 只在增值页面
提供结构化判断和安全入口。

## 用户问题

共享远端账号下同时存在个人项目会话、其他项目会话、Claude Code 共享会话和手工 tmux 会话。旧列表只
显示“TermuxPro 创建 / 归属未知”，用户无法在进入前判断：

- 这是当前工作区上下文，还是同一 TermuxPro 用户的其他项目；
- 这是其他 TermuxPro 使用者创建的会话，还是没有任何 TermuxPro 标记的手工会话；
- 哪些会话可以安全重命名/停止，哪些只能显式进入。

## 变更

- `TmuxSessionInfo` 增加 `OwnershipState`：当前工作区、其他工作区、其他 TermuxPro 使用者、
  归属标记不完整、未标记。
- tmux 会话列表行展示更细的归属标签。
- 操作弹窗根据归属展示不同安全说明。
- 只有当前工作区会话继续允许重命名和停止；其他归属只允许显式进入。

## 验证

- `git diff --check`：通过。
- `./scripts/validate-skills.sh`：通过。
- `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.TmuxSessionParserTest --tests com.termux.app.TaskSessionsActivityTest`：通过。

## 剩余门禁

- 真实回环 sshd/tmux 生命周期复验。
- GitHub Actions 全模块 CI。
- 360dp 默认字体与 200% 字体截图复验。
