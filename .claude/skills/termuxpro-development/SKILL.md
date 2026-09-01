---
name: termuxpro-development
description: 迭代、评审、测试或发布 TermuxPro Android 移动 AI 终端时使用。
---

# TermuxPro 开发工作流

本文件是 Claude Code 入口。开始工作前完整读取仓库根目录
`.agents/skills/termuxpro-development/SKILL.md`，其内容是 Codex 与 Claude Code 共同遵循的规范源。
若无法读取该文件则停止发布类操作，并报告项目检出不完整。

关键边界：Termux 原始终端、PTY、包管理、本地 shell、基础会话和基础快捷键只作为兼容守护与回归面，
不得作为 TermuxPro 主线研发或竞品研究目标。迭代资源优先投入远程工作区、AI CLI、tmux/Git 可视化、
自定义快捷指令、移动端信息架构、交互效率和低频工具箱等增值服务；任何增值入口失败时必须安全回退，
不得污染或破坏原始 Termux 能力。

执行门禁：每个新切片必须先判断是否属于 TermuxPro 增值服务。若只是原本 Termux 已有功能，只能做
“不受影响”的兼容回归，不做重复研究、重复设计或重复实现。
