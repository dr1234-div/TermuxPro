# Git 工作台目标上下文验收记录（2026-09-01）

## 背景

TermuxPro 的 Git 能力属于远程开发增值服务，不是重做 Termux 原始终端。用户可能同时维护多台服务器和
多个项目；如果 Git 工作台只展示当前分支和项目路径，用户仍需要靠记忆判断这个状态来自哪台远端机器。

## 改动

- Git 工作台首屏目标信息从单独项目路径升级为 `host:port · path`。
- 该信息来自工作区传入的 SSH 目标，不从终端当前 shell 猜测，避免污染原始终端。
- 不新增 Git 原始命令能力，不触碰 Termux PTY、滚动、包管理、本地会话等基础能力。

## 验收标准

- Git 工作台概览必须展示远端主机、SSH 端口和项目路径。
- 多服务器场景下，用户在进入分支、修改、提交操作前能确认目标机器。
- 既有分支、index、fetch/pull/push、提交等操作状态不受影响。

## 验证

- `git diff --check`：通过。
- `./scripts/validate-skills.sh`：通过。
- `:app:testDebugUnitTest --tests com.termux.app.GitDiffActivityTest`：通过。
- `./scripts/pre-push-smoke.sh origin/dev`：通过。资源守卫识别当前为远程/共享环境，Gradle 使用
  `--max-workers=2`；KVM 不可用，未启动本地模拟器。
