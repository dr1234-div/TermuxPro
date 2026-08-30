# TermuxPro 0.2.0 正式版发布说明

`0.2.0` 聚焦手机端 SSH 远程开发的安全连接、tmux 会话隔离，以及 Codex CLI 与 Claude Code 的显式
会话选择。正式版吸收 `rc.1`、`rc.2` 的回归修复，不会自动进入未指定或归属不明的远端会话。

## 主要改进

- 工作区默认只建立 SSH；是否进入 tmux、默认会话及恢复策略由每个工作区独立配置。
- tmux 列表区分 TermuxPro 自有与归属未知会话；未知会话可显式进入，但应用不能停止。
- 会话归属结合随机 owner、目标指纹、服务端 PID、会话 ID 和创建时间校验，拒绝同名替换与跨目标误用。
- Claude Code、Codex CLI 默认新建安全上下文，历史会话必须显式选择。
- 首页收敛为核心连接路径，Git、文件、任务、诊断和 SSH 密钥等能力移入连接后的工具入口。
- 默认字体与 200% 系统字体均纳入 Android 35、360dp 深色主题截图回归；AI 会话弹窗补充行尾换行空间。
- SSH/tmux 集成测试使用独立 tmux 套接字，禁止清理用户默认服务器及 `hdr-TermuxPro日常迭代` 会话。

## 发布验证

- 全模块 JVM/Robolectric 测试、Android Lint、Debug/Release 构建及 Android 测试源码编译。
- 隔离 SSH/tmux fixture：错误 owner 被拒绝、正确 owner 可管理、未知会话无停止权限。
- Android 35 模拟器：360dp、简体中文、深色主题、默认字体和 200% 字体截图矩阵。
- Release 产物：包名、版本、Launcher、ARM64 ABI、R8、v1/v2 签名、证书与 SHA-256 校验。

## 验证边界

- 自动化覆盖 Android 系统行为、应用布局和隔离 SSH/tmux 集成；不同 OEM 的后台策略、实体软键盘组合和
  长时间弱网仍需持续扩大设备样本，但不绑定某一个手机品牌作为稳定发布的唯一门禁。
- Codex/Claude 历史会话继续使用各自 CLI 原生选择器，统一历史会话中心进入后续版本。

## 安装提醒

- 包名为 `com.termux`，与其他签名来源的 Termux 不能直接覆盖安装。
- 卸载已有 Termux 会删除其应用私有 Linux 环境，请先备份重要数据。
- 密码、私钥和 Token 不写入工作区配置，由 OpenSSH 或远端环境管理。
