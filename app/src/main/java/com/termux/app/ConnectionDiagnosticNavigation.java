package com.termux.app;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开连接诊断页，帮助用户在当前远程上下文内定位 SSH/环境问题。 */
final class ConnectionDiagnosticNavigation {
    private ConnectionDiagnosticNavigation() {}

    @Nullable
    static Intent newIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured()) return null;
        return ConnectionDiagnosticActivity.newIntent(context, target.host, target.port, target.path,
            target.id);
    }
}
