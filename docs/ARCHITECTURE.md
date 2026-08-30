# TermuxPro 架构说明

TermuxPro 基于官方 Termux 的 Java/XML Android 工程，复用其 PTY、终端渲染、bootstrap、前台服务
和软件包生态，在 `app` 模块增加移动 AI 开发工作区。

## 当前边界

- `WorkspaceActivity` 是默认入口，管理本地/SSH 工作区和开发工具。
- `TermuxActivity` 继续承载终端渲染、输入、会话和额外按键。
- SSH、tmux、Claude Code、Codex CLI 均在 Termux 用户空间通过官方 OpenSSH/命令行工具运行。
- 每个本地工作区拥有独立的非敏感随机 UUID。TermuxPro 创建远端 tmux 会话时同时写入
  `@termuxpro_owner` 和由 SSH 目标、端口、项目路径生成的非敏感摘要。恢复、附着和停止先按完整名称解析
  tmux server PID、不可复用 session ID 与创建时间，再在同一个 tmux 服务端命令队列中校验句柄和两项
  标记后执行动作；同名会话在检查期间被替换时必须失败关闭。名称前缀不能授予管理权限。
- 这些标记用于防止共享账号下的误进入和误停止，不是同一 Unix UID 内的安全认证：同 UID 进程本就能
  读取或修改 tmux 状态。需要抵御恶意参与者时必须使用不同 Unix 账号、容器或独立主机进行系统级隔离。
- 远端探测使用有界、可取消的命令执行；文件浏览为只读，避免手机误操作覆盖源码。
- `com.termux`、`/data/data/com.termux/files/usr` 和官方 bootstrap 暂时保留。这是兼容约束，
  不是品牌声明，也意味着不同签名来源无法共存。
- 不引入 NewTermux 或其他 Termux 衍生项目代码、设计和依赖。

## 安全不变量

- 不保存 SSH 密码、私钥、AI Token；不把敏感命令输出写入日志。
- SSH 目标与路径必须通过校验和安全引用，不能拼接为可注入选项。
- AI 确认必须由用户二次操作；拒绝和中断使用明确控制键。
- Web 预览隧道默认只绑定 `127.0.0.1`。
- 远端读取、搜索和 Diff 必须限制数据量并支持取消。

## 后续演进

独立 applicationId、自有 bootstrap/软件仓库、Mosh、可写 SFTP、增强远端代理和应用内模型属于独立
架构里程碑。任何一项都必须先有迁移、威胁模型和回滚方案，不能为了表面改名破坏现有本地环境。
