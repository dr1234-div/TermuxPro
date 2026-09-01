# 终端触摸滚动模式缺陷修复记录

## 背景

用户在 0.5.0 体验中反馈：终端页面无法像手机应用一样上下滑动查看历史输出，手指滑动会表现为命令行上下历史选择。

## 产品判断

手机端终端的触摸滑动不能只有一种语义：

- 普通 shell/SSH 场景：手指上下滑动应优先阅读终端历史输出，不能写入上下方向键污染当前命令行。
- Claude Code、Codex CLI、vim、less、tmux pane 等 AI/TUI 场景：用户可能希望滚动程序内部面板，而不是退出到历史输出。

因此本次不再把“触摸滑动”硬编码成单一路径，而是增加触摸滚动模式：

1. `滚动终端历史（推荐）`：默认值。触摸滑动进入 scrollback，不发送方向键。
2. `滚动 AI/TUI 面板`：当终端程序启用鼠标追踪时，触摸滑动转为鼠标滚轮事件；如果程序不支持鼠标追踪，仍回退为 scrollback，不发送上下方向键。

## 实现约束

- 外接鼠标滚轮保留原有程序内滚动兼容能力。
- 手机触摸滑动不再落入 alternate screen 的 `KEYCODE_DPAD_UP/DOWN` fallback。
- 设置项放在“设置 → 终端显示 → 触摸滚动模式”。
- 默认值保持安全：保护 shell 输入和命令行历史。

## 验收点

- 默认模式下，在普通 SSH shell 中执行多屏输出后，手指上下滑动可以查看历史输出。
- 默认模式下，手指滑动不会触发 shell 命令历史上下切换。
- AI/TUI 模式下，Claude Code/Codex CLI 等启用鼠标追踪的程序可接收滚轮事件滚动内部面板。
- AI/TUI 模式下，如果当前程序不支持鼠标追踪，触摸滑动仍滚动 scrollback，不发送上下方向键。
- 外接鼠标滚轮继续兼容 less/vim/tmux pane 等程序内滚动。

## 已执行校验

- `./scripts/validate-skills.sh`
- `git diff --check`
- `./test/terminal-touch-scrollback-test.sh`
- `./test/dialog-readable-style-test.sh`

## 受限校验

本地远程机为共享环境，Gradle 聚焦单测启动后超过 120 秒无输出。为避免占用 CPU/内存，已停止本地重任务，完整编译、Robolectric、Lint、Debug APK 和模拟器验收交由 GitHub Actions 执行。
