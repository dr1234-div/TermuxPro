# Git 工作台安全删除本地分支验收记录

## 用户问题

用户反馈 Git 能力仍不闭环：能创建/切换分支，但无法完成本地临时分支的维护和删除；这会让手机端
Git 工作台更像命令集合，而不是面向开发者真实流程的产品。

## 产品裁定

- 优先级：P1。
- 场景：手机 SSH 进入远端项目后，查看当前仓库状态，清理已经合并或不再需要的本地临时分支。
- 非目标：
  - 不删除远端分支。
  - 不提供 `git branch -D` 强制删除。
  - 不删除当前分支。
  - 不把 Git 提交做成“删除”伪 CRUD。

## 设计与交互

- 在 Git 工作台横向操作区增加“删除本地分支”，和“分支 / 新建分支”放在同一上下文。
- 只展示可安全删除的本地分支，当前分支不进入删除列表。
- 删除前必须二次确认，并说明不会删除远端、不会 force、未合并时 Git 会拒绝。
- 危险确认按钮使用 `tp_danger`，避免和普通主操作混淆。
- 无可删除分支时给出解释，不让用户猜为什么按钮不可用。

## 安全边界

- 远端命令只执行 `git branch -d -- <branch>`。
- 分支名经过本地保守校验和远端 `git check-ref-format --branch` 复核。
- 当前分支返回专用退出码 `79`。
- 本地分支不存在返回专用退出码 `80`。
- 未合并分支由 Git 原生命令拒绝，工作树、index 和远端分支均保留。
- 不执行 `git push`，不执行 `git branch -D`。

## QA 门禁

- `WorkspaceCommandBuilderTest`：
  - 断言命令包含 `git branch -d --`。
  - 断言命令不包含 `git branch -D` 和 `git push`。
  - 断言非法分支名被拒绝。
  - 使用真实临时 Git 仓库验证：已合并本地分支可删除、当前分支不可删除、缺失分支返回 `80`、
    未合并分支不能被安全删除且仍保留。
- `GitDiffActivityTest`：
  - 删除列表不包含当前分支。
  - 存在其他本地分支时删除入口启用。
  - 危险确认弹窗文案包含“不删除远端分支”。
  - 危险确认按钮使用 `tp_danger`。

## 本地验证

- XML 解析：`strings.xml`、`values-zh-rCN/strings.xml`、`activity_git_diff.xml` 通过。
- `./scripts/validate-skills.sh`：通过。
- `git diff --check`：通过。
- `./test/version-metadata-test.sh`：通过，当前版本元数据为 `0.7.1 (70102)`。
- `./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.WorkspaceCommandBuilderTest --tests com.termux.app.GitDiffActivityTest`：
  本地共享服务器 240 秒无输出后超时停止，未继续占用资源；完整 JVM/Robolectric、Lint、Debug APK 与
  模拟器 UI 交给 GitHub CI。

## 验收结论

本轮补齐 Git 本地分支生命周期的“删除”出口，但保持安全默认值：只允许安全删除本地非当前分支。
后续继续补按文件暂存、stash 和冲突恢复。
