package com.termux.app;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/** 统一约束用户可创建和重命名的 tmux 会话名称。 */
final class TmuxSessionNameValidator {
    // tmux 会把点号静默改成下划线；禁止点号，避免 UI 名称与远端实际对象不一致并遗留孤儿会话。
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");

    private TmuxSessionNameValidator() {}

    static boolean isValid(@Nullable String value) {
        return value != null && SAFE_NAME.matcher(value).matches();
    }
}
