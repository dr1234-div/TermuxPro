# AI CLI 会话中心行动入口验收记录（2026-09-01）

## 增值服务准入

- 分类：AI CLI、远程工作区、移动端工作流效率、上下文工具箱。
- 增值层：让手机用户从一个清晰页面直接进入 Claude/Codex 的新建或历史选择，减少回首页、找工具箱、记命令的成本。
- 非目标：不研究、不重做、不改变 Termux 原始终端、PTY、本地 shell、基础会话或包管理能力。

## 改动范围

- `AiCliSessionCenterActivity` 增加 4 个显式行动入口：
  - 新建 Claude：`claude`
  - Claude 历史：`claude --resume`
  - 新建 Codex：`codex`
  - Codex 历史：`codex resume`
- 所有入口复用当前活动远程工作区，打开独立 `TermuxActivity` 新会话。
- 启动策略固定为 `WorkspaceCommandBuilder.POLICY_SSH_ONLY`，不自动进入、创建或恢复 tmux。
- 工作区缺失或 SSH 目标无效时，不执行命令，退回工作台配置。

## 风险控制

- 不读取 Claude/Codex 私有历史，只打开 CLI 原生历史选择器。
- 不向当前可见 shell 注入命令，避免污染正在运行的任务。
- 不触碰 Termux 原始终端能力；原始能力只作为回归面确认“不受影响”。

## 本轮待验证

- Robolectric：AI 会话中心空状态、工作区展示、工具跳转、AI 启动 Intent 和 tmux 非自动进入断言。
- 静态门禁：深色弹窗/列表可读性规则、diff 空白检查。
- 冒烟：按变更类型运行 `scripts/pre-push-smoke.sh origin/dev`。
