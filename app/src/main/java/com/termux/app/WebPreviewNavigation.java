package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 终端工具箱打开 Web 预览增值能力，不向当前 shell 注入命令。 */
final class WebPreviewNavigation {
    private WebPreviewNavigation() {}

    @Nullable
    static Intent newStartTunnelIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured() || !target.hasValidPreviewPorts()) {
            return null;
        }
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND,
            WorkspaceCommandBuilder.buildPortForwardCommand(
                target.host, target.port, target.localPort, target.remotePort));
        intent.putExtra(TermuxActivity.EXTRA_NEW_SESSION, true);
        return intent;
    }

    @Nullable
    static Intent newOpenBrowserIntentForActiveWorkspace(@NonNull Context context) {
        WorkspaceTarget target = WorkspaceTargetStore.readActive(context);
        if (target == null || !target.isConfigured() || !target.hasValidPreviewPorts()) {
            return null;
        }
        return new Intent(Intent.ACTION_VIEW,
            Uri.parse("http://127.0.0.1:" + target.localPort));
    }
}
