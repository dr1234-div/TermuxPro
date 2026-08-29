package com.termux.app;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 在有限的终端文本尾部执行大小写不敏感搜索，避免长会话占用过多内存。 */
final class TerminalSearchMatcher {

    private static final int MAX_LINE_DISPLAY_CHARS = 320;

    private TerminalSearchMatcher() {}

    @NonNull
    static Result search(@NonNull String transcript, @NonNull String query,
                         int maxInputChars, int maxMatches) {
        String needle = query.trim();
        if (needle.isEmpty() || maxInputChars <= 0 || maxMatches <= 0) {
            return new Result(new ArrayList<>(), false, false);
        }
        boolean inputTruncated = transcript.length() > maxInputChars;
        String input = inputTruncated ? transcript.substring(transcript.length() - maxInputChars) : transcript;
        String lowerNeedle = needle.toLowerCase(Locale.ROOT);
        String[] lines = input.split("\n", -1);
        List<String> matches = new ArrayList<>();
        boolean resultTruncated = false;
        for (String line : lines) {
            int position = line.toLowerCase(Locale.ROOT).indexOf(lowerNeedle);
            if (position < 0) continue;
            if (matches.size() >= maxMatches) {
                resultTruncated = true;
                break;
            }
            matches.add(compactLine(line, position, needle.length()));
        }
        return new Result(matches, inputTruncated, resultTruncated);
    }

    @NonNull
    private static String compactLine(String line, int matchStart, int matchLength) {
        if (line.length() <= MAX_LINE_DISPLAY_CHARS) return line;
        int context = (MAX_LINE_DISPLAY_CHARS - Math.min(matchLength, MAX_LINE_DISPLAY_CHARS)) / 2;
        int start = Math.max(0, matchStart - context);
        int end = Math.min(line.length(), start + MAX_LINE_DISPLAY_CHARS);
        start = Math.max(0, end - MAX_LINE_DISPLAY_CHARS);
        return (start > 0 ? "…" : "") + line.substring(start, end) + (end < line.length() ? "…" : "");
    }

    static final class Result {
        final List<String> matches;
        final boolean inputTruncated;
        final boolean resultTruncated;

        Result(List<String> matches, boolean inputTruncated, boolean resultTruncated) {
            this.matches = matches;
            this.inputTruncated = inputTruncated;
            this.resultTruncated = resultTruncated;
        }
    }
}
