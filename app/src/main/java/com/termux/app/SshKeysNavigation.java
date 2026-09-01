package com.termux.app;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开 SSH 密钥页，只展示/复制公钥，不读取私钥。 */
final class SshKeysNavigation {
    private SshKeysNavigation() {}

    @Nullable
    static Intent newIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured()) return null;
        return SshKeysActivity.newIntent(context, target.host, target.port);
    }
}
