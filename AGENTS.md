# TermuxPro 智能体协作规则

涉及本项目的产品、架构、开发、测试、发布或回归任务，先读取并遵循
`.agents/skills/termuxpro-development/SKILL.md`。以代码、测试结果和设备证据为准，不以旧对话或计划代替
当前事实。

默认使用简体中文沟通和编写项目文档。不得参考 NewTermux 或其他 Termux 衍生项目；唯一代码上游是
官方 `termux/termux-app`。任何外部推送、正式发布、删除数据或密钥操作都必须保持在用户授权范围内。

业务分支名统一为 `dev_<englishCamelCase>_<YYYYMMDD>`，紧急修复分支名为
`hotfix_<englishCamelCase>_<YYYYMMDD>` 或使用关联 bug ID。业务部分只使用英语小驼峰，不使用中文。

首次 `dev → master` 发布合并前，两者根历史不同；功能分支只需同步 `origin/dev`，不得对 `master`
执行 rebase。首次发布合并完成后，再恢复同时同步稳定分支和研发分支的常规流程。
