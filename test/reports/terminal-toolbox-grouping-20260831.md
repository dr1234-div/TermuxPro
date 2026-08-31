# 终端工具箱分组验收记录

## 用户问题

用户主要在终端里完成 SSH 远程开发、Codex CLI、Claude Code、tmux 与 Git 操作。首页已经不再平铺低频工具，
但终端里的工具箱如果仍是一个没有信息层级的长列表，用户需要逐项读完才能找到“当前会话、快捷指令、
Git、AI 安全动作、键区切换”等不同类型的操作。

## 本轮产品决策

- 保留现有功能入口，不删减用户已熟悉的动作。
- 把工具箱从原始平铺顺序改为按开发者任务分组：
  1. 当前上下文：tmux 会话、快捷指令、搜索输出。
  2. 项目与 Git：Git 状态、Git 修改、项目任务。
  3. AI 安全操作：确认当前选项、拒绝/返回、中断。
  4. 键区切换：Shell、AI CLI、Vim。
- 分组标题为禁用菜单项，不触发任何终端输入，避免误操作。

## 验收标准

1. 工具箱入口仍为终端顶栏可见文字“工具箱”。
2. 菜单必须按开发任务分组，而不是单层平铺。
3. AI 确认、拒绝和中断仍保留在显式安全操作分组里。
4. 键区切换不与执行命令混在一起。
5. 新增结构有自动化测试覆盖，防止后续退回平铺长列表。

## 当前证据

- 聚焦 JVM/Robolectric：
  `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests 'com.termux.app.TerminalProjectToolsMenuTest' --tests 'com.termux.app.CustomLayoutsSmokeTest'`
  通过，77 个任务执行或复用缓存。
- 本地全量低资源验证：
  `TERMUXPRO_USE_CHINA_MIRROR=1 ./scripts/test-all.sh && git diff --check`
  通过，169 个 Gradle 任务执行或复用缓存；Skill 校验、workflow 策略校验、签名 APK 覆盖升级冒烟脚本均通过。

## 待补证据

- GitHub CI 与 Emulator UI
- dev 合并后 CI
