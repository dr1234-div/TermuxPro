# 官方 Termux 上游同步

`upstream` 只用于拉取 `https://github.com/termux/termux-app.git`，禁止推送。同步在专用分支完成：

```bash
git fetch upstream
git switch dev
git switch -c chore/sync-termux-YYYYMMDD
git merge --no-ff upstream/master
```

解决冲突后必须运行全模块测试、Lint、Debug/Release 构建以及工作区真机回归。保留上游版权和 GPLv3
声明；不得用批量覆盖丢弃 TermuxPro 的入口、安全校验或中文资源。通过 PR 合入 `dev`，不得直接合入
`master`。
