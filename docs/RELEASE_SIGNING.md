# Release 签名

正式 APK 使用项目独有的 PKCS#12 签名，不再使用上游公开测试证书。

- 密钥：`.signing/ai-terminal-release.p12`
- 密码：`.signing/ai-terminal-release.pass`
- 别名：`ai-terminal`
- 两个文件均被 Git 忽略，只允许保存在受控备份中。
- 丢失密钥后无法为已有安装提供可直接升级的 APK。

单命令构建并验证 ARM64 Release APK：

```bash
./scripts/build-ai-terminal-release.sh
```

脚本固定以离线模式和最多两个 Gradle Worker 执行全部单元测试、Release Lint/R8、构建及以下发布闸门：

- 包名 `com.termux`、版本 `0.1.0 (10000)`
- Launcher 为 `com.termux.app.WorkspaceActivity`
- APK 只包含 `arm64-v8a`
- APK Signature v1/v2 均有效
- 签名证书 SHA-256 与项目发布证书一致

验证通过后，交付目录为 `dist/0.1.0/`：

- `ai-terminal-0.1.0-arm64-v8a.apk`
- `SHA256SUMS`
- `APK_SIGNATURE.txt`
- `BUILD_INFO.txt`
- `RELEASE_NOTES.md`
- `DEVICE_ACCEPTANCE.md`

`dist/` 被 Git 忽略。发布时将整个版本目录交付，避免误发 Gradle 中间产物或旧 APK。
