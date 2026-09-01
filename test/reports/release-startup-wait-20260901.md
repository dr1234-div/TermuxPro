# Release 启动等待假阴性修复记录

## 问题

`v0.9.2` 稳定 Release workflow run `33538417112` 在“验收待发布签名 APK 的覆盖升级”步骤失败：

- `0.9.1` 基线 APK 安装成功。
- `0.9.2` 候选 APK 覆盖安装成功。
- 包版本与 UID 校验通过。
- 脚本启动后未检测到 `com.termux` 进程，退出失败。

## 证据

失败 artifact 证明应用实际上已启动：

- `logcat.txt` 记录 `ActivityTaskManager START ... cmp=com.termux/.app.WorkspaceActivity`。
- `logcat.txt` 记录 `ActivityManager: Start proc ... com.termux ... WorkspaceActivity`。
- `window.xml` 记录 `package="com.termux"`、`com.termux:id/workspace_scroll_view` 和首页内容。
- 未发现 `AndroidRuntime FATAL EXCEPTION`。

## 根因

验收脚本用 `monkey` 触发 Launcher 后，立即循环调用 `pidof com.termux`。在 GitHub 模拟器启动/安装后的
短暂窗口内，Activity 已进入启动流程，但进程查询可能尚未稳定返回，导致假阴性。失败输出没有包含前台
Activity、等待过程和 pidof 证据，排障成本偏高。

## 修复

- 改用 `am start -W -n com.termux/com.termux.app.WorkspaceActivity` 显式启动 Launcher Activity。
- 最多等待 45 秒检测进程，并记录每次等待的 pid 与前台 Activity。
- 失败 artifact 新增 `launch-candidate.txt`、`process-wait.txt`、`process-failure.txt`、`activity.txt`
  和 `pidof.txt`。
- 回归测试新增延迟 pid 场景，确认前两次 `pidof` 为空、第三次返回时脚本可通过。

## 发布处理

`v0.9.2` 标签已推送，按稳定发布规则不移动、不覆盖、不删除。修复脚本后递增为 `0.9.3` 重新发布稳定版。
