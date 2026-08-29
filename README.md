# TermuxPro

TermuxPro 是面向 Android 手机的移动 AI 开发终端。它以官方
[Termux](https://github.com/termux/termux-app) 为唯一上游，在保留本地 Linux/PTY 能力的同时，
把 SSH 远程项目、Claude Code、Codex CLI、tmux、Git、文件和开发任务组织成适合触屏的工作区。

> TermuxPro 是独立社区衍生项目，并非 Termux 官方产品，也未获得 Termux 项目背书。
> 当前为了兼容官方 bootstrap 和软件包路径保留 `com.termux` 包名，因此不能与不同签名来源的
> Termux 或其插件同时安装。切换前请先备份数据。

## 当前版本

`0.1.0` 是 ARM64 测试版，面向 Android 7 及以上设备，优先验证 Android 14–16。第一阶段聚焦：

- 中文工作区与多 SSH 项目配置
- Claude Code `--continue` 和 Codex CLI `resume --last` 快速恢复
- 持久化 tmux 会话、后台任务和二次确认停止
- Shell、AI、Vim 软键盘快捷键与 AI 确认/拒绝/中断安全操作
- 远端只读文件浏览、文件预览、Git Diff 和项目任务识别
- SSH 连接诊断、公钥生成/复制/安装以及安全目标校验
- Web 开发端口通过仅绑定本机回环地址的 SSH 隧道预览
- 最近 1 MB 终端输出搜索，最多返回 100 条且不落盘
- 完整本地 Termux Linux 环境

远端文件写入、Mosh、增强远端代理以及应用内模型调用尚未作为 `0.1.0` 稳定能力承诺。

## 安装与使用

发布产物为 `termuxpro-<版本>-arm64-v8a.apk`。安装前请备份现有 Termux 数据，并卸载其他
签名来源的 Termux 及插件，否则 Android 会拒绝安装。校验 APK 的 SHA-256 和签名证书指纹后再侧载。

首次启动点击“安装 SSH 连接组件”，等待 bootstrap 与 OpenSSH 安装完成。创建工作区时填写 SSH
主机/IP/`user@host`/`~/.ssh/config` 别名和远端项目目录。建议远端提前安装 OpenSSH、tmux、Git、
Claude Code 和 Codex CLI。工作区内的 Claude、Codex、Git、文件、任务与诊断入口复用同一 SSH 配置。
应用不保存 SSH 密码，不读取私钥，也不会自动确认 AI CLI 的危险授权。

## 开发环境

- Linux 或 macOS（Windows 建议 WSL2）
- Git 2.40+、JDK 17
- Android SDK Platform 36、Build Tools 35.0.0
- Android NDK `29.0.14206865`
- ARM64 Android 真机用于最终验收

Gradle Wrapper 已提交，无需单独安装 Gradle：

```bash
git clone git@github.com:dr1234-div/TermuxPro.git
cd TermuxPro
git switch dev
export ANDROID_SDK_ROOT=/path/to/android-sdk
# 中国网络环境按需执行：export TERMUXPRO_USE_CHINA_MIRROR=1
./scripts/bootstrap-dev-env.sh
./scripts/doctor.sh
./scripts/test-all.sh
```

首次构建需要联网下载 Gradle 和 Maven 依赖；依赖齐全后可传 `--offline`。详细说明见
[开发指南](docs/DEVELOPMENT.md)。

## 构建与验收

```bash
./scripts/test-all.sh                         # 全模块 JVM 测试
./gradlew --no-daemon --max-workers=2 :app:assembleDebug
./scripts/build-termuxpro-release.sh          # 需要本机 Release 签名材料
./scripts/device-smoke-test.sh                # 单台已授权真机覆盖安装冒烟
```

Android 标准测试位于各模块 `src/test` 和 `src/androidTest`；根目录 [test](test/README.md)
提供跨模块矩阵、发布门禁和真机验收模板。自动化测试不能替代真实 SSH、软键盘、后台和 AI CLI 回归。

## 分支与交付流程

- `dev`：日常产品开发和集成分支。
- 功能分支从 `dev` 创建，使用 `dev_<englishCamelCase>_<YYYYMMDD>` 命名，完成后通过 PR 合回
  `dev`；紧急修复使用 `hotfix_<englishCamelCase>_<YYYYMMDD>` 或关联 bug ID。
- `master`：只接收通过自动化、Release 校验和三星真机 P0 验收的发布候选。
- 官方 Termux 更新通过只读 `upstream` 同步，不向上游推送 TermuxPro 代码。

完整规范见 [贡献指南](CONTRIBUTING.md)、[架构说明](docs/ARCHITECTURE.md)和
[发布与签名](docs/RELEASE_SIGNING.md)。Codex 与 Claude Code 应加载项目中的
`termuxpro-development` Skill，按产品 → 架构 → 开发 → 测试 → 验收 → 回归闭环执行。

## 安全与隐私

- 工作区不存储 SSH 密码、AI Token 或私钥内容。
- 发布私钥和密码永远不进入 Git；仓库仅保存公开证书、指纹和配置模板。
- SSH 目标拒绝控制字符、空白和选项注入；AI CLI 的确认操作必须由用户二次确认。
- 安全问题请按 [SECURITY.md](SECURITY.md) 私下报告，公开 Issue 不要粘贴密钥或公司日志。

## 仓库结构

```text
app/                  Android 主应用及 TermuxPro 业务测试
terminal-emulator/    终端模拟器
terminal-view/        终端视图
termux-shared/        Termux 共享能力
docs/                 架构、开发、签名和验收文档
scripts/              环境、测试、构建和设备脚本
test/                 跨模块测试计划、用例与验收记录
.agents/skills/       Codex 项目 Skill（规范源）
.claude/skills/       Claude Code Skill 入口
```

## 许可证与上游

本项目依据 [GNU GPL v3](LICENSE.md) 发布，并保留原 Termux 项目的版权和许可证声明。TermuxPro
的修改代码同样按 GPLv3 提供。`Termux` 名称及相关标识属于其各自权利人；本项目使用
`TermuxPro` 名称用于区分社区衍生版本。

- 上游源码：https://github.com/termux/termux-app
- 上游软件包：https://github.com/termux/termux-packages
- 本项目问题：https://github.com/dr1234-div/TermuxPro/issues
