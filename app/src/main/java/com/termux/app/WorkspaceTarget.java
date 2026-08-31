package com.termux.app;

import androidx.annotation.NonNull;

/** 可安全展示并传递给远程工具的工作区目标快照。 */
final class WorkspaceTarget {
    @NonNull final String id;
    @NonNull final String name;
    @NonNull final String host;
    final int port;
    @NonNull final String path;

    WorkspaceTarget(@NonNull String id, @NonNull String name, @NonNull String host,
                    int port, @NonNull String path) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.path = path;
    }

    boolean isConfigured() {
        return !id.trim().isEmpty() && SshTargetValidator.isValid(host)
            && port >= 1 && port <= 65535 && !path.trim().isEmpty();
    }
}
