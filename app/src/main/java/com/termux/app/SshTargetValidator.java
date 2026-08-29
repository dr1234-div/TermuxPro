package com.termux.app;

import androidx.annotation.NonNull;

/** 校验作为 OpenSSH 单一 argv 参数传递的目标，支持 ~/.ssh/config 别名。 */
final class SshTargetValidator {

    private static final int MAX_TARGET_LENGTH = 512;

    private SshTargetValidator() {}

    static boolean isValid(@NonNull String value) {
        if (value.isEmpty() || value.length() > MAX_TARGET_LENGTH || value.charAt(0) == '-') return false;
        int atCount = 0;
        int atIndex = -1;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)) return false;
            if (character == '@') {
                atCount++;
                atIndex = index;
            }
        }
        if (atCount > 1) return false;
        return atCount == 0 || (atIndex > 0 && atIndex < value.length() - 1);
    }
}
