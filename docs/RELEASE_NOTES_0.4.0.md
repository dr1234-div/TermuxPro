# TermuxPro 0.4.0 正式版发布说明

`0.4.0` 是远程 tmux 会话中心与发布门禁治理的正式版本，面向手机 SSH 远程开发、Codex CLI 与
Claude Code 使用场景。本版本不再自动进入未知 tmux 会话，而是以显式选择和归属校验保护共享服务器。

## 核心变化

- 新增远程 tmux 会话中心：支持查看、新建、进入、重命名和安全停止 TermuxPro 自有会话。
- 区分 TermuxPro 自有会话与归属未知的共享会话；未知归属默认只读，只允许显式进入，不提供写操作。
- 新建会话绑定当前服务器、项目目录、tmux server、session id 与 ownership 元数据，避免误接管他人会话。
- 重命名和停止前重新核验 owner、工作区指纹和 tmux 实例身份；归属变化、外部删除或同名冲突会失败关闭。
- 连接策略支持普通 SSH、显式选择 tmux、新建/恢复指定 tmux 等多场景，不再默认污染共享会话。
- Codex CLI / Claude Code 继续采用显式新建或原生历史选择器，避免共享 Claude 账号下误续接他人上下文。
- 发布流水线改为先构建正式签名 APK，并在模拟器中完成上个稳定版到当前版本的覆盖升级验收后，才创建公开 Release。
- 研发 PR 不再触发由 `github-actions[bot]` PR 事件导致的 `pull_request action_required` 噪声。

## 验收结论

- `v0.4.0-rc.2` 已通过完整 CI、模拟器 UI、merge commit 收尾 CI 和签名 APK 覆盖升级发布门禁。
- `0.4.0` 正式版沿用同一冻结范围，`versionCode=40003`，高于 `0.4.0-rc.2` 的 `40002`。
- 正式 Release 仍会在发布前执行签名 APK 覆盖升级、Launcher/桌面名称、进程和 AndroidRuntime 冒烟验证。

## 已知限制

- TermuxPro 只管理带有匹配 ownership 元数据的自有会话，不会接管既有共享 tmux 会话。
- Claude Code 与 Codex CLI 历史会话仍使用各自原生选择器，统一 AI 会话元数据中心尚未完成。
- Android 实体设备或云真机上的软键盘、弱网、后台恢复及 OEM 行为仍需在后续版本继续补充证据。

## 安装提醒

- 包名保留为 `com.termux`，与其他签名来源的 Termux 不能直接覆盖安装。
- 卸载应用会删除其私有 Linux 环境和本地配置，升级体验前请备份重要数据。
- 如果你已经安装 `0.4.0-rc.2`，可直接覆盖升级到本正式版。
