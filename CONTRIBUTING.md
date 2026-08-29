# 参与 TermuxPro 开发

1. 从 `dev` 创建功能分支；一个分支只解决一个明确需求。功能分支使用
   `dev_<englishCamelCase>_<YYYYMMDD>`，紧急修复使用 `hotfix_<englishCamelCase>_<YYYYMMDD>` 或关联
   bug ID；业务名必须是英语小驼峰，例如 `dev_fixRemoteConnection_20260829`。
2. 提交使用 Conventional Commits：`<type>(<scope>): <中文描述>`。
3. 变更必须包含与风险相称的测试，并运行 `./scripts/test-all.sh` 与相关构建。
4. UI 变更附截图；SSH、签名、路径、授权和后台行为变更附安全说明。
5. PR 目标为 `dev`。只有版本候选在三星真机通过 P0 清单后才能从 `dev` 合入 `master`。

禁止提交密钥、密码、Token、真实主机、公司代码、未脱敏日志、Release 私钥或构建缓存。发现安全问题
请遵循 [SECURITY.md](SECURITY.md)，不要创建公开漏洞 Issue。
