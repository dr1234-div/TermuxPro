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
v1/v2 签名。设备接入后运行 `./scripts/device-smoke-test.sh`，再逐项完成
[真机验收清单](../docs/DEVICE_ACCEPTANCE.md)。

## 证据

使用 [验收报告模板](reports/ACCEPTANCE_TEMPLATE.md) 记录设备、Android 版本、APK SHA-256、步骤、结果、
缺陷和复测。截图、录屏和 logcat 必须脱敏；大体积原始证据不得提交 Git。
