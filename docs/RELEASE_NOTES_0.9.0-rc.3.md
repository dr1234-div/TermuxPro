# TermuxPro 0.9.0-rc.3 候选版发布说明

`0.9.0-rc.3` 是 `0.9.0-rc.2` 发布链路失败后的候选修复版，不扩大 0.9.0 发布列车范围。

## 核心变化

- 修复 Release 设备验收脚本在 ADB 设备命令返回空内容时直接触发 Bash `parameter null or not set`
  的问题。后续会输出明确的覆盖升级诊断错误，便于定位真实设备状态。
- 增加发布设备冒烟回归：模拟 `dumpsys package com.termux` 空输出时，验收脚本必须给出业务错误，
  不能因脚本自身参数展开失败而中断。
- 保留 `0.9.0-rc.2` 已冻结的增值体验范围：候选发布等待兜底、Claude Code 共享账号风险提示、
  Codex CLI 当前项目确认提示。
- 继续遵守产品边界：Termux 原本已有的终端、PTY、包管理、本地 shell、基础会话和基础快捷键能力
  只做“不受影响”回归守护，不作为 TermuxPro 主线研究或重造方向。

## 验收重点

1. Release workflow 能完成签名 APK 构建、覆盖升级、运行时冒烟和 GitHub Pre-release 创建。
2. 设备验收脚本遇到空设备输出时，能返回明确诊断，不再出现 `parameter null or not set`。
3. Claude Code/Codex CLI 启动提示与新建/历史会话策略保持 `0.9.0-rc.2` 冻结范围。
4. 原始 Termux 终端能力不受本候选版发布脚本修复影响。

## 已知限制

- `v0.9.0-rc.2` 标签已保留为失败证据，不覆盖、不删除、不移动。
- 本地共享服务器不启动 Android 模拟器；运行时证据来自 GitHub Actions 隔离模拟器和 Release workflow。

## 安装提醒

- 包名仍为 `com.termux`，与其他签名来源的 Termux 不能直接覆盖安装。
- 升级前请备份重要本地 Linux 环境和配置；卸载应用会删除应用私有数据。
- 如果已安装 `0.8.1` 正式版或 `0.9.0-rc.1` 候选版，可直接覆盖升级到本候选版本。
