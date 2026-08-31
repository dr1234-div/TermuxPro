package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.R.attr;
import android.app.AlertDialog;
import android.content.Intent;
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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.annotation.Config;

/** 无设备环境下验证启动页可创建、中文资源可加载且关键入口齐全。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class WorkspaceActivitySmokeTest {

    @Test
    public void launcherKeepsOnlySetupActionVisibleWhenSshIsMissing() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();

        assertEquals("TermuxPro", activity.getTitle());
        assertEquals("继续远程项目", activity.getString(R.string.workspace_home_title));
        assertEquals("打开远程终端", activity.getString(R.string.workspace_connect_action));
        assertEquals("连接配置", activity.getString(R.string.workspace_manage_action));
        int[] visibleViews = {R.id.workspace_setup_button};
        for (int id : visibleViews) {
            View view = activity.findViewById(id);
            assertNotNull(view);
            assertEquals(View.VISIBLE, view.getVisibility());
        }
        int[] hiddenViews = {
            R.id.workspace_remote_card,
            R.id.workspace_ai_actions,
            R.id.workspace_development_tools_card,
            R.id.workspace_preview_card,
            R.id.workspace_local_terminal_button,
            R.id.workspace_new_button,
            R.id.workspace_save_button,
            R.id.workspace_copy_button,
            R.id.workspace_delete_button
        };
        for (int id : hiddenViews) {
            assertEquals(View.GONE, activity.findViewById(id).getVisibility());
        }
        activity.finish();
    }

    @Test
    public void configuredHomeKeepsLowFrequencyToolsOutOfHomeScreen() {
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), WorkspaceActivity.class);
        intent.putExtra(WorkspaceActivity.EXTRA_UI_TEST_SSH_READY, true);
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class, intent)
            .setup().get();

        ((EditText) activity.findViewById(R.id.workspace_host_input))
            .setText("hdr@192.168.1.153");
        activity.findViewById(R.id.workspace_save_button).performClick();

        assertEquals("服务器与项目",
            ((TextView) activity.findViewById(R.id.workspace_remote_card_title))
                .getText().toString());
        assertEquals("连接配置",
            ((TextView) activity.findViewById(R.id.workspace_manage_button)).getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_summary).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_manage_button).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_new_button).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_selector).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_development_tools_card).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_preview_card).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_local_terminal_button).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_host_input).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_connection_policy_selector)
            .getVisibility());
        activity.finish();
    }

    @Test
    public void savedWorkspaceCollapsesEditorIntoConnectionSummary() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        ((EditText) activity.findViewById(R.id.workspace_host_input))
            .setText("hdr@192.168.1.153");
        activity.findViewById(R.id.workspace_save_button).performClick();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_summary).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_host_input).getVisibility());
        assertTrue(((TextView) activity.findViewById(R.id.workspace_summary_details))
            .getText().toString().contains("hdr@192.168.1.153"));

        activity.findViewById(R.id.workspace_edit_button).performClick();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_host_input).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_summary).getVisibility());
        activity.finish();
    }

    @Test
    public void firstConfigurationKeepsTmuxOptionsBehindAdvancedSettings() {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_host_input).getVisibility());
        assertEquals(View.GONE,
            activity.findViewById(R.id.workspace_connection_policy_selector).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_session_name_input).getVisibility());

        activity.findViewById(R.id.workspace_advanced_button).performClick();
        assertEquals(View.VISIBLE,
            activity.findViewById(R.id.workspace_connection_policy_selector).getVisibility());
        assertEquals(View.GONE,
            activity.findViewById(R.id.workspace_session_name_input).getVisibility());
        ((Spinner) activity.findViewById(R.id.workspace_connection_policy_selector)).setSelection(2);
        assertEquals(View.VISIBLE,
            activity.findViewById(R.id.workspace_session_name_input).getVisibility());
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
        assertEquals(View.VISIBLE, selector.getVisibility());
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
        ((EditText) activity.findViewById(R.id.workspace_host_input))
            .setText("hdr@192.168.1.153");
        activity.findViewById(R.id.workspace_save_button).performClick();
        String profiles = activity.getSharedPreferences("ai_terminal_workspace", 0)
            .getString("profiles_v2", "[]");
        String workspaceId = new JSONArray(profiles).getJSONObject(0).getString("id");
        new WorkspaceConnectionStateStore(activity).save(workspaceId,
            new WorkspaceConnectionState(WorkspaceConnectionState.Status.VERIFIED,
                null, System.currentTimeMillis()));

        activity.onResume();
        TextView feedback = activity.findViewById(R.id.workspace_connection_feedback);
        assertTrue(feedback.getText().toString().contains("最近验证"));
        assertTrue(feedback.getText().toString().contains("SSH 身份认证"));
        assertEquals(View.VISIBLE,
            activity.findViewById(R.id.workspace_ai_actions).getVisibility());
        activity.finish();
    }

    @Test
    public void expiredVerificationIsHonestWithoutBlockingSafeAiLaunch() throws JSONException {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        ((EditText) activity.findViewById(R.id.workspace_host_input))
            .setText("hdr@192.168.1.153");
        activity.findViewById(R.id.workspace_save_button).performClick();
        String profiles = activity.getSharedPreferences("ai_terminal_workspace", 0)
            .getString("profiles_v2", "[]");
        String workspaceId = new JSONArray(profiles).getJSONObject(0).getString("id");
        new WorkspaceConnectionStateStore(activity).save(workspaceId,
            new WorkspaceConnectionState(WorkspaceConnectionState.Status.VERIFIED,
                null, System.currentTimeMillis() - WorkspaceConnectionState.VERIFICATION_TTL_MS - 1L));

        activity.onResume();
        assertTrue(((TextView) activity.findViewById(R.id.workspace_summary_details))
            .getText().toString().contains("验证已过期"));
        assertTrue(((TextView) activity.findViewById(R.id.workspace_connection_feedback))
            .getText().toString().contains("检查结果已过期"));
        assertEquals(View.VISIBLE,
            activity.findViewById(R.id.workspace_connection_diagnostic_primary).getVisibility());
        assertEquals(View.VISIBLE,
            activity.findViewById(R.id.workspace_ai_actions).getVisibility());
        activity.finish();
    }

    @Test
    public void editingConnectionIdentityInvalidatesPreviousVerification() throws JSONException {
        WorkspaceActivity activity = Robolectric.buildActivity(WorkspaceActivity.class).setup().get();
        EditText host = activity.findViewById(R.id.workspace_host_input);
        host.setText("hdr@old.example.com");
        activity.findViewById(R.id.workspace_save_button).performClick();
        String profiles = activity.getSharedPreferences("ai_terminal_workspace", 0)
            .getString("profiles_v2", "[]");
        String workspaceId = new JSONArray(profiles).getJSONObject(0).getString("id");
        WorkspaceConnectionStateStore store = new WorkspaceConnectionStateStore(activity);
        store.save(workspaceId, new WorkspaceConnectionState(
            WorkspaceConnectionState.Status.VERIFIED, null, 1_700_000_000_000L));
        activity.onResume();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.workspace_ai_actions).getVisibility());

        activity.findViewById(R.id.workspace_edit_button).performClick();
        host.setText("hdr@new.example.com");
        activity.findViewById(R.id.workspace_save_button).performClick();

        assertEquals(null, store.read(workspaceId));
        assertEquals(View.GONE, activity.findViewById(R.id.workspace_ai_actions).getVisibility());
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
        assertEquals(2,
            ((android.widget.TextView) dialog.getListView().getChildAt(0)).getMaxLines());
        assertTrue(((android.widget.TextView) dialog.getListView().getChildAt(0)).getPaddingEnd() > 0);
        assertEquals(activity.getColor(R.color.tp_primary),
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
        dialog.dismiss();
        activity.finish();
    }
}
