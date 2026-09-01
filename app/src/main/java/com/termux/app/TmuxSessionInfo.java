package com.termux.app;

import androidx.annotation.NonNull;

/** 远端 tmux 会话的最小脱敏元数据。 */
final class TmuxSessionInfo {
    enum OwnershipState {
        CURRENT_WORKSPACE,
        OTHER_WORKSPACE,
        OTHER_OWNER,
        INCOMPLETE_MARKER,
        UNMARKED
    }

    final String name;
    final int windows;
    final boolean attached;
    final long createdEpochSeconds;
    final long activityEpochSeconds;
    final OwnershipState ownershipState;
    final boolean managedByTermuxPro;

    TmuxSessionInfo(@NonNull String name, int windows, boolean attached,
                    long createdEpochSeconds, long activityEpochSeconds,
                    @NonNull String ownerMarker, @NonNull String workspaceMarker,
                    @NonNull String expectedOwnerToken, @NonNull String expectedWorkspaceFingerprint) {
        this.name = name;
        this.windows = windows;
        this.attached = attached;
        this.createdEpochSeconds = createdEpochSeconds;
        this.activityEpochSeconds = activityEpochSeconds;
        boolean hasOwner = !ownerMarker.isEmpty();
        boolean hasWorkspace = !workspaceMarker.isEmpty();
        boolean ownerMatches = hasOwner && ownerMarker.equals(expectedOwnerToken);
        boolean workspaceMatches = hasWorkspace && workspaceMarker.equals(expectedWorkspaceFingerprint);
        if (ownerMatches && workspaceMatches) {
            this.ownershipState = OwnershipState.CURRENT_WORKSPACE;
        } else if (ownerMatches && hasWorkspace) {
            this.ownershipState = OwnershipState.OTHER_WORKSPACE;
        } else if (hasOwner) {
            this.ownershipState = OwnershipState.OTHER_OWNER;
        } else if (hasWorkspace) {
            this.ownershipState = OwnershipState.INCOMPLETE_MARKER;
        } else {
            this.ownershipState = OwnershipState.UNMARKED;
        }
        this.managedByTermuxPro = this.ownershipState == OwnershipState.CURRENT_WORKSPACE;
    }
}
