package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.R.attr;
import android.app.AlertDialog;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONArray;
import org.json.JSONException;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.annotation.Config;

/** 无设备环境下验证启动页可创建、中文资源可加载且关键入口齐全。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class WorkspaceActivitySmokeTest {

    @Test
    public void launcherInflatesWithAllPrimaryMobileActions() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();

        assertEquals("TermuxPro", activity.getTitle());
        assertEquals("继续远程项目", activity.getString(R.string.workspace_home_title));
        int[] requiredViews = {
            R.id.workspace_connect_button,
            R.id.workspace_connection_feedback,
            R.id.workspace_connection_diagnostic_primary,
            R.id.workspace_claude_button,
            R.id.workspace_codex_button,
            R.id.workspace_review_diff_button,
            R.id.workspace_remote_files_button,
            R.id.workspace_project_tasks_button,
            R.id.workspace_task_sessions_button,
            R.id.workspace_diagnostic_button,
            R.id.workspace_ssh_keys_button,
            R.id.workspace_start_preview_button,
            R.id.workspace_open_preview_button,
            R.id.workspace_local_terminal_button,
            R.id.workspace_copy_button
        };
        for (int id : requiredViews) {
            View view = activity.findViewById(id);
            assertNotNull(view);
            assertEquals(View.VISIBLE, view.getVisibility());
        }
        activity.finish();
    }

    @Test
    public void copyWorkspaceKeepsEditedConnectionMetadataAndPersistsIt() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        ((EditText) activity.findViewById(R.id.workspace_host_input)).setText("hdr@192.168.1.153");
        ((EditText) activity.findViewById(R.id.workspace_path_input)).setText("~/termux-pro");

        activity.findViewById(R.id.workspace_copy_button).performClick();

        Spinner selector = activity.findViewById(R.id.workspace_selector);
        assertEquals(2, selector.getCount());
        assertEquals("远程开发 副本", selector.getSelectedItem().toString());
        assertEquals("hdr@192.168.1.153",
            ((EditText) activity.findViewById(R.id.workspace_host_input)).getText().toString());
        assertEquals(View.GONE,
            activity.findViewById(R.id.workspace_unsaved_indicator).getVisibility());
        activity.finish();

        WorkspaceActivity restored = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        assertEquals(2, ((Spinner) restored.findViewById(R.id.workspace_selector)).getCount());
        assertEquals("hdr@192.168.1.153",
            ((EditText) restored.findViewById(R.id.workspace_host_input)).getText().toString());
        restored.finish();
    }

    @Test
    public void unsavedEditsRequireConfirmationBeforeCreatingWorkspace() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        ((EditText) activity.findViewById(R.id.workspace_host_input)).setText("edited.example.com");
        assertEquals(View.VISIBLE,
            activity.findViewById(R.id.workspace_unsaved_indicator).getVisibility());

        activity.findViewById(R.id.workspace_new_button).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("放弃未保存的修改？", shadowOf(dialog).getTitle());
        assertEquals(1, ((Spinner) activity.findViewById(R.id.workspace_selector)).getCount());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, ((Spinner) activity.findViewById(R.id.workspace_selector)).getCount());
        assertEquals(View.GONE,
            activity.findViewById(R.id.workspace_unsaved_indicator).getVisibility());
        activity.finish();
    }

    @Test
    public void unsavedEditsRequireConfirmationBeforeSwitchingWorkspace() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        activity.findViewById(R.id.workspace_new_button).performClick();
        Spinner selector = activity.findViewById(R.id.workspace_selector);
        assertEquals(2, selector.getCount());

        ((EditText) activity.findViewById(R.id.workspace_host_input)).setText("unsaved.example.com");
        selector.setSelection(0);
        shadowOf(Looper.getMainLooper()).idle();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(1, selector.getSelectedItemPosition());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, selector.getSelectedItemPosition());
        assertEquals("", ((EditText) activity.findViewById(R.id.workspace_host_input))
            .getText().toString());
        activity.finish();
    }

    @Test
    public void connectionFeedbackShowsVerifiedFactsPerWorkspace() throws JSONException {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        activity.findViewById(R.id.workspace_save_button).performClick();
        String profiles = activity.getSharedPreferences("ai_terminal_workspace", 0)
            .getString("profiles_v2", "[]");
        String workspaceId = new JSONArray(profiles).getJSONObject(0).getString("id");
        new WorkspaceConnectionStateStore(activity).save(workspaceId,
            new WorkspaceConnectionState(WorkspaceConnectionState.Status.VERIFIED,
                null, 1_700_000_000_000L));

        activity.onResume();
        TextView feedback = activity.findViewById(R.id.workspace_connection_feedback);
        assertTrue(feedback.getText().toString().contains("已验证"));
        assertTrue(feedback.getText().toString().contains("SSH 身份认证"));
        activity.finish();
    }

    @Test
    public void productPagesUseOpaqueSystemBarsAndLightDefaultText() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        TypedValue value = new TypedValue();

        assertTrue(activity.getTheme().resolveAttribute(attr.windowTranslucentStatus, value, true));
        assertFalse(value.data != 0);
        assertTrue(activity.getTheme().resolveAttribute(attr.textColorPrimary, value, true));
        assertEquals(activity.getColor(R.color.tp_text_primary),
            activity.getColorStateList(value.resourceId).getDefaultColor());
        assertTrue(activity.getTheme().resolveAttribute(attr.colorAccent, value, true));
        assertEquals(activity.getColor(R.color.tp_primary), value.data);
        activity.finish();
    }

    @Test
    public void aiShortcutRequiresExplicitNewOrHistoryChoice() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();

        activity.findViewById(R.id.workspace_claude_button).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("启动 Claude Code", shadowOf(dialog).getTitle());
        assertEquals("新建会话（安全默认）", dialog.getListView().getAdapter().getItem(0));
        assertEquals("选择历史会话", dialog.getListView().getAdapter().getItem(1));
        assertEquals(activity.getColor(R.color.tp_primary),
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
        dialog.dismiss();
        activity.finish();
    }
}
