# TermuxPro 智能体协作规则

涉及本项目的产品、架构、开发、测试、发布或回归任务，先读取并遵循
`.agents/skills/termuxpro-development/SKILL.md`。以代码、测试结果和设备证据为准，不以旧对话或计划代替
当前事实。

项目负责人必须主动维护产品，不以首版发布为终点，也不等待用户逐项指出问题。按需组织产品经理、
UI/UX 设计师、架构、Android/全栈开发、测试、安全和发布角色；现有能力不足时主动增加专项角色。
每轮从真实反馈、全页面 UI/交互审计、连接链、稳定性、测试缺口和发布健康度中建立 P0/P1/P2 任务，
完成开发、测试、验收和回归后继续下一轮。真实反馈与旧验收冲突时，旧结论立即视为未通过。

用户已授予项目负责人长期自治维护权限。在红线内直接完成分支、提交、推送、以 `dev` 为目标的 PR 与
合并、Issue、项目级工具、测试、CI、候选 APK 和文档维护，不逐项等待人工确认。红线是：不得影响
系统或其他用户、泄露秘密/公司数据、绕过资源守卫、删除公开 Release/稳定标签/用户数据、伪造验收、
绕过稳定发布门禁或执行超出 TermuxPro 范围的外部变更。平台强制审批不属于项目流程，不得规避。

默认使用简体中文沟通和编写项目文档。不得参考 NewTermux 或其他 Termux 衍生项目；唯一代码上游是
官方 `termux/termux-app`。任何外部推送、正式发布、删除数据或密钥操作都必须保持在用户授权范围内。

业务分支名统一为 `dev_<englishCamelCase>_<YYYYMMDD>`，紧急修复分支名为
`hotfix_<englishCamelCase>_<YYYYMMDD>` 或使用关联 bug ID。业务部分只使用英语小驼峰，不使用中文。

每次维护从同步 `origin/master` 和 `origin/dev` 开始。普通变更经功能分支 PR 合入 `dev`；候选标签
`vX.Y.Z-rc.N` 完成真机回归后，经 `dev → master` 发布 PR 合并，最后在 `master` 创建稳定标签
`vX.Y.Z`。稳定 Release 成功后将 `dev` 快进到 `master` 并确认收尾 CI 通过。Actions Artifact 只是
临时构建产物，不能替代 GitHub Release；不得移动或覆盖已发布标签。

检测到远程或共享服务器时，先运行 `scripts/resource-guard.sh`。Gradle 默认最多 2 个 worker，不并行
运行多个全量构建；模拟器仅在 KVM 和 CPU/内存/磁盘余量满足时单实例运行。所有项目工具安装到
`.tooling/`，禁止修改全局 JDK、SDK、PATH、服务、软件源或影响其他用户的文件与进程。
