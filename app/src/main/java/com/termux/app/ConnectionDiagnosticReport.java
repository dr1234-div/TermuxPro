package com.termux.app;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 解析远端连接诊断的 NUL 分隔结果。 */
final class ConnectionDiagnosticReport {

    private ConnectionDiagnosticReport() {}

    @NonNull
    static List<Item> parse(@NonNull String output) {
        String[] fields = output.split("\u0000", -1);
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (!fields[index].isEmpty()) values.put(fields[index], fields[index + 1]);
        }
        List<Item> items = new ArrayList<>();
        add(items, values, "SYSTEM", "系统");
        add(items, values, "PROJECT", "项目目录");
        add(items, values, "SHELL", "Shell");
        add(items, values, "TMUX", "tmux 会话恢复");
        add(items, values, "GIT", "Git 审查");
        add(items, values, "NODE", "Node.js 项目");
        add(items, values, "JAVA", "Java 项目");
        add(items, values, "CLAUDE", "Claude Code");
        add(items, values, "CODEX", "Codex CLI");
        return items;
    }

    private static void add(List<Item> items, Map<String, String> values, String key, String label) {
        String value = values.get(key);
        if (value != null) items.add(new Item(label, value, "OK".equals(value)));
    }

    static final class Item {
        final String label;
        final String value;
        final boolean available;

        Item(String label, String value, boolean available) {
            this.label = label;
            this.value = value;
            this.available = available;
        }

        @NonNull
        @Override
        public String toString() {
            String display = "OK".equals(value) ? "可用" : ("MISSING".equals(value) ? "未找到" : value.trim());
            return (available ? "✓  " : "•  ") + label + "\n    " + display;
        }
    }
}
