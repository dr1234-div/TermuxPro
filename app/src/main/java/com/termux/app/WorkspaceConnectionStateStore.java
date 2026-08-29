package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 仅按随机工作区 ID 保存时间与诊断阶段，不保存主机、用户名、路径或任何凭据。 */
final class WorkspaceConnectionStateStore {

    private static final String PREFERENCES_NAME = "termuxpro_workspace_connection_state";
    private static final String KEY_STATUS = "status_";
    private static final String KEY_STAGE = "stage_";
    private static final String KEY_TIMESTAMP = "timestamp_";
    private final SharedPreferences mPreferences;

    WorkspaceConnectionStateStore(@NonNull Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    void save(@NonNull String workspaceId, @NonNull WorkspaceConnectionState state) {
        SharedPreferences.Editor editor = mPreferences.edit()
            .putString(KEY_STATUS + workspaceId, state.status.name())
            .putLong(KEY_TIMESTAMP + workspaceId, state.timestamp);
        if (state.stage == null) editor.remove(KEY_STAGE + workspaceId);
        else editor.putString(KEY_STAGE + workspaceId, state.stage.name());
        editor.apply();
    }

    @Nullable
    WorkspaceConnectionState read(@NonNull String workspaceId) {
        String statusValue = mPreferences.getString(KEY_STATUS + workspaceId, null);
        long timestamp = mPreferences.getLong(KEY_TIMESTAMP + workspaceId, 0L);
        if (statusValue == null || timestamp <= 0L) return null;
        try {
            WorkspaceConnectionState.Status status =
                WorkspaceConnectionState.Status.valueOf(statusValue);
            String stageValue = mPreferences.getString(KEY_STAGE + workspaceId, null);
            SshDiagnosticStages.Stage stage = stageValue == null ? null :
                SshDiagnosticStages.Stage.valueOf(stageValue);
            return new WorkspaceConnectionState(status, stage, timestamp);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    void clear(@NonNull String workspaceId) {
        mPreferences.edit().remove(KEY_STATUS + workspaceId).remove(KEY_STAGE + workspaceId)
            .remove(KEY_TIMESTAMP + workspaceId).apply();
    }
}
