# AI CLI 会话中心 Git 下一步入口验收记录（2026-09-01）

## 增值服务准入

- 分类：AI CLI、远程工作区、Git 可视化、上下文工具箱。
- 增值层：用户在手机上启动 Claude/Codex 后，下一步通常是查看当前项目分支、改动和提交。入口放在 AI 会话中心，减少回首页或记命令成本。
- 非目标：不改造 `git` 命令本身，不研究或重做 Termux 原始终端能力。

## 改动范围

- AI CLI 会话中心新增“查看 Git 改动”入口。
- 入口复用 `GitWorkbenchNavigation.newIntentForActiveWorkspace(..., false)`，打开结构化 Git 工作台概览。
- 工作区缺失时失败关闭并回到工作台配置，不向当前 shell 注入 `git status` 或其他命令。

## 验收重点

- 有有效工作区时，入口打开 `GitDiffActivity` 且默认进入概览模式。
- 无有效工作区时，入口回到 `WorkspaceActivity`。
- 不改变 Termux 原始终端、PTY、滚动、基础 shell 行为。
