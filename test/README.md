# TermuxPro 测试入口

Android 测试代码继续放在 Gradle 标准目录（各模块的 `src/test` 与 `src/androidTest`）。本目录保存跨模块
测试计划、发布门禁、真机用例和脱敏验收记录，不重复测试源码。

## 自动化

```bash
./scripts/doctor.sh
./scripts/test-all.sh
./gradlew --no-daemon --max-workers=2 lint
./gradlew --no-daemon --max-workers=2 :app:assembleDebug
```

Release 候选还必须运行 `./scripts/build-termuxpro-release.sh`，验证版本、Launcher、ARM64 ABI、证书及
v1/v2 签名。候选 Release 发布后，手动运行 `TermuxPro Emulator UI`，它会直接下载稳定版与当前候选的
正式签名 APK，验证覆盖升级、UID 保持、版本、应用名、Launcher、进程和崩溃日志；不能用重新构建的
Debug APK 代替这项证据。设备接入后运行 `./scripts/device-smoke-test.sh`，再逐项完成
[真机验收清单](../docs/DEVICE_ACCEPTANCE.md)。

`./test/terminal-touch-scrollback-test.sh` 是终端核心体验门禁：手机手指上下滑动必须滚动历史输出，
不得在 alternate screen、Claude Code、Codex CLI 或 shell 输入状态中退化为 `DPAD_UP/DPAD_DOWN`
命令历史；鼠标滚轮和显式键盘翻页保留各自语义。

## 证据

使用 [验收报告模板](reports/ACCEPTANCE_TEMPLATE.md) 记录设备、Android 版本、APK SHA-256、步骤、结果、
缺陷和复测。截图、录屏和 logcat 必须脱敏；大体积原始证据不得提交 Git。
