package com.termux.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DiffLineClassifierTest {

    @Test
    public void recognizesUnifiedDiffLinesWithoutConfusingFileHeaders() {
        assertEquals(DiffLineClassifier.Kind.HEADER, DiffLineClassifier.classify("+++ b/app.java"));
        assertEquals(DiffLineClassifier.Kind.HEADER, DiffLineClassifier.classify("--- a/app.java"));
        assertEquals(DiffLineClassifier.Kind.HUNK, DiffLineClassifier.classify("@@ -1,2 +1,3 @@"));
        assertEquals(DiffLineClassifier.Kind.ADDITION, DiffLineClassifier.classify("+new line"));
        assertEquals(DiffLineClassifier.Kind.DELETION, DiffLineClassifier.classify("-old line"));
        assertEquals(DiffLineClassifier.Kind.NORMAL, DiffLineClassifier.classify(" context"));
    }
}
