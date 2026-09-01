# tmux 会话中心安全排序验收记录

## 用户问题

用户主要通过 Android 手机 SSH 到远程服务器继续 Codex CLI / Claude Code 工作。共享 Unix 账号或共享
Claude Code 环境下，tmux 列表可能混有当前项目、其他项目、其他 TermuxPro 使用者和手工会话。如果列表
直接沿用远端 tmux 原始顺序，当前工作区会话可能被未知会话或他人会话挤到后面，增加误进错误上下文的风险。

## 本轮改动

- 不改变远端 tmux 查询命令，不读取 pane 内容，不解析 AI CLI 历史正文。
- 仅在 App 本地对已解析的脱敏会话元数据排序：
  1. 当前工作区；
  2. 其他 TermuxPro 工作区；
  3. 其他 TermuxPro 使用者；
  4. TermuxPro 标记不完整；
  5. 未标记/归属未知。
- 同一归属等级内按最近活跃时间、创建时间、名称排序。

## 产品与安全裁定

- 产品：通过。用户进入会话中心后，最可能应该操作的当前工作区会话优先可见。
- UI/UX：通过。减少扫读负担和误点风险，不新增首页控件。
- 架构：通过。排序逻辑独立在 `TmuxSessionDisplayOrder`，解析器仍保持原始解析语义。
- 安全：通过。非当前工作区仍只允许显式进入，不开放重命名/停止；没有任何 `kill-server` 路径。

## 回归面

1. 当前工作区会话即使活跃时间较旧，也必须排在未知或其他归属会话之前。
2. 同类当前工作区会话按最近活跃和创建时间排序。
3. 解析器仍保留原始 tmux 输出顺序，避免排序副作用污染其他调用。
4. 原始 Termux 终端能力不受影响，本轮只修改 TermuxPro 会话中心增值展示。

## 验收证据

- `TmuxSessionDisplayOrderTest.currentWorkspaceSessionsAreShownBeforeSharedOrUnknownSessions`
- `TmuxSessionDisplayOrderTest.sameOwnershipUsesRecentActivityThenName`
- `TaskSessionsActivityTest.previewShowsOwnedAndUnknownSessionsWithDiscoverableCreateAction`
