package com.termux.app;

import androidx.annotation.NonNull;

/** AI CLI 通用键盘动作；不解析或自动接受任何授权提示。 */
final class AiTerminalAction {

    enum Type { CONFIRM_SELECTION, REJECT_OR_BACK, INTERRUPT }

    private AiTerminalAction() {}

    @NonNull
    static String payload(Type type) {
        switch (type) {
            case CONFIRM_SELECTION:
                return "\r";
            case REJECT_OR_BACK:
                return "\u001b";
            case INTERRUPT:
                return "\u0003";
            default:
                throw new IllegalArgumentException("Unsupported action: " + type);
        }
    }

    static boolean requiresConfirmation(Type type) {
        return type == Type.CONFIRM_SELECTION;
    }
}
