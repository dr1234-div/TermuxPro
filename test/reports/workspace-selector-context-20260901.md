# 工作区选择器上下文优化验收记录

## 用户问题

用户反馈远程开发配置和多工作区场景需要更符合真实业务：不能让用户凭一个抽象名称猜当前会连到哪台
服务器、哪个目录。多工作区切换如果只显示“远程开发”这类名称，容易误连或污染错误项目上下文。

## 产品决策

- 单工作区时继续隐藏选择器，避免制造只有一个选项的假交互。
- 多工作区时选择器显示两行信息：工作区名称、服务器与项目目录。
- 未配置工作区显示“未配置服务器”，避免空白选项被误认为可连接。
- 不改变工作区存储结构、SSH 命令、tmux 策略和连接认证行为。

## 验收标准

1. 复制或新增多个工作区后，选择器可见。
2. 选择器当前项和下拉项都能看到工作区名称、host 和 path。
3. `WorkspaceProfile.toString()` 仍保持纯名称，避免影响现有选择、测试和日志语义。
4. 单工作区时选择器仍隐藏。
5. 360dp、深色主题和 200% 字体由既有 Emulator UI 门禁继续覆盖。

## 本轮验证

- `WorkspaceActivitySmokeTest.copyWorkspaceKeepsEditedConnectionMetadataAndPersistsIt` 增加选择器显示断言。
- 本地已通过：`./scripts/validate-skills.sh`、`git diff --check`、`./test/version-metadata-test.sh`。
- 共享服务器本地 `./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest --tests
  com.termux.app.WorkspaceActivitySmokeTest` 在 240 秒内无有效输出并被 `timeout` 停止；不在远程共享环境
  继续消耗资源，完整编译、Robolectric 与模拟器证据交由 GitHub CI / Emulator UI 门禁验证。
