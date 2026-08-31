# tmux 会话列表上下文增强验收记录

## 用户问题

用户的核心路径是手机 SSH 到远端后继续 Codex CLI、Claude Code 或 tmux 工作。共享 Unix 账号或共享
Claude Code 环境中，tmux 列表如果只显示名称、窗口数和归属，用户很难判断“这个会话是不是我现在应该进
的上下文”。误入会话虽不等同于删除，但会污染专用迭代会话、造成上下文混乱，并增加共享账号下的风险。

## 本轮产品决策

- 不读取 pane 内容，不解析 Claude/Codex 历史正文，避免泄露共享账号内容。
- 增强 tmux 自身脱敏元数据：会话名、窗口数、是否 attached、创建时间、最近活跃时间、TermuxPro 归属。
- 未知归属仍只允许显式进入，不提供重命名或停止。
- tmux 枚举改为先读取 `session_id`，再按 session id 查询字段；不再用 `session_id:session_name` 按冒号切分，
  避免未知会话名包含冒号时误解析。

## 验收标准

1. 列表行必须展示创建时间和最近活跃时间，帮助用户判断上下文新旧。
2. 归属未知会话仍不可被 TermuxPro 停止或重命名。
3. 命令构造不得读取窗格内容，不得 attach，不得出现 `kill-server`。
4. NUL 分隔解析必须保留包含冒号的会话名。
5. 缺失 tmux 时仍显示普通 SSH 退路。

## 当前证据

- 聚焦 JVM/Robolectric：
  `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests 'com.termux.app.TmuxSessionParserTest' --tests 'com.termux.app.TaskSessionsActivityTest' --tests 'com.termux.app.WorkspaceCommandBuilderTest'`
  通过，77 个任务执行或复用缓存。

## 待补证据

- `./scripts/test-all.sh`
- GitHub CI 与 Emulator UI
- dev 合并后 CI
