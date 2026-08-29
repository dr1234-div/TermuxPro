---
name: termuxpro-development
description: 迭代、评审、测试或发布 TermuxPro Android 移动 AI 终端时使用，统一产品、架构、开发、测试、验收和回归口径。
---

# TermuxPro 开发工作流

先以当前代码、`README.md`、`docs/ARCHITECTURE.md` 和 `test/` 为事实基线。唯一代码上游是官方
`termux/termux-app`；不得参考 NewTermux 或其他衍生项目。

## 角色闭环

按任务规模覆盖以下职责；可由一个智能体依次承担，也可在用户授权时并行委派：

1. 产品：定义用户问题、范围、交互和可量化验收标准。
2. 架构：确认与 Termux PTY/bootstrap、`com.termux`、Android 生命周期及安全不变量兼容。
3. 开发：从 `dev` 创建符合命名规范的功能分支，实现最小完整纵向闭环并补测试。
4. 测试：执行单元、布局、静态、APK 和适用的真机测试，记录失败证据。
5. 验收：逐项比对原始需求和发布门禁；未满足时回到产品或开发继续打磨。

不能用文档、Mock 或一次启动替代真实 SSH、软键盘、后台、网络切换和 AI CLI 真机证据。

## 不变量

- 产品名为 `TermuxPro`；保留 `com.termux` 只是官方 bootstrap/路径兼容策略。
- 不保存或记录 SSH 密码、私钥、AI Token、公司源码和未脱敏终端输出。
- 不提交 Release 私钥或密码。只提交公开证书、指纹、模板和验证工具。
- 不自动接受 SSH 主机密钥，不自动批准 Claude Code/Codex CLI 权限。
- 远端命令、目标和路径必须抗选项/命令注入；读取和搜索必须有大小限制且可取消。
- 不向 `upstream` 推送。日常开发合入 `dev`；只有完整发布门禁通过后才能合入 `master`。
- 分支业务名必须使用有意义的英语小驼峰：`dev_<englishCamelCase>_<YYYYMMDD>`；缺陷修复使用
  `hotfix_<englishCamelCase>_<YYYYMMDD>` 或关联 bug ID。禁止中文、空格和无意义编号。
- 保留 GPLv3、上游版权和衍生项目声明。

## 完成定义

功能只有在以下证据齐全时才算完成：

- 需求、异常路径、安全和回滚行为明确。
- 相关 JVM/Robolectric/仪器测试通过；UI 可在生产主题和中文资源下运行。
- `./scripts/test-all.sh`、Lint 和适用 APK 构建通过。
- 涉及设备行为时，完成 `test/` 对应真机用例并保存脱敏报告。
- README、架构、使用说明和发布说明随行为同步。
- 工作树不含秘密或构建产物，提交符合项目规范。

若任一门禁失败，修复后从受影响层开始重新执行测试，不降低验收标准来获得通过。

## 维护与发布闭环

普通迭代必须按以下状态推进，不得把 Actions Artifact 当作 GitHub Release，也不得跳过失败门禁：

1. 同步 `master`、`dev`，从 `dev` 创建英语小驼峰业务分支。
2. 完成功能、测试、文档和本地验证后提交推送，创建以 `dev` 为目标的 PR。
3. PR CI 全绿后合入 `dev`；需要设备能力时先发布 `vX.Y.Z-rc.N` 并完成真机回归。
4. 将真机结论、已知限制和候选 APK SHA-256 写入版本说明及 `test/reports/`，不得补造证据。
5. 创建 `dev → master` 发布 PR；发布门禁和 CI 全绿后才能合并。
6. 在 `master` 合并提交创建稳定标签 `vX.Y.Z`。标签流水线必须完成测试、Lint、R8、签名、APK
   内容校验和 GitHub Release 创建；只运行 `workflow_dispatch` 得到的是临时 Artifact，不算发布。
7. 核对稳定 Release 非 Draft/Pre-release、附件和 SHA-256 完整、默认分支为 `master`、无遗留 PR；
   再将 `dev` 快进到 `master` 发布提交，并等待收尾 CI 通过。

稳定版本不得覆盖或移动既有标签。发布失败时保留失败证据，修复工作流或代码后递增候选版本；未经用户
明确授权，不删除已公开 Release、标签或用户可下载产物。

## 按需参考

- 产品范围或架构决策：读 `docs/ARCHITECTURE.md`。
- 环境和构建故障：读 `docs/DEVELOPMENT.md`。
- 测试、真机或发布判断：读 `test/README.md` 和 `test/plans/release-gates.md`。
- 签名或发布：读 `docs/RELEASE_SIGNING.md`；操作私钥前再次确认权限。
- 同步官方 Termux：读 `docs/UPSTREAM_SYNC.md`。
