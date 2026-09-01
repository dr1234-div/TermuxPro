package com.termux.app;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

/** 生成 AI CLI 启动前的目标上下文说明，避免不同入口展示不一致。 */
final class AiCliLaunchMessage {
    private AiCliLaunchMessage() {}

    @NonNull
    static String forTarget(@NonNull Context context, @NonNull AiCliLaunchCommand.Tool tool,
                            @Nullable WorkspaceTarget target) {
        if (target == null) {
            return forValues(context, tool, "", -1, "");
        }
        return forValues(context, tool, target.host, target.port, target.path);
    }

    @NonNull
    static String forValues(@NonNull Context context, @NonNull AiCliLaunchCommand.Tool tool,
                            @NonNull String host, int port, @NonNull String path) {
        String guidance = context.getString(AiCliLaunchCommand.guidanceMessage(tool));
        String normalizedHost = host.trim();
        String normalizedPath = path.trim();
        if (!SshTargetValidator.isValid(normalizedHost) || port < 1 || port > 65535 ||
                TextUtils.isEmpty(normalizedPath)) {
            return context.getString(R.string.ai_session_target_incomplete, guidance);
        }
        return context.getString(R.string.ai_session_target_message, normalizedHost, port,
            normalizedPath, guidance);
    }
}
