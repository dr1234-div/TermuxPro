# SSH 目标粘贴输入容错

## 背景

用户实际使用 TermuxPro 的主路径是 Android 手机 SSH 到远程服务器后使用 Codex CLI 和 Claude Code。
手机端经常从聊天、文档或服务器信息中复制完整 SSH 命令，而不是手动拆成“地址”和“端口”两个字段。
如果产品只接受严格的 `user@host`，会让首次连接变成猜规则。

## 增值服务分类

- 远程工作区。
- AI CLI 移动端启动体验。
- Git/tmux/文件/诊断等远程工具入口的一致性。

该切片不研究、不重做 Termux 原始终端、PTY、本地 shell、包管理或基础 SSH 客户端能力，只提升
TermuxPro 工作区外壳对远程目标输入的容错和安全标准化。

## 本轮改动

- 新增 `SshTargetParser`，把常见粘贴格式统一解析为 OpenSSH 单一目标参数和端口：
  - `user@host`
  - `user@host:port`
  - `ssh -p port user@host`
  - `ssh://user@host:port/path`
- `WorkspaceActivity` 的保存、远程连接、Git 工作台、tmux 会话、远程文件、项目任务、连接诊断、
  SSH 密钥和 Web 预览入口统一使用解析后的目标。
- 保存前会把粘贴内容写回标准字段，例如 `ssh -p 22022 hdr@192.168.1.153` 会变成：
  - SSH 地址：`hdr@192.168.1.153`
  - 端口：`22022`
- 收紧 `SshTargetValidator`，拒绝 `;`、`|`、`/` 等不应出现在 SSH 单一目标参数中的字符，避免错误目标
  进入后续远程工具链。

## 已执行验证

```text
./test/dialog-readable-style-test.sh
git diff --check
TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest \
  --tests 'com.termux.app.SshTargetParserTest' \
  --tests 'com.termux.app.SshTargetValidatorTest' \
  --tests 'com.termux.app.WorkspaceActivitySmokeTest' \
  --tests 'com.termux.app.WorkspaceCommandBuilderTest'
```

结果：

- 静态弹窗可读性门禁通过。
- SSH 目标解析、目标校验、工作区保存标准化和 SSH 命令构造测试通过。
- 本轮没有连接真实远端服务器，没有访问 153 机器，也没有执行 tmux 命令。

## 后续验收

- 候选版前用模拟器覆盖工作区输入：纯目标、带端口目标、完整 ssh 命令粘贴。
- 真机体验时重点验证小米/三星软键盘复制粘贴后字段是否自动规整，错误文案是否能让用户知道该如何修正。
