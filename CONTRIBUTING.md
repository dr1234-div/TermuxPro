# 参与 TermuxPro 开发

1. 从 `dev` 创建功能分支；一个分支只解决一个明确需求。功能分支使用
   `dev_<englishCamelCase>_<YYYYMMDD>`，紧急修复使用 `hotfix_<englishCamelCase>_<YYYYMMDD>` 或关联
   bug ID；业务名必须是英语小驼峰，例如 `dev_fixRemoteConnection_20260829`。
2. 提交使用 Conventional Commits：`<type>(<scope>): <中文描述>`。
3. 变更必须包含与风险相称的测试，并运行 `./scripts/test-all.sh` 与相关构建。
4. UI 变更附截图；SSH、签名、路径、授权和后台行为变更附安全说明。
5. PR 目标为 `dev`。只有版本候选在三星真机通过 P0 清单后才能从 `dev` 合入 `master`。
6. 候选版本使用 `vX.Y.Z-rc.N` 并自动发布为 Pre-release；真机验收结论必须写入 `test/reports/`。
7. 稳定版通过 `dev → master` 发布 PR，合并后在 `master` 创建 `vX.Y.Z` 标签，由 Actions 创建正式
   Release；发布完成后将 `dev` 快进对齐 `master` 并确认 CI 全绿。

禁止提交密钥、密码、Token、真实主机、公司代码、未脱敏日志、Release 私钥或构建缓存。发现安全问题
请遵循 [SECURITY.md](SECURITY.md)，不要创建公开漏洞 Issue。

## 持续维护要求

- 每次迭代同时评估产品、UI/UX、架构、开发、测试、安全和发布影响，不把用户截图中的单点修复当作
  同类问题全部解决。
- UI 改动至少检查深浅主题、所有相关页面、360dp 窄屏、最大系统字体、48dp 触控目标和文本对比度。
- 在远程共享开发机运行全量构建、模拟器或压力测试前执行 `scripts/resource-guard.sh`，遵守输出的并发
  和资源限制；项目工具只能安装在 `.tooling/`。
