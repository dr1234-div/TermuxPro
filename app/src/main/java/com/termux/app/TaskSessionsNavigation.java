package com.termux.app;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开 TermuxPro 会话中心，避免向当前 shell 注入原始 tmux 命令。 */
final class TaskSessionsNavigation {
    private TaskSessionsNavigation() {}

    @Nullable
    static Intent newIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured()) return null;
        String ownerToken = new WorkspaceOwnershipStore(context).getOrCreate(target.id);
        if (!WorkspaceOwnershipStore.isValid(ownerToken)) return null;
        return TaskSessionsActivity.newIntent(context, target.host, target.port, target.path, ownerToken);
    }
}
