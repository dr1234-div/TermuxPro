package com.termux.app;

import androidx.annotation.NonNull;

/** 纯文本 unified diff 行分类，不解析或记录文件正文。 */
final class DiffLineClassifier {

    enum Kind { HEADER, HUNK, ADDITION, DELETION, NORMAL }

    private DiffLineClassifier() {}

    @NonNull
    static Kind classify(@NonNull String line) {
        if (line.startsWith("diff --git ") || line.startsWith("index ") ||
            line.startsWith("--- ") || line.startsWith("+++ ") || line.equals("--- DIFF ---")) {
            return Kind.HEADER;
        }
        if (line.startsWith("@@")) return Kind.HUNK;
        if (line.startsWith("+")) return Kind.ADDITION;
        if (line.startsWith("-")) return Kind.DELETION;
        return Kind.NORMAL;
    }
}
