package com.termux.app;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** 解析只包含名称、窗口数和连接状态的 NUL 分隔 tmux 输出。 */
final class TmuxSessionParser {
    static final String MISSING_MARKER = "__TERMUXPRO_TMUX_MISSING__";

    private TmuxSessionParser() {}

    @NonNull
    static List<TmuxSessionInfo> parse(@NonNull String output) {
        List<TmuxSessionInfo> sessions = new ArrayList<>();
        String[] fields = output.split("\u0000", -1);
        for (int index = 0; index + 2 < fields.length; index += 3) {
            String name = fields[index];
            if (name.isEmpty() || MISSING_MARKER.equals(name)) continue;
            int windows;
            try {
                windows = Integer.parseInt(fields[index + 1]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            sessions.add(new TmuxSessionInfo(name, windows, !"0".equals(fields[index + 2])));
        }
        return sessions;
    }

    static boolean reportsMissingTmux(@NonNull String output) {
        return output.startsWith(MISSING_MARKER + "\u0000");
    }
}
