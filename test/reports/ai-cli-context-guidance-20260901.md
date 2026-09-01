# AI CLI 启动上下文提示验收记录

## 背景

用户主要在手机端 SSH 到远程服务器使用 Codex CLI 和 Claude Code。Claude Code 在共享账号下存在“误入他人
上下文/误恢复历史会话”的风险；Codex CLI 虽通常按用户隔离，也不应由 TermuxPro 自动进入最近会话。

本轮只打磨 TermuxPro 增值服务，不研究或重做 Termux 原始终端能力。

## 改动

- `Claude Code` 启动弹窗新增共享账号风险说明，明确“新建会话”为安全默认。
- `Codex CLI` 启动弹窗新增当前项目确认说明，明确 TermuxPro 不自动进入最近会话。
- 历史入口继续只打开 CLI 原生选择器，由用户显式选择目标会话。
- 使用中英文资源字符串，不硬编码文案。

## 非目标

- 不解析 Claude/Codex 私有历史数据。
- 不自动选择最近 AI 会话。
- 不修改 Termux 原始终端输入、PTY、滚动或会话行为。

## 验收标准

- Claude 和 Codex 弹窗必须有不同的上下文提示。
- Claude 提示必须覆盖共享账号风险。
- Codex 提示必须覆盖不自动进入最近会话。
- 新建和历史命令策略保持不变。

## 回归面

- `AiCliLaunchCommandTest` 锁定命令策略和提示资源映射。
- `dialog-readable-style-test.sh` 继续保证 AI 弹窗走 TermuxPro 可读样式。
- `pre-push-smoke.sh` 按变更范围补跑相关 Android/Robolectric 测试。
