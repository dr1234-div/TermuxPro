# 终端工具箱 tmux 会话中心入口回归记录

## 用户问题

TermuxPro 不应把 Termux 已有基础命令当作主要增值点重复展示。终端工具箱中的 `tmux 会话` 如果只是向
当前 shell 写入 `tmux list-sessions`，用户仍然只能阅读原始输出，无法完成安全的新建、进入、重命名、
停止和归属判断，也可能污染当前 Claude/Codex/TUI 输入上下文。

## 产品决策

- Termux 原始终端能力只做守护，不重造、不干扰。
- TermuxPro 增值层负责把高频远程研发动作做成可视化、可恢复、可校验的页面。
- 终端工具箱 `tmux 会话` 入口改为打开当前活动远程工作区的会话中心。
- 当前没有有效工作区时失败关闭，只提示返回工作台配置服务器、项目目录和 tmux 策略。

## 验收重点

- 不再向当前终端注入 `tmux list-sessions`。
- 使用当前活动工作区解析 host、port、project path 和本地 owner token。
- 会话 CRUD、归属核验和 `kill-session`/禁止默认 `kill-server` 仍由既有会话中心负责。
- 原始 shell、滚动、快捷键和用户手工 tmux 命令不被拦截或替换。

## 验证

- `git diff --check` 通过。
- 静态检索确认 `TermuxActivity` 不再包含 `confirmAndSendCommand("tmux list-sessions")`。
- `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.TaskSessionsNavigationTest --tests com.termux.app.TaskSessionsActivityTest --tests com.termux.app.WorkspaceOwnershipStoreTest --tests com.termux.app.TerminalProjectToolsMenuTest --tests com.termux.app.CustomLayoutsSmokeTest` 通过。
- `./scripts/pre-push-smoke.sh origin/dev` 通过；包含 Skill 校验、版本元数据、workflow 触发策略、GitHub
  CLI 封装校验、check suite 诊断脚本测试、资源守卫和相关 Gradle 单元测试。
- 远端 CI 和 PR 编号待补。
