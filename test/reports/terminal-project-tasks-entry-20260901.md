# 终端工具箱项目任务入口回归记录

## 问题

终端工具箱的“运行项目检查”此前会向当前 shell 注入一段启发式测试命令。用户主要在手机端 SSH 使用
Codex CLI/Claude Code，这种入口会污染当前 AI/TUI 输入上下文，也无法提供任务列表、确认、失败恢复和
持久会话管理。

## 产品决策

- Termux 原始 shell 能力保持可用，用户仍可手动运行任意项目命令。
- TermuxPro 增值入口不再写入当前 shell，而是打开“项目任务”页面。
- 项目任务页负责远端元数据识别、任务列表、命令确认和独立持久会话启动。
- 没有有效工作区时失败关闭，提示回到工作台配置服务器和项目目录。

## 验收重点

- 终端工具箱不再注入 package/npm/pnpm/mvn/gradle 检测命令。
- 有活动工作区时打开 `ProjectTasksActivity`。
- 无活动工作区、非法端口、空路径或非法 owner token 时不误用旧配置。
- 原始终端输入、滚动、快捷键和手工命令不受影响。

## 验证

- `git diff --check` 通过。
- `./test/pre-push-smoke-test.sh` 通过；项目任务相关代码已纳入推送前冒烟映射。
- `./test/workflow-trigger-policy-test.sh` 通过。
- 静态检索确认 `TermuxActivity` 不再包含 `pnpm test`、`mvn test`、`gradlew test` 或
  `No supported project check` 等项目检查命令注入。
- `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests com.termux.app.ProjectTasksNavigationTest --tests com.termux.app.ProjectTaskDetectorTest --tests com.termux.app.RemoteToolRecoveryTest --tests com.termux.app.WorkspaceCommandBuilderTest --tests com.termux.app.TerminalProjectToolsMenuTest --tests com.termux.app.CustomLayoutsSmokeTest` 通过。
- `./scripts/pre-push-smoke.sh origin/dev` 通过；包含静态门禁、资源守卫和映射 Gradle 测试。
- PR 和远端 CI 编号待补。
