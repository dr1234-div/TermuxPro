# 终端工具箱 Git 工作台入口验收记录

## 用户问题

用户主要在手机上通过 SSH 使用 Codex CLI 和 Claude Code。终端工具箱里的 Git 能力如果只是向当前 shell
写入 `git status` 或 `git diff`，用户仍需要在软键盘和终端输出中自己解读分支、改动和提交历史，不符合
TermuxPro 的增值服务方向。

## 产品决策

- 保留 Termux 原始终端能力：用户仍可手动执行任意 Git 命令。
- TermuxPro 增值入口应优先打开移动 Git 工作台，提供结构化状态、分支、同步、Diff、暂存和提交入口。
- 终端工具箱不复制工作区配置逻辑，只通过 `WorkspaceTargetStore` 读取当前活动工作区快照。

## 本轮变更

- “Git 状态”从终端工具箱直接打开 Git 工作台概览。
- “审查 Git Diff”从终端工具箱直接打开 Git 工作台 Diff 页面。
- 没有有效远程工作区时失败关闭，提示用户回到工作台配置服务器和项目目录；不向当前 shell 写入命令。

## 验收重点

- 有有效工作区时，生成 `GitDiffActivity` Intent，并传入 host、port、path。
- Git 状态入口不启动 Diff，Git Diff 入口显式启动 Diff。
- 无工作区、无 host、非法端口或空路径时不打开 Git 工作台，不误用旧工作区或第一个工作区。
- 本轮不修改 Termux 原始终端输入、PTY、shell 或 Git 命令执行能力。

## 已执行验证

- `git diff --check`：通过。
- `./test/terminal-touch-scrollback-test.sh`：通过，证明本轮未破坏终端触摸 scrollback 策略。
- `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.GitWorkbenchNavigationTest --tests com.termux.app.GitDiffActivityTest --tests com.termux.app.TerminalProjectToolsMenuTest --tests com.termux.app.CustomLayoutsSmokeTest`：通过。
- `./scripts/pre-push-smoke.sh origin/dev`：通过。

## 待远端门禁

- GitHub Actions `TermuxPro CI`
- GitHub Actions `TermuxPro Emulator UI`
- 自动 PR 合并后 `dev` 收尾 CI
