# 终端触摸滚动模式可发现性验收记录

## 用户问题

用户在 `0.5.0` 体验包中反馈：终端页面无法通过手指上下滑动查看上方历史输出，滑动被表现为命令行上下选择。

## 处理边界

- 不重构 Termux 原始终端内核。
- 保持原始终端能力不被破坏：手机触摸默认滚动 scrollback，外接鼠标滚轮继续兼容终端程序内滚动。
- TermuxPro 只在增值服务层补强可发现性：用户应能直接看出当前触摸滚动模式。

## 本轮变更

- 终端顶栏工具入口根据当前设置显示为“工具箱 · 历史”或“工具箱 · TUI”。
- 工具箱菜单中的切换项改为“当前：终端历史；点此切换到 AI/TUI 面板”或“当前：AI/TUI 面板；点此切换到终端历史”。
- 切换后同步更新按钮文字与无障碍描述，避免状态与实际行为不一致。

## 验收重点

- 默认模式必须仍是终端历史滚动，不能向 shell、Codex CLI 或 Claude Code 写入上下方向键。
- AI/TUI 模式只有在远端程序支持鼠标追踪时才发送滚轮事件；不支持时仍回退到终端历史滚动。
- 入口保留在终端页工具箱，不回流首页，不增加首页控件负担。

## 已执行验证

- `./test/terminal-touch-scrollback-test.sh`：通过。
- `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.TerminalProjectToolsMenuTest --tests com.termux.app.TerminalTouchScrollPolicyTest --tests com.termux.app.CustomLayoutsSmokeTest`：通过。

## 限制

本轮未启动本地 Android 模拟器：当前机器 KVM 不可用且工作区磁盘使用率约 95%。该限制不影响本轮源码策略和 JVM/UI 资源验证；推送后仍以 GitHub Actions 的完整 CI 与模拟器 UI 门禁作为合入依据。
