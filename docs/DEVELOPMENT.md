# TermuxPro 开发指南

## 环境要求

开发机需要 Git、64 位 JDK 17、Android SDK Platform 36、Build Tools 35.0.0、Platform Tools，
以及 NDK `29.0.14206865`。推荐 Linux；macOS 可用，Windows 推荐 WSL2 配合 Android Studio SDK。
项目使用 Gradle Wrapper 9.2.1，构建过程最多使用 2 个 Worker，默认 JVM 上限 2 GB。

## 首次准备

```bash
git clone git@github.com:dr1234-div/TermuxPro.git
cd TermuxPro
git switch dev
export ANDROID_SDK_ROOT=/path/to/android-sdk
./scripts/bootstrap-dev-env.sh
./scripts/doctor.sh
```

`bootstrap-dev-env.sh` 使用 SDK 自带的 `sdkmanager` 补齐固定版本组件并生成未跟踪的
`local.properties`。它不会安装系统级 JDK，也不会写入 shell 配置。若尚无 Android SDK Command-line
Tools，请先通过 Android Studio SDK Manager 安装。首次依赖解析需要联网；之后可向 Gradle 传
`--offline`。首次安装 SDK 组件时需要由开发者本人阅读并接受 Android SDK 许可证：

```bash
sdkmanager --licenses
```

无法稳定访问 Google Maven 的中国网络环境可显式启用腾讯 Maven 代理优先级：

```bash
export TERMUXPRO_USE_CHINA_MIRROR=1
```

GitHub Actions 和可直连环境默认优先使用 `google()` 与 Maven Central。

## 常用命令

项目 Skill 使用仓库内一键校验入口。首次运行会在被忽略的
`.tooling/skill-validator-venv` 创建项目级虚拟环境，并按 `requirements-tools.txt` 安装锁定版 PyYAML；
它不依赖系统 Python 包，也不会修改全局环境：

```bash
./scripts/validate-skills.sh
```

GitHub API 操作使用 `./scripts/github-cli.sh <gh 参数>`；该入口会自动选择系统 `gh` 或仓库内
`.tooling/gh/bin/gh`，不依赖交互 shell 的 `PATH`。

所有仓库构建入口会 source `scripts/resolve-jdk17.sh`，优先选择项目内同时包含 `java`/`javac` 的完整
JDK 17。执行临时 Gradle 子任务时也先 source 该脚本，不要手工把系统 JRE 目录设为 `JAVA_HOME`：

```bash
source scripts/resolve-jdk17.sh
./gradlew --no-daemon --max-workers=2 <task>
```

```bash
./scripts/test-all.sh
./gradlew --no-daemon --max-workers=2 :app:assembleDebug
./gradlew --no-daemon --max-workers=2 lint
./scripts/build-termuxpro-release.sh
./scripts/device-smoke-test.sh
```

Debug 构建和测试不依赖 Release 私钥。正式构建需要按 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)
准备本机签名材料。禁止把 `local.properties`、SDK、Gradle 缓存、SSH 配置、AI Token、私钥或密码提交。

## Git 远程

```bash
git remote -v
# origin   dr1234-div/TermuxPro（拉取与推送）
# upstream termux/termux-app（仅拉取）
```

业务开发以 `dev` 为集成分支；`master` 只接收已通过发布门禁的版本。同步官方更新前阅读
[UPSTREAM_SYNC.md](UPSTREAM_SYNC.md)。
