package com.termux.app;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开项目任务页，避免把启发式检测命令写入当前 AI/TUI shell。 */
final class ProjectTasksNavigation {
    private ProjectTasksNavigation() {}

    @Nullable
    static Intent newIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured()) return null;
        String ownerToken = new WorkspaceOwnershipStore(context).getOrCreate(target.id);
        if (!WorkspaceOwnershipStore.isValid(ownerToken)) return null;
        return ProjectTasksActivity.newIntent(context, target.host, target.port, target.path, ownerToken);
    }
}
