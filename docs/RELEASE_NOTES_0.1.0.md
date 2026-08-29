# TermuxPro 0.1.0 测试版发布说明

## 产品定位

TermuxPro 是基于官方 Termux 源码构建的 Android 移动开发终端，第一阶段聚焦 SSH 连接远程开发机，
优化 Claude Code、Codex CLI、tmux 和软键盘工作流。

## 本版功能

- 简体中文工作区首页和触屏开发工具面板
- 多远端工作区保存、切换和 tmux 会话恢复
- Claude Code、Codex CLI 一键启动/继续
- 多行提示词编辑，Shell、AI CLI、Vim 三套快捷键
- AI 当前选项确认、Esc 拒绝/返回和 Ctrl+C 中断；应用不自动授权
- Git 修改审查、远端文件只读浏览和文本预览
- Vue、React、uni-app、Node.js、Maven、Gradle 项目识别与任务启动
- 独立持久任务会话及后台任务重新进入/停止
- Web 项目 loopback SSH 隧道与浏览器预览
- SSH Config 别名、ProxyJump、IPv4/IPv6、密钥生成和公钥安装
- 远端环境诊断和有界终端输出搜索
- 后台终端交互提醒，不在通知中展示终端正文

## 安装前必须了解

- 包名保留为 `com.termux` 以兼容官方 bootstrap 和软件仓库路径。
- 本项目使用独立签名，无法覆盖其他签名来源的 Termux。安装前请备份原 Termux 数据，
  并卸载冲突应用；卸载会删除其应用私有 Linux 环境。
- 首次启动只有最小 Shell，需要点击“安装 SSH 连接组件”安装官方 OpenSSH 软件包。
- 第一次连接服务器时仍需在终端中人工核对并确认真实主机指纹。
- 密码、私钥和 Token 不保存在工作区配置中；它们继续由 OpenSSH 或远端环境管理。

## 当前验证状态

- 26 项 JVM/Robolectric 测试通过。
- 9 个自定义页面在生产主题和简体中文资源下完成运行时布局膨胀测试。
- Release Lint、R8、ARM64 构建、包名/版本/Launcher/ABI 闸门通过。
- APK Signature v1/v2 和项目发布证书指纹验证通过。
- 当前尚未完成三星 Android 真机上的安装、软键盘、SSH、Claude Code、Codex CLI、
  锁屏恢复及网络切换回归，因此版本标记为测试版。

完整真机步骤见 `DEVICE_ACCEPTANCE.md`。
