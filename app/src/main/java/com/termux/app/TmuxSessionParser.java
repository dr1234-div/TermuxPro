package com.termux.app;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** 解析名称、窗口数、连接状态、时间和两项非敏感工作区标记组成的 NUL 分隔 tmux 输出。 */
final class TmuxSessionParser {
    static final String MISSING_MARKER = "__TERMUXPRO_TMUX_MISSING__";
    private static final int FIELD_COUNT = 7;

    private TmuxSessionParser() {}

    @NonNull
    static List<TmuxSessionInfo> parse(@NonNull String output, @NonNull String expectedOwnerToken,
                                       @NonNull String expectedWorkspaceFingerprint) {
        List<TmuxSessionInfo> sessions = new ArrayList<>();
        String[] fields = output.split("\u0000", -1);
        for (int index = 0; index + FIELD_COUNT - 1 < fields.length; index += FIELD_COUNT) {
            String name = fields[index];
            if (name.isEmpty() || MISSING_MARKER.equals(name)) continue;
            int windows;
            long created;
            long activity;
            try {
                windows = Integer.parseInt(fields[index + 1]);
                created = parseLongOrZero(fields[index + 3]);
                activity = parseLongOrZero(fields[index + 4]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            sessions.add(new TmuxSessionInfo(name, windows, !"0".equals(fields[index + 2]),
                created, activity, fields[index + 5], fields[index + 6], expectedOwnerToken,
                expectedWorkspaceFingerprint));
        }
        return sessions;
    }

    private static long parseLongOrZero(@NonNull String value) {
        if (value.isEmpty()) return 0L;
        return Long.parseLong(value);
    }

    static boolean reportsMissingTmux(@NonNull String output) {
        return output.startsWith(MISSING_MARKER + "\u0000");
    }
}
