package com.termux.app;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开远端文件页，避免用户回首页或向当前 shell 注入文件命令。 */
final class RemoteFilesNavigation {
    private RemoteFilesNavigation() {}

    @Nullable
    static Intent newIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured()) return null;
        return RemoteFilesActivity.newIntent(context, target.host, target.port, target.path);
    }
}
