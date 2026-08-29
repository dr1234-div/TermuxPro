package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ProjectTaskDetectorTest {

    @Test
    public void detectsUniAppAndOrdersCommonScriptsFirst() throws Exception {
        String json = "{\"scripts\":{\"lint\":\"eslint .\",\"dev\":\"vite\","
            + "\"unsafe;name\":\"ignored body\"},\"dependencies\":{\"@dcloudio/uni-app\":\"1\"}}";
        ProjectTaskDetector.ProjectInfo info = ProjectTaskDetector.parse("PACKAGE_JSON\0pnpm\0" + json);

        assertEquals("uni-app · pnpm", info.type);
        assertEquals("dev", info.tasks.get(0).label);
        assertTrue(info.tasks.get(2).command.contains("'unsafe;name'"));
        assertTrue(!info.tasks.get(2).command.contains("ignored body"));
    }

    @Test
    public void detectsMavenWrapperWithoutReadingBuildScripts() throws Exception {
        ProjectTaskDetector.ProjectInfo info = ProjectTaskDetector.parse("MAVEN_WRAPPER\0");

        assertEquals("Maven", info.type);
        assertEquals("./mvnw test", info.tasks.get(0).command);
    }
}
