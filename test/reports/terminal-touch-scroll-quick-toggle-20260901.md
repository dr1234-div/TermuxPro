# 终端触摸滚动快捷切换验收记录

## 背景

`0.8.0` 已把触摸滚动策略从“固定发送方向键”修正为“默认滚动终端历史，可配置为 AI/TUI 面板滚动”。
但设置入口位于全局设置页，用户在 Claude Code/Codex CLI 终端现场遇到滚动不顺时，需要离开当前上下文。

## 本轮改进

在终端“工具箱 → 当前上下文”中新增触摸滚动模式快捷项：

- 当前为默认历史模式时，显示 `触摸滚动：AI/TUI 面板`，点击后切到 AI/TUI 面板滚动。
- 当前为 AI/TUI 模式时，显示 `触摸滚动：终端历史`，点击后切回普通历史滚动。
- 切换后立即写入偏好并应用到当前 TerminalView，不要求用户重启 App 或重新连接 SSH。
- 入口留在终端工具箱，不回流首页，避免首页重新堆控件。

## 产品验收

- 符合“用户在终端内按需使用工具”的信息架构，不要求返回首页或设置页。
- 默认仍保护 shell 输入，避免手指滑动误触命令历史。
- AI CLI 使用时可在当前上下文快速切换，适合 Claude Code/Codex CLI 的不同滚动语义。

## 自动化证据

- `TerminalProjectToolsMenuTest` 验证菜单分组顺序和触摸滚动项标题。
- `TerminalTouchScrollPolicyTest` 验证默认 scrollback、AI/TUI 鼠标追踪滚轮和非鼠标 fallback。
- `test/terminal-touch-scrollback-test.sh` 锁定触摸路径不会退化为方向键 fallback。

## 已执行本地轻量校验

待提交前执行：

- `./scripts/validate-skills.sh`
- `git diff --check`
- `./test/terminal-touch-scrollback-test.sh`
- `./test/dialog-readable-style-test.sh`

完整 Gradle、Lint、Debug APK 和模拟器截图门禁交给 GitHub Actions。
