# GitHub Actions 失败提醒复盘

## 结论

- 最近 100 条 Actions 记录中，核心 `TermuxPro CI`、`TermuxPro Emulator UI`、候选发布链路存在多次成功记录，不是“全部失败”。
- 用户收到失败邮件的主要来源是历史工作流设计缺陷：
  1. 预合并 CI 真实失败时，旧版 `auto-dev-pr` 也以失败退出，导致同一问题产生重复失败提醒。
  2. `dev_release*` 旧匹配过宽，证据分支可能误触发候选发布检查，遇到已有 tag 后失败。
  3. 自动 PR 脚本缺少 GitHub CLI 命令级超时与重试，网络或 API 抖动时可能长时间 pending，最终变成不可解释失败。

## 影响

- 不影响已经通过 CI、模拟器验收和 Release 验收的 APK 内容本身。
- 会影响仓库健康状态、邮件通知噪声和用户对自治迭代稳定性的判断。
- 如果自动 PR 在 merge、tag 或 release 触发阶段失败，会影响对应分支自动进入 `dev` 或自动发布候选包。

## 已采取治理

- 预合并 CI / 模拟器失败时，自动 PR 保留 PR 并成功退出，避免和核心 CI 重复发失败提醒。
- 候选发布分支改为精确匹配 `dev_release数字Rc数字_YYYYMMDD`，避免证据/文档分支误触发候选 Release。
- `auto-dev-pr.yml` 增加 `GH_COMMAND_TIMEOUT_SECONDS`、`gh_safe` 和 `gh_retry`：
  - 单次 GitHub CLI 调用超时 45 秒。
  - 关键写操作最多重试 3 次。
  - 等待 CI/Release 时允许短暂查询失败继续轮询，避免把 GitHub API 抖动误判成项目失败。

## 后续观察项

- 每轮合并后检查最近失败 run 是否由真实代码缺陷、GitHub 外部集成还是工作流噪声导致。
- 如果仍出现 `pull_request action_required` 且无 jobs，优先区分 GitHub Actions 与外部 App check suite，不能直接当作项目 CI 失败。

## 2026-09-01 复核

用户反馈邮件中 PR 和 CLI 操作持续出现 fail 后，重新查询最近 100 条 Actions：

- 最近窗口内没有新的 `TermuxPro CI` 或 `TermuxPro Emulator UI` 真实失败。
- 真实 `failure` 只来自 `v0.9.0-rc.2` 发布链路：
  - Release run `33501845633`：发布设备验收脚本空输出路径失败。
  - 自动 PR run `33501017144`：同一候选发布链路失败。
  - 该问题已在后续 rc.3 与 `v0.9.0` 稳定版发布中修复并通过。
- 近期 `cancelled` 记录来自并发策略取消旧 run 或 dev push run 被后续 workflow_dispatch 收尾 run 替代，
  不代表 APK、代码或模拟器验收失败。

处理规则：

- 每轮合入后必须核对最近失败 run；若是新 failure，立即定位并修，不进入下一功能切片。
- 对 `cancelled` 必须先确认是否存在替代成功 run；有替代成功 run 时记录为通知噪声，不升级为产品失败。
- 对候选/正式 Release 失败必须记录失败 run、原因、修复版本和后续成功 run，禁止用“后来好了”覆盖失败证据。
