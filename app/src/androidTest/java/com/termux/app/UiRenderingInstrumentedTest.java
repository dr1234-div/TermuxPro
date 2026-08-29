package com.termux.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

/** 在真实 Android 渲染器中逐页截图，防止厂商主题默认文字色和大字体回归。 */
@RunWith(AndroidJUnit4.class)
public final class UiRenderingInstrumentedTest {

    @Test
    public void captureCriticalDarkPages() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        capture(context, "workspace", new Intent(context, WorkspaceActivity.class));
        capture(context, "remote-files",
            RemoteFilesActivity.newIntent(context, "invalid", 0, "~/project"));
        capture(context, "project-tasks",
            ProjectTasksActivity.newIntent(context, "invalid", 0, "~/project"));
        capture(context, "connection-diagnostic",
            ConnectionDiagnosticActivity.newIntent(context, "invalid", 0, "~/project"));
        capture(context, "task-sessions",
            TaskSessionsActivity.newIntent(context, "invalid", 0));
    }

    private void capture(Context context, String name, Intent intent) throws Exception {
        try (ActivityScenario<? extends Activity> scenario = ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(500L);
            UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            Bitmap screenshot = automation.takeScreenshot();
            assertNotNull("无法截取页面：" + name, screenshot);
            assertTrue(screenshot.getWidth() > 0 && screenshot.getHeight() > 0);
            writeScreenshot(name);
            screenshot.recycle();
        }
    }

    private void writeScreenshot(String name) throws IOException {
        Bundle arguments = InstrumentationRegistry.getArguments();
        String suffix = arguments.getString("screenshotSuffix", "default")
            .replaceAll("[^a-zA-Z0-9._-]", "_");
        String directory = "/sdcard/Download/termuxpro-ui-screenshots";
        String output = directory + "/" + name + "-" + suffix + ".png";
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        String command = "mkdir -p " + directory + " && screencap -p " + output;
        try (ParcelFileDescriptor descriptor = automation.executeShellCommand(command);
             InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) {
                // 读取到 EOF，确保 shell 截图命令执行完成后再切换页面。
            }
        }
    }
}
