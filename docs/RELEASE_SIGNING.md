# TermuxPro 发布签名

正式 APK 使用 TermuxPro 独有的 PKCS#12 私钥。公开仓库只提交
[`certs/termuxpro-release.pem`](../certs/termuxpro-release.pem) 公钥证书；`.signing/`、私钥和密码始终
被 Git 忽略。任何开发者都可无签名秘密地完成 Debug 构建和测试。

## 当前发布身份

- 别名：`termuxpro`
- 算法：RSA 4096 / SHA-256
- 证书主体：`CN=TermuxPro Release, OU=Mobile Development, O=TermuxPro`
- SHA-256：`10:C8:20:D3:92:D5:BD:96:72:31:70:67:A5:D1:1F:C1:25:02:17:82:41:66:96:1D:F7:B3:85:12:8D:4B:49:CD`
- 私钥：`.signing/termuxpro-release.p12`（不提交）
- 密码：`.signing/termuxpro-release.pass`（不提交）

私钥必须至少有两份加密离线备份。丢失私钥后无法为已安装版本提供覆盖升级；泄露时必须停止发布、
公告轮换并评估 Android 签名迁移方案。GitHub Actions 只可通过仓库 Actions Secrets 临时恢复
私钥，PR 和 Fork 工作流不得接触签名秘密。

上游随源码公开的 `app/testkey_untrusted.jks` 仅用于 Debug 兼容测试，任何人都能获得，绝不能用于
TermuxPro Release。CI 对它设置唯一白名单，其他密钥或密码文件一律拒绝。

## 为独立开发环境生成测试用 Release 密钥

下面生成的是该开发者自己的密钥，不能发布为官方 TermuxPro 升级包：

```bash
mkdir -p .signing
openssl rand -base64 48 > .signing/termuxpro-release.pass
chmod 600 .signing/termuxpro-release.pass
keytool -genkeypair -alias termuxpro -keyalg RSA -keysize 4096 \
  -sigalg SHA256withRSA -validity 9125 \
  -dname "CN=TermuxPro Local Release, O=Local Development" \
  -storetype PKCS12 -keystore .signing/termuxpro-release.p12 \
  -storepass:file .signing/termuxpro-release.pass \
  -keypass:file .signing/termuxpro-release.pass
```

正式发布脚本固定校验项目公开证书指纹，因此本地自签名只适合验证 Gradle Release 配置。要构建正式
候选，必须从受控备份恢复匹配本仓库公开证书的私钥。

## 构建

```bash
./scripts/build-termuxpro-release.sh
# 缓存完整且明确需要离线时：
TERMUXPRO_OFFLINE=1 ./scripts/build-termuxpro-release.sh
```

脚本运行全模块测试、Lint、Release/R8，并验证包名、版本、Launcher、ARM64 ABI、v1/v2 签名和证书。
产物输出到 `dist/0.1.0/termuxpro-0.1.0-arm64-v8a.apk`，同时生成 SHA-256、签名报告、构建信息、
发布说明和真机清单。`dist/` 不提交 Git。

## GitHub Actions 自动正式打包

在仓库 `Settings → Secrets and variables → Actions` 创建两个 Repository secrets：

- `TERMUXPRO_RELEASE_KEYSTORE_BASE64`：`.signing/termuxpro-release.p12` 的单行 Base64。
- `TERMUXPRO_RELEASE_KEYSTORE_PASSWORD`：`.signing/termuxpro-release.pass` 的完整内容。

在持有私钥的安全机器上生成第一个值：

```bash
base64 -w 0 .signing/termuxpro-release.p12
```

配置后可在 `Actions → TermuxPro Release → Run workflow` 手动生成正式 APK。推送版本标签会在完成
测试、Lint、签名和 APK 校验后自动创建 GitHub Release，例如：

```bash
git tag -a v0.1.0 -m "TermuxPro 0.1.0"
git push origin v0.1.0
```

不要把 Base64 内容或密码写入仓库、Issue、PR、Actions 日志。GitHub Secrets 不能作为唯一备份，原始
PKCS12 私钥仍需保存于密码管理器、加密介质或离线备份中。
