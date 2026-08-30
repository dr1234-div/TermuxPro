package com.termux.app;

import androidx.annotation.NonNull;

import java.util.UUID;

/** 用户按工作区保存的非敏感快捷指令。 */
final class CustomCommand {
    enum Confirmation {
        ALWAYS,
        DANGEROUS_ONLY
    }

    @NonNull final String id;
    @NonNull final String name;
    @NonNull final String command;
    @NonNull final String workingDirectory;
    @NonNull final String group;
    final boolean enabled;
    @NonNull final Confirmation confirmation;

    CustomCommand(@NonNull String id, @NonNull String name, @NonNull String command,
                  @NonNull String workingDirectory, @NonNull String group, boolean enabled,
                  @NonNull Confirmation confirmation) {
        this.id = id;
        this.name = name;
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.group = group;
        this.enabled = enabled;
        this.confirmation = confirmation;
    }

    @NonNull
    static CustomCommand create(@NonNull String name, @NonNull String command,
                                @NonNull String workingDirectory, @NonNull String group,
                                @NonNull Confirmation confirmation) {
        return new CustomCommand(UUID.randomUUID().toString(), name, command, workingDirectory,
            group, true, confirmation);
    }

    @NonNull
    CustomCommand copyWithId() {
        return new CustomCommand(UUID.randomUUID().toString(), name, command, workingDirectory,
            group, enabled, confirmation);
    }

    @NonNull
    CustomCommand withEnabled(boolean value) {
        return new CustomCommand(id, name, command, workingDirectory, group, value, confirmation);
    }
}
