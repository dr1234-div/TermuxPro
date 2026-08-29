package com.termux.app;

import androidx.annotation.NonNull;

/** AI CLI 启动策略。历史会话只能通过用户可见的原生选择器进入。 */
final class AiCliLaunchCommand {

    enum Tool { CLAUDE, CODEX }
    enum Mode { NEW_SESSION, PICK_HISTORY }

    private AiCliLaunchCommand() {}

    @NonNull
    static String command(Tool tool, Mode mode) {
        if (mode == Mode.NEW_SESSION) return tool == Tool.CLAUDE ? "claude" : "codex";
        return tool == Tool.CLAUDE ? "claude --resume" : "codex resume";
    }

    @NonNull
    static String displayName(Tool tool) {
        return tool == Tool.CLAUDE ? "Claude Code" : "Codex CLI";
    }
}
