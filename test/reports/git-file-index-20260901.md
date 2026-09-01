# Git 工作台按文件暂存验收记录（2026-09-01）

## 增值服务准入

- 分类：tmux/Git 可视化。
- 目标：补齐移动端 Git 工作台的文件级 index 管理，减少用户为单文件暂存/取消暂存返回命令行的频率。
- 非目标：不重做 Git 原生命令；不实现丢弃修改、强制 reset、rebase、merge 冲突解决或远端推送策略。

## 产品与安全决策

- 概览协议追加 `TP_STATUS_Z` + `git status --porcelain=v1 -z`，App 只解析状态和路径，不解析文件内容。
- 新增“按文件”入口：展示已暂存、未暂存、已暂存 + 未暂存三种范围。
- 单文件操作只改变 index：
  - 暂存：`git add -A -- <path>`。
  - 取消暂存：有 HEAD 时 `git restore --staged -- <path>`；无 HEAD 时仅从 index 移除该路径。
- 所有路径来自 Git 状态列表并经过 shell quoting；拒绝空路径和 NUL 路径。
- 不提交、不推送、不丢弃、不执行 `reset --hard`。

## 验证

- `git diff --check`：通过。
- `./test/pre-push-smoke-test.sh`：通过。
- `./scripts/pre-push-smoke.sh origin/dev`：通过，包含资源守卫、Skill/版本/工作流/GitHub CLI 静态门禁和相关 Gradle/Robolectric 冒烟测试。
- 远端 CI 与模拟器 UI：待自动 PR 流水线验证。
