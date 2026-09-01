# 终端工具箱 AI 启动入口验收记录

## 用户问题

用户的核心场景是在 Android 手机上 SSH 到远程服务器后使用 Claude Code 和 Codex CLI。此前终端页顶部已有
Claude/Codex 快捷按钮，但“工具箱”里的 AI 分组只提供确认、拒绝和中断等安全动作。用户在终端现场打开
工具箱寻找 AI 工作入口时，容易把安全动作误认为完整 AI 能力，或需要返回工作台/记住顶部按钮。

## 本轮改动

- 将工具箱分组文案从“AI 安全操作”调整为“AI 工作”。
- 在该分组顶部新增“启动 Claude Code”和“启动 Codex CLI”。
- 点击后复用既有 AI 启动弹窗、工具差异说明、安全默认新建和历史会话显式选择。
- 终端页启动弹窗补齐当前活动工作区目标说明，展示服务器、端口和项目路径；工作区缺失或配置不完整时
  失败关闭为“当前目标未完整配置”。
- 不向当前 shell 静默注入历史恢复命令，不解析 Claude/Codex 历史数据，不修改 Termux 原始终端输入、
  PTY、滚动或本地会话能力。

## 产品与 UI/UX 裁定

- 产品：通过。AI 工作入口回到用户真实任务流，工具箱不再只暴露底层安全动作。
- UI/UX：通过。AI 启动动作位于安全动作之前，符合“先完成任务，再处理异常/控制”的阅读顺序。
- 架构：通过。复用现有 `showAiLaunchDialog()` 和 `AiCliLaunchCommand`，并抽出 `AiCliLaunchMessage`
  统一工作台与终端入口的目标提示，不新增远端命令拼接策略。
- 安全：通过。Claude 共享账号仍默认新建；历史入口仍只打开 CLI 原生选择器，由用户显式选择。

## 回归面

1. 工具箱分组顺序必须保持“当前上下文 / 项目与 Git / 连接与安全 / AI 工作 / 键区切换”。
2. AI 工作分组必须先展示 Claude/Codex 启动，再展示确认、拒绝和中断。
3. AI 启动命令策略不变：Claude 新建 `claude`，历史 `claude --resume`；Codex 新建 `codex`，历史
   `codex resume`。
4. 终端页和工作台页的 AI 启动弹窗都必须展示同一目标上下文规则。
5. 原始 Termux 终端能力不受影响，本轮只修改 TermuxPro 增值工具箱入口。

## 验收证据

- `TerminalProjectToolsMenuTest.toolboxUsesDeveloperTaskGroupsBeforeRawActions` 覆盖新增菜单项、中文标题和
  分组顺序。
- `AiCliLaunchCommandTest` 继续覆盖 Claude/Codex 新建与历史命令策略。
- `AiCliLaunchCommandTest.launchMessageShowsTargetForTerminalAndWorkspaceEntrypoints` 覆盖终端与工作台共用
  的目标提示。
- `AiCliLaunchCommandTest.launchMessageFailsClosedWhenTerminalHasNoConfiguredWorkspace` 覆盖终端无有效工作区
  时失败关闭。
- `test/ai-launch-decision-copy-test.sh` 继续覆盖共享 Claude 账号风险和“不自动进入最近会话”文案。
