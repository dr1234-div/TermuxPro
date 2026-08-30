package com.termux.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.LocaleManager;
import android.app.UiAutomation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.LocaleList;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/** 在真实 Android 渲染器中逐页截图，防止厂商主题默认文字色和大字体回归。 */
@RunWith(AndroidJUnit4.class)
public final class UiRenderingInstrumentedTest {

    @Test
    public void captureCriticalDarkPages() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        forceSimplifiedChinese(context);
        Intent workspaceIntent = new Intent(context, WorkspaceActivity.class)
            .putExtra(WorkspaceActivity.EXTRA_UI_TEST_SSH_READY, true);
        capture(context, "workspace", workspaceIntent);
        capture(context, "workspace-policy", new Intent(workspaceIntent), activity -> {
            activity.findViewById(com.termux.R.id.workspace_advanced_button).performClick();
            View policy = activity.findViewById(com.termux.R.id.workspace_connection_policy_selector);
            ScrollView scroll = activity.findViewById(com.termux.R.id.workspace_scroll_view);
            Rect bounds = new Rect();
            policy.getDrawingRect(bounds);
            scroll.offsetDescendantRectToMyCoords(policy, bounds);
            scroll.scrollTo(0, Math.max(0, bounds.top - 120));
        });
        capture(context, "workspace-connection-guidance", new Intent(workspaceIntent), activity -> {
            ((android.widget.EditText) activity.findViewById(
                com.termux.R.id.workspace_host_input)).setText("hdr@192.168.1.153");
            activity.findViewById(com.termux.R.id.workspace_save_button).performClick();
        });
        capture(context, "workspace-connection-verified", new Intent(workspaceIntent),
            activity -> {
                ((android.widget.EditText) activity.findViewById(
                    com.termux.R.id.workspace_host_input)).setText("hdr@192.168.1.153");
                activity.findViewById(com.termux.R.id.workspace_save_button).performClick();
                String workspaceId = activity.getSharedPreferences("ai_terminal_workspace", 0)
                    .getString("active_profile", "");
                assertTrue("截图工作区必须具有持久化 ID", !workspaceId.isEmpty());
                new WorkspaceConnectionStateStore(activity).save(workspaceId,
                    new WorkspaceConnectionState(WorkspaceConnectionState.Status.VERIFIED,
                        null, 1_700_000_000_000L));
                ((WorkspaceActivity) activity).onResume();
            });
        capture(context, "ai-session-choice", new Intent(workspaceIntent), activity -> {
            ((android.widget.EditText) activity.findViewById(
                com.termux.R.id.workspace_host_input)).setText("hdr@192.168.1.153");
            activity.findViewById(com.termux.R.id.workspace_save_button).performClick();
            String workspaceId = activity.getSharedPreferences("ai_terminal_workspace", 0)
                .getString("active_profile", "");
            new WorkspaceConnectionStateStore(activity).save(workspaceId,
                new WorkspaceConnectionState(WorkspaceConnectionState.Status.VERIFIED,
                    null, 1_700_000_000_000L));
            ((WorkspaceActivity) activity).onResume();
            activity.findViewById(com.termux.R.id.workspace_claude_button).performClick();
        });
        capture(context, "remote-files",
            RemoteFilesActivity.newIntent(context, "invalid", 0, "~/project"), activity ->
                assertReadableRecoveryState(activity, com.termux.R.id.remote_files_status_state,
                    com.termux.R.id.remote_files_status_message,
                    com.termux.R.id.remote_files_return_workspace_button));
        capture(context, "project-tasks",
            ProjectTasksActivity.newIntent(context, "invalid", 0, "~/project"), activity ->
                {
                assertToolbarActionsVisible(activity,
                    com.termux.R.id.project_tasks_back_button,
                    com.termux.R.id.project_tasks_refresh_button);
                assertReadableRecoveryMessage(activity,
                    com.termux.R.id.project_tasks_status,
                    com.termux.R.id.project_tasks_recovery_button);
                });
        capture(context, "connection-diagnostic",
            ConnectionDiagnosticActivity.newIntent(context, "invalid", 0, "~/project",
                "ui-workspace"), activity -> {
                assertToolbarActionsVisible(activity,
                    com.termux.R.id.connection_diagnostic_back_button,
                    com.termux.R.id.connection_diagnostic_refresh_button);
                assertReadableRecoveryMessage(activity,
                    com.termux.R.id.connection_diagnostic_status,
                    com.termux.R.id.connection_diagnostic_return_workspace_button);
                });
        capture(context, "task-sessions",
            TaskSessionsActivity.newIntent(context, "invalid", 0, "~/project"), activity -> {
                assertToolbarActionsVisible(activity,
                    com.termux.R.id.task_sessions_back_button,
                    com.termux.R.id.task_sessions_refresh_button);
                assertReadableRecoveryMessage(activity,
                    com.termux.R.id.task_sessions_status,
                    com.termux.R.id.task_sessions_recovery_button);
                });
        capture(context, "git-diff",
            GitDiffActivity.newIntent(context, "invalid", 0, "~/project"), activity ->
                assertReadableRecoveryState(activity, com.termux.R.id.git_diff_status_state,
                    com.termux.R.id.git_diff_status_message,
                    com.termux.R.id.git_diff_return_workspace_button));
        capture(context, "remote-file-preview",
            RemoteFilePreviewActivity.newIntent(context, "invalid", 0, "~/project", "README.md"),
            activity -> assertReadableRecoveryState(activity,
                com.termux.R.id.remote_file_status_state,
                com.termux.R.id.remote_file_status_message,
                com.termux.R.id.remote_file_return_workspace_button));
        capture(context, "ssh-keys",
            SshKeysActivity.newIntent(context, "invalid", 0));
    }

    /** 截图门禁固定使用产品主语言，避免英文短文案通过后误判中文布局也通过。 */
    @SuppressWarnings("deprecation")
    private void forceSimplifiedChinese(Context context) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager localeManager = context.getSystemService(LocaleManager.class);
            if (localeManager != null) {
                localeManager.setApplicationLocales(LocaleList.forLanguageTags("zh-CN"));
            }
        }
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        context.getResources().updateConfiguration(configuration,
            context.getResources().getDisplayMetrics());
        Context instrumentationContext = InstrumentationRegistry.getInstrumentation().getContext();
        instrumentationContext.getResources().updateConfiguration(configuration,
            instrumentationContext.getResources().getDisplayMetrics());
    }

    private void scrollTo(Activity activity, int viewId) {
        View target = activity.findViewById(viewId);
        ScrollView scroll = activity.findViewById(com.termux.R.id.workspace_scroll_view);
        Rect bounds = new Rect();
        target.getDrawingRect(bounds);
        scroll.offsetDescendantRectToMyCoords(target, bounds);
        scroll.scrollTo(0, Math.max(0, bounds.top - 80));
    }

    private void assertReadableRecoveryState(Activity activity, int stateId, int messageId,
                                             int actionId) {
        assertTrue(activity.findViewById(stateId).getVisibility() == View.VISIBLE);
        TextView message = activity.findViewById(messageId);
        assertTrue(!message.getText().toString().isEmpty());
        assertTrue(!message.getText().toString().contains("退出码"));
        assertTrue(activity.findViewById(actionId).getVisibility() == View.VISIBLE);
    }

    private void assertReadableRecoveryMessage(Activity activity, int messageId, int actionId) {
        TextView message = activity.findViewById(messageId);
        assertTrue(message.getVisibility() == View.VISIBLE);
        assertTrue(!message.getText().toString().isEmpty());
        assertTrue(!message.getText().toString().contains("退出码"));
        assertTrue(activity.findViewById(actionId).getVisibility() == View.VISIBLE);
    }

    private void assertToolbarActionsVisible(Activity activity, int backId, int refreshId) {
        assertViewHasVisibleBounds(activity.findViewById(backId));
        assertViewHasVisibleBounds(activity.findViewById(refreshId));
    }

    private void assertViewHasVisibleBounds(View view) {
        Rect bounds = new Rect();
        assertTrue(view.isShown());
        assertTrue(view.getGlobalVisibleRect(bounds));
        assertTrue(bounds.width() >= view.getWidth() / 2);
        assertTrue(bounds.height() >= view.getHeight() / 2);
    }

    private void capture(Context context, String name, Intent intent) throws Exception {
        capture(context, name, intent, null);
    }

    private void capture(Context context, String name, Intent intent, ScreenPreparer preparer)
        throws Exception {
        try (ActivityScenario<? extends Activity> scenario = ActivityScenario.launch(intent)) {
            if (preparer != null) scenario.onActivity(preparer::prepare);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(500L);
            UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            Bitmap screenshot = automation.takeScreenshot();
            assertNotNull("无法截取页面：" + name, screenshot);
            assertTrue(screenshot.getWidth() > 0 && screenshot.getHeight() > 0);
            writeScreenshot(context, screenshot, name);
            screenshot.recycle();
        }
    }

    private interface ScreenPreparer {
        void prepare(Activity activity);
    }

    private void writeScreenshot(Context context, Bitmap screenshot, String name) throws IOException {
        assertTrue("模拟器截图需要 Android 10 及以上 MediaStore", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Bundle arguments = InstrumentationRegistry.getArguments();
        String suffix = arguments.getString("screenshotSuffix", "default")
            .replaceAll("[^a-zA-Z0-9._-]", "_");

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name + "-" + suffix + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/termuxpro-ui-screenshots");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri output = context.getContentResolver().insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull("无法创建截图媒体文件：" + name, output);
        try (OutputStream stream = context.getContentResolver().openOutputStream(output)) {
            assertNotNull("无法打开截图输出流：" + name, stream);
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        assertTrue(context.getContentResolver().update(output, values, null, null) == 1);
    }
}
