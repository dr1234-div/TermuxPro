package com.termux.app;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开 Git 工作台的目标解析，避免回退到原始 git 命令输出。 */
final class GitWorkbenchNavigation {
    private GitWorkbenchNavigation() {}

    @Nullable
    static Intent newIntentForActiveWorkspace(@NonNull Context context, boolean startInDiff) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured()) return null;
        return GitDiffActivity.newIntent(context, target.host, target.port, target.path)
            .putExtra(GitDiffActivity.EXTRA_START_IN_DIFF, startInDiff);
    }
}
