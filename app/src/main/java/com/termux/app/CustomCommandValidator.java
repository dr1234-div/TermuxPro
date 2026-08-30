package com.termux.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/** 在快捷指令落盘前拒绝空值、超长内容和常见秘密。 */
final class CustomCommandValidator {
    enum Error {
        NAME_REQUIRED,
        NAME_TOO_LONG,
        COMMAND_REQUIRED,
        COMMAND_TOO_LONG,
        DIRECTORY_TOO_LONG,
        GROUP_TOO_LONG,
        POSSIBLE_SECRET
    }

    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
        "(?i)(password|passwd|token|secret|api[_-]?key|access[_-]?key)\\s*=\\s*[^$\\s][^\\s]*");
    private static final Pattern PRIVATE_KEY = Pattern.compile(
        "(?i)-----BEGIN(?: [A-Z0-9]+)? PRIVATE KEY-----");
    private static final Pattern URL_CREDENTIALS = Pattern.compile(
        "(?i)[a-z][a-z0-9+.-]*://[^/\\s:@]+:[^/\\s@]+@");
    private static final Pattern DANGEROUS_COMMAND = Pattern.compile(
        "(?im)(^|[;&|]\\s*)(sudo\\s+|su\\s|rm\\s|mv\\s|chmod\\s|chown\\s|kill(?:all)?\\s|"
            + "pkill\\s|git\\s+(?:reset\\s+--hard|clean\\s|push\\s+--force)|"
            + "docker\\s+(?:rm|system\\s+prune)|kubectl\\s+delete|drop\\s+(?:table|database))");

    private CustomCommandValidator() {}

    @Nullable
    static Error validate(@NonNull CustomCommand value) {
        if (value.name.trim().isEmpty()) return Error.NAME_REQUIRED;
        if (value.name.length() > 64) return Error.NAME_TOO_LONG;
        if (value.command.trim().isEmpty()) return Error.COMMAND_REQUIRED;
        if (value.command.length() > 4096) return Error.COMMAND_TOO_LONG;
        if (value.workingDirectory.length() > 1024) return Error.DIRECTORY_TOO_LONG;
        if (value.group.length() > 40) return Error.GROUP_TOO_LONG;
        if (containsPossibleSecret(value.command)) return Error.POSSIBLE_SECRET;
        return null;
    }

    static boolean containsPossibleSecret(@NonNull String command) {
        return SECRET_ASSIGNMENT.matcher(command).find()
            || PRIVATE_KEY.matcher(command).find()
            || URL_CREDENTIALS.matcher(command).find();
    }

    static boolean isLikelyDangerous(@NonNull String command) {
        return DANGEROUS_COMMAND.matcher(command).find();
    }
}
