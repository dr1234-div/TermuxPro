package com.termux.app;

import androidx.annotation.NonNull;

/** 可安全展示并传递给远程工具的工作区目标快照。 */
final class WorkspaceTarget {
    @NonNull final String id;
    @NonNull final String name;
    @NonNull final String host;
    final int port;
    @NonNull final String path;
    final int remotePort;
    final int localPort;

    WorkspaceTarget(@NonNull String id, @NonNull String name, @NonNull String host,
                    int port, @NonNull String path) {
        this(id, name, host, port, path, -1, -1);
    }

    WorkspaceTarget(@NonNull String id, @NonNull String name, @NonNull String host,
                    int port, @NonNull String path, int remotePort, int localPort) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.path = path;
        this.remotePort = remotePort;
        this.localPort = localPort;
    }

    boolean isConfigured() {
        return !id.trim().isEmpty() && SshTargetValidator.isValid(host)
            && port >= 1 && port <= 65535 && !path.trim().isEmpty();
    }

    boolean hasValidPreviewPorts() {
        return remotePort >= 1 && remotePort <= 65535 && localPort >= 1 && localPort <= 65535;
    }
}
