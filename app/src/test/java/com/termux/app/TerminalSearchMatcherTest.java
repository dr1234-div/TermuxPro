package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalSearchMatcherTest {

    @Test
    public void searchesChineseAndLatinCaseInsensitively() {
        TerminalSearchMatcher.Result latin = TerminalSearchMatcher.search(
            "ok\nBuild ERROR here\n完成", "error", 10_000, 20);
        TerminalSearchMatcher.Result chinese = TerminalSearchMatcher.search(
            "等待输入\n任务完成", "完成", 10_000, 20);

        assertEquals(1, latin.matches.size());
        assertTrue(latin.matches.get(0).contains("ERROR"));
        assertEquals("任务完成", chinese.matches.get(0));
    }

    @Test
    public void boundsInputResultsAndLongLines() {
        StringBuilder transcript = new StringBuilder("old needle\n");
        for (int index = 0; index < 20; index++) transcript.append("needle ").append(index).append('\n');
        transcript.append(new String(new char[500]).replace('\0', 'x')).append("needle");
        TerminalSearchMatcher.Result result = TerminalSearchMatcher.search(
            transcript.toString(), "needle", 700, 30);
        TerminalSearchMatcher.Result capped = TerminalSearchMatcher.search(
            transcript.toString(), "needle", 700, 3);

        assertTrue(result.inputTruncated);
        assertTrue(capped.resultTruncated);
        assertEquals(3, capped.matches.size());
        assertTrue(result.matches.get(result.matches.size() - 1).length() <= 322);
        assertFalse(result.matches.get(0).contains("old needle"));
    }
}
