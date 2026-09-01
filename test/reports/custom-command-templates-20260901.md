# 快捷指令模板增值切片验收记录

日期：2026-09-01

## 增值服务准入

- 分类：自定义快捷指令、AI CLI、tmux/Git 可视化、移动端工作流效率。
- 用户问题：手机软键盘输入长命令成本高，空白快捷指令页让用户继续依赖记忆和手写命令。
- 非目标：不研究、不重写 Termux 原始终端、PTY、包管理、本地 shell、基础会话或基础快捷键能力。
- 安全边界：模板只预填编辑器；保存前不落盘，运行前仍经过确认、危险命令识别和秘密检测。

## 本轮交付

- 快捷指令页新增“模板”入口。
- 空状态新增模板说明，引导用户从 Codex、Claude、Git、tmux 和前端检查模板开始。
- 模板入口与“新建”按钮拆到独立动作栏，保持 48dp 触控目标和 200% 字体可用性。
- `scripts/pre-push-smoke.sh` 增加快捷指令相关变更到 `CustomCommandsActivityTest`、
  `CustomCommandStoreTest` 和 `WorkspaceCommandBuilderTest` 的映射，避免后续漏测。

## 验收证据

- `git diff --check`：通过。
- `./scripts/validate-skills.sh`：通过。
- `test/pre-push-smoke-test.sh`：通过。
- `test/dialog-readable-style-test.sh`：通过。
- `TERMUXPRO_DRY_RUN=1 scripts/pre-push-smoke.sh origin/dev`：确认快捷指令变更会触发
  `CustomCommandsActivityTest`、`CustomCommandStoreTest`、`CustomLayoutsSmokeTest`、
  `WorkspaceActivitySmokeTest` 和 `WorkspaceCommandBuilderTest`。
- `scripts/pre-push-smoke.sh origin/dev`：通过；Gradle 编译与相关 Robolectric 单测成功，
  `BUILD SUCCESSFUL in 36s`。

## 回归面

- 模板选择后不自动保存、不自动执行。
- 保存后才进入当前工作区快捷指令列表。
- 无效工作区禁用模板与新建入口。
- 200% 字体下快捷指令标题、返回和主操作仍在窄屏内可见。
- 原始 Termux 终端能力未被修改；本轮只增加 TermuxPro 增值入口。
