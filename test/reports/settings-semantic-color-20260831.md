# 设置页视觉语义修复验收记录

## 用户问题

设置页面顶部使用红色时，会让用户误判当前页面存在危险、错误或破坏性操作。设置页属于普通配置入口，
不应使用危险语义色。

## 根因

`SettingsActivity` 在 Manifest 中仍使用 `Theme.TermuxApp.DayNight.NoActionBar`。该上游主题来自
`termux-shared`，其 `colorPrimary/colorPrimaryVariant` 仍是红色体系；共享 toolbar 使用
`?attr/colorPrimaryDark`，因此设置页顶部会显示红色。

## 本轮修复

- 将 `SettingsActivity` 主题改为 `Theme.TermuxPro.DayNight.NoActionBar`。
- 保留设置页原有内容和导航，不改变 Termux 设置功能。
- 新增 Manifest 回归测试，防止设置页再次退回上游红色主题。

## 验收标准

1. 设置页不再继承上游红色主色。
2. 危险色仍只保留给破坏性动作或明确错误状态。
3. 设置页功能入口不变。
4. 新增测试覆盖主题声明。

## 当前证据

- 聚焦 JVM/Robolectric：
  `TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests 'com.termux.app.ManifestProductIdentityTest' --tests 'com.termux.app.CustomLayoutsSmokeTest'`
  通过，77 个任务执行或复用缓存。
- 本地全量低资源验证：
  `TERMUXPRO_USE_CHINA_MIRROR=1 ./scripts/test-all.sh && git diff --check`
  通过，169 个 Gradle 任务执行或复用缓存；Skill 校验、workflow 策略校验、签名 APK 覆盖升级冒烟脚本均通过。

## 待补证据

- GitHub CI 与 Emulator UI
- dev 合并后 CI
