# Release 设备验收空输出失败复盘

## 现象

`v0.9.0-rc.2` Release workflow 在“验收待发布签名 APK 的覆盖升级”步骤失败。日志显示：

- 候选 APK 构建成功。
- 稳定版基线 APK 构建成功。
- 稳定版安装和候选版覆盖安装均返回 `Success`。
- 失败点为 `scripts/verify-release-apk-on-device.sh: line 38: 1: parameter null or not set`。
- GitHub Release 创建步骤未执行，因此 `v0.9.0-rc.2` 没有公开发布。

## 根因

`verify-release-apk-on-device.sh` 的 `require_contains` 和 `require_regex` 使用 `${1:?}` 读取待检查文本。
当 `adb shell dumpsys package com.termux` 或其他设备命令返回空字符串时，脚本会触发 Bash 参数错误，
没有输出明确的业务失败原因。

这会降低发布门禁的诊断质量，并可能误导为 APK 构建问题。

## 修复

- 待检查文本改为 `${1-}`，允许空字符串进入业务校验。
- 业务校验继续失败关闭，但输出明确错误，例如“覆盖升级后设备上的 versionCode 不正确”。
- `test/release-apk-device-smoke-test.sh` 增加空 `dumpsys package` 回归，确认不会再暴露
  `parameter null or not set`。

## 发布处理

`v0.9.0-rc.2` 标签已触发失败的 Release workflow，不覆盖、不移动、不删除。修复合入 `dev` 后，递增到
`0.9.0-rc.3` 重新冻结候选版本。
