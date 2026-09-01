# 增值页深色弹窗可读性防线

## 背景

用户反馈 0.2.0 之后仍有部分页面存在黑体字或弹窗内容看不清的问题。该问题属于 TermuxPro 增值层的
移动端体验缺陷，不是原始 Termux 终端、PTY、包管理或基础会话能力的重做需求。

## 本轮范围

- 自定义快捷指令弹窗。
- tmux 会话中心弹窗。
- 工作区和终端内 AI CLI 启动弹窗。
- 终端提示词编辑器弹窗。
- Git 工作台普通、危险和带输入校验的弹窗展示入口。

## 非目标

- 不修改原始 Termux 终端的 PTY 行为。
- 不研究或重做 Termux 已有的基础终端、包管理、本地 shell、URL 识别或进程结束能力。
- 不改变原始终端回退路径；增值层失败时仍应回到基础终端可用状态。

## 代码策略

- `TermuxProDialogStyle.show()` 统一负责展示前套用深色背景、标题、正文、按钮和列表文字颜色。
- `TermuxProDialogStyle.prepare()` 支持 Git 工作台这类需要先创建 Dialog 供测试断言、再展示的场景，
  同时仍统一绑定产品样式和输入校验。
- `AiSessionDialog.show()` 统一 AI CLI 启动弹窗的大字体可读性与取消按钮品牌色。
- 列表弹窗会对当前列表行和后续新增行都重新套用深色文字，避免厂商主题或列表复用导致黑字回退。

## 门禁

- `test/dialog-readable-style-test.sh` 禁止 TermuxPro 增值页手写 `setOnShowListener`。
- 普通增值页禁止直接 `dialog.show()`，必须走 `TermuxProDialogStyle.show()` 或专用包装。
- Git 工作台只允许 `showPreparedDialog()` 展示已经通过 `TermuxProDialogStyle.prepare()` 绑定样式和校验的 Dialog。

## 已执行验证

```text
./test/dialog-readable-style-test.sh
git diff --check
TERMUXPRO_USE_CHINA_MIRROR=1 ./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest \
  --tests 'com.termux.app.GitDiffActivityTest' \
  --tests 'com.termux.app.TaskSessionsActivityTest' \
  --tests 'com.termux.app.CustomCommandsActivityTest'
```

结果：

- 弹窗可读样式静态校验通过。
- Git/ tmux/快捷指令相关 Robolectric 单元测试通过。
- Gradle 构建成功，77 个任务中 15 个执行、62 个命中缓存。

## 后续验收

- 下一次候选版发布前继续跑默认字体和 200% 字体模拟器截图。
- 若用户在真机上再次发现黑字，需要优先归类到“增值页弹窗样式防线”并补相应页面的静态或 Robolectric 回归。
