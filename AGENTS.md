# TermuxPro 智能体协作规则

涉及本项目的产品、架构、开发、测试、发布或回归任务，先读取并遵循
`.agents/skills/termuxpro-development/SKILL.md`。以代码、测试结果和设备证据为准，不以旧对话或计划代替
当前事实。

默认使用简体中文沟通和编写项目文档。不得参考 NewTermux 或其他 Termux 衍生项目；唯一代码上游是
官方 `termux/termux-app`。任何外部推送、正式发布、删除数据或密钥操作都必须保持在用户授权范围内。

业务分支名统一为 `dev_<englishCamelCase>_<YYYYMMDD>`，紧急修复分支名为
`hotfix_<englishCamelCase>_<YYYYMMDD>` 或使用关联 bug ID。业务部分只使用英语小驼峰，不使用中文。

每次维护从同步 `origin/master` 和 `origin/dev` 开始。普通变更经功能分支 PR 合入 `dev`；候选标签
`vX.Y.Z-rc.N` 完成真机回归后，经 `dev → master` 发布 PR 合并，最后在 `master` 创建稳定标签
`vX.Y.Z`。稳定 Release 成功后将 `dev` 快进到 `master` 并确认收尾 CI 通过。Actions Artifact 只是
临时构建产物，不能替代 GitHub Release；不得移动或覆盖已发布标签。
