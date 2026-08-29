package com.termux.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/** 不包含主机或凭据的工作区连接事实，用于避免把“打开终端”误报成“连接成功”。 */
final class WorkspaceConnectionState {

    enum Status { TERMINAL_OPENED, VERIFIED, ACTION_REQUIRED, FAILED, UNKNOWN }

    final Status status;
    @Nullable final SshDiagnosticStages.Stage stage;
    final long timestamp;

    WorkspaceConnectionState(@NonNull Status status,
                             @Nullable SshDiagnosticStages.Stage stage, long timestamp) {
        this.status = status;
        this.stage = stage;
        this.timestamp = timestamp;
    }

    static WorkspaceConnectionState terminalOpened(long timestamp) {
        return new WorkspaceConnectionState(Status.TERMINAL_OPENED, null, timestamp);
    }

    static WorkspaceConnectionState fromStages(@NonNull List<SshDiagnosticStages.Item> stages,
                                                long timestamp) {
        boolean pending = stages.isEmpty();
        for (SshDiagnosticStages.Item item : stages) {
            if (item.state == SshDiagnosticStages.State.ACTION_REQUIRED) {
                return new WorkspaceConnectionState(Status.ACTION_REQUIRED, item.stage, timestamp);
            }
            if (item.state == SshDiagnosticStages.State.FAILED) {
                return new WorkspaceConnectionState(Status.FAILED, item.stage, timestamp);
            }
            pending |= item.state == SshDiagnosticStages.State.PENDING;
        }
        return new WorkspaceConnectionState(pending ? Status.UNKNOWN : Status.VERIFIED,
            null, timestamp);
    }
}
