# AI CLI 启动目标上下文验收记录（2026-09-01）

## 背景

用户核心场景是通过 SSH 进入远端服务器后使用 Claude Code 与 Codex CLI。此前 AI CLI 启动弹窗只提供
“新建会话 / 选择历史会话”两个动作，缺少即将进入的远端目标预览。在共享 Claude 账号或多个远端工作区
场景下，用户容易误进错误项目或错误历史会话。

## 改动

- 工作区 AI CLI 启动弹窗增加当前目标说明：`服务器:端口 · 项目路径`。
- 配置不完整时提示“当前目标未完整配置”，避免用户误以为会进入可用远端上下文。
- Claude Code 与 Codex CLI 继续展示不同风险提示：
  - Claude Code：强调共享远程账号下默认新建会话更安全。
  - Codex CLI：强调用户级隔离但仍需确认当前项目。
- 不修改 Termux 原始终端、PTY、包管理、本地 shell 或基础会话能力。

## 验收标准

- 启动 Claude Code 时，配置不完整必须展示不完整提示和共享账号风险提示。
- 启动 Codex CLI 时，已配置工作区必须展示目标服务器、端口、项目路径和 Codex 项目确认提示。
- 选项仍必须是显式“新建会话”和“选择历史会话”，TermuxPro 不自动进入最近 AI 历史。
- 原始终端能力不受影响，本切片只作用于 TermuxPro AI CLI 增值入口。

## 验证

- `git diff --check`：通过。
- `./scripts/validate-skills.sh`：通过。
- `:app:testDebugUnitTest --tests com.termux.app.AiCliLaunchCommandTest --tests com.termux.app.WorkspaceActivitySmokeTest`：
  首次发现 Robolectric 在带正文的系统列表弹窗中不稳定挂载 `ListView` 首行 View，已改为验证弹窗正文、
  动作文案和按钮样式，不把测试绑定到系统弹窗内部临时子 View；修正后通过。
- `./scripts/pre-push-smoke.sh origin/dev`：通过。资源守卫识别当前为远程/共享环境，Gradle 使用
  `--max-workers=2`；KVM 不可用，未启动本地模拟器。
