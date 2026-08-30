package com.termux.app;

import androidx.annotation.NonNull;

/** 远端 tmux 会话的最小脱敏元数据。 */
final class TmuxSessionInfo {
    final String name;
    final int windows;
    final boolean attached;
    final boolean managedByTermuxPro;

    TmuxSessionInfo(@NonNull String name, int windows, boolean attached,
                    @NonNull String ownerMarker, @NonNull String workspaceMarker,
                    @NonNull String expectedOwnerToken, @NonNull String expectedWorkspaceFingerprint) {
        this.name = name;
        this.windows = windows;
        this.attached = attached;
        this.managedByTermuxPro = !ownerMarker.isEmpty() && ownerMarker.equals(expectedOwnerToken)
            && !workspaceMarker.isEmpty() && workspaceMarker.equals(expectedWorkspaceFingerprint);
    }
}
