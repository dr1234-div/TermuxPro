# 终端触摸滚动策略补强验收记录

## 用户反馈

用户体验 `0.5.0` 安装包时发现：终端页无法上下滚动查看历史输出，手指上下滑动被解释成命令行
上/下方向键，导致 shell、Codex CLI 或 Claude Code 中的命令历史/列表选择被移动。

## 产品裁定

- 优先级：P1。
- 影响路径：手机软键盘 + SSH 远程终端 + AI CLI 长输出阅读。
- 正确语义：手机屏幕上的单指竖向滑动默认用于阅读 scrollback，不应向远端进程写入
  `DPAD_UP` / `DPAD_DOWN`。
- 兼容保留：外接鼠标滚轮仍可在支持 mouse tracking 或 alternate screen 的 TUI 中保持程序内滚动。

## 修复范围

- `TerminalView.doScroll(MotionEvent, int)` 改为根据输入来源自动选择策略。
- 新增 `TerminalView.shouldForceScrollbackForScrollEvent()`：
  - 非鼠标来源：强制 scrollback。
  - 外接鼠标来源：保留既有程序内滚动兼容路径。
- 保留手指拖动、手指惯性滑动和 Shift+Page 既有强制 scrollback 规则。

## 回归门禁

- `TerminalTouchScrollPolicyTest` 覆盖：
  - 默认触摸事件必须强制 scrollback。
  - `SOURCE_TOUCHSCREEN` 必须强制 scrollback。
  - `SOURCE_MOUSE` 必须保留程序内滚动兼容。
  - 空事件保持 legacy 兼容路径。
- `test/terminal-touch-scrollback-test.sh` 增加源码级不变量：
  - 默认 `doScroll` 必须调用输入来源策略。
  - 非鼠标滚动不得落入 alternate screen 方向键路径。

## 本地验证

- `./test/terminal-touch-scrollback-test.sh`：通过。
- `./scripts/validate-skills.sh`：通过。
- `git diff --check`：通过。
- `./test/version-metadata-test.sh`：通过，当前版本元数据为 `0.7.1 (70102)`。
- `./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.TerminalTouchScrollPolicyTest`：
  本地共享服务器 240 秒无输出后超时停止，未继续占用资源；完整 JVM/Robolectric 与模拟器回归交给 GitHub CI。

## 验收结论

本轮是触摸滚动策略补强，不触碰 SSH、tmux、远端命令和用户会话。候选版发布前仍需在模拟器或可用
Android 设备上验证真实终端页长输出拖动手感。
