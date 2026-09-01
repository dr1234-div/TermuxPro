package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.termux.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class CustomCommandsActivityTest {

    @Before
    public void setUp() {
        RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, 0).edit().clear()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"workspace-a\",\"name\":\"移动端\",\"host\":\"hdr@192.168.1.153\",\"port\":\"22\",\"path\":\"~/project\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "workspace-a").commit();
        RuntimeEnvironment.getApplication().getSharedPreferences(
            "termuxpro_custom_commands", 0).edit().clear().commit();
    }

    @Test
    public void showsExplicitTargetAndEmptyCreationPath() {
        CustomCommandsActivity activity = Robolectric.buildActivity(
            CustomCommandsActivity.class).setup().get();

        assertEquals("移动端", ((TextView) activity.findViewById(
            R.id.custom_commands_target)).getText().toString());
        assertTrue(((TextView) activity.findViewById(R.id.custom_commands_target_details))
            .getText().toString().contains("hdr@192.168.1.153:22"));
        assertEquals(View.VISIBLE, activity.findViewById(R.id.custom_commands_empty).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(
            R.id.custom_commands_template_hint).getVisibility());

        activity.findViewById(R.id.custom_commands_add).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(activity.getColor(R.color.tp_primary),
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
        assertEquals(activity.getColor(R.color.tp_text_secondary),
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
        ((EditText) dialog.findViewById(R.id.custom_command_name_input)).setText("查看状态");
        ((EditText) dialog.findViewById(R.id.custom_command_value_input)).setText("git status --short");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout list = activity.findViewById(R.id.custom_commands_list);
        assertEquals(1, list.getChildCount());
        assertEquals(View.GONE, activity.findViewById(R.id.custom_commands_empty).getVisibility());
        assertEquals(View.GONE, activity.findViewById(
            R.id.custom_commands_template_hint).getVisibility());
    }

    @Test
    public void templatesPrefillEditorWithoutSavingOrExecuting() {
        CustomCommandsActivity activity = Robolectric.buildActivity(
            CustomCommandsActivity.class).setup().get();

        activity.findViewById(R.id.custom_commands_templates).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog templateDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(templateDialog);
        assertEquals(activity.getString(R.string.custom_commands_template_title),
            shadowOf(templateDialog).getTitle());
        ListView listView = templateDialog.getListView();
        assertTrue(listView.getAdapter().getCount() >= 7);
        assertTrue(listView.getAdapter().getItem(0).toString().contains("codex resume"));
        assertTrue(listView.getAdapter().getItem(1).toString().contains("claude"));
        assertFalse(listView.getAdapter().getItem(1).toString().contains("--continue"));
        assertTrue(listView.getAdapter().getItem(2).toString().contains("claude --resume"));
        assertEquals(0, new CustomCommandStore(RuntimeEnvironment.getApplication())
            .list("workspace-a").size());
        assertEquals(null, shadowOf(activity).getNextStartedActivity());

        listView.performItemClick(listView.getAdapter().getView(0, null, listView), 0,
            listView.getAdapter().getItemId(0));
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog editor = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(editor);
        assertEquals(activity.getString(R.string.custom_commands_create_title),
            shadowOf(editor).getTitle());
        assertEquals("Codex：打开历史会话", ((EditText) editor.findViewById(
            R.id.custom_command_name_input)).getText().toString());
        assertEquals("codex resume", ((EditText) editor.findViewById(
            R.id.custom_command_value_input)).getText().toString());
        assertEquals("AI", ((EditText) editor.findViewById(
            R.id.custom_command_group_input)).getText().toString());
        assertEquals(0, new CustomCommandStore(RuntimeEnvironment.getApplication())
            .list("workspace-a").size());

        editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, new CustomCommandStore(RuntimeEnvironment.getApplication())
            .list("workspace-a").size());
        assertEquals(null, shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void previewNamesTargetAndStartsNewSshSessionOnlyAfterConfirmation() {
        CustomCommandStore store = new CustomCommandStore(RuntimeEnvironment.getApplication());
        store.save("workspace-a", CustomCommand.create("查看状态", "git status --short", "", "Git",
            CustomCommand.Confirmation.ALWAYS));
        CustomCommandsActivity activity = Robolectric.buildActivity(
            CustomCommandsActivity.class).setup().get();
        LinearLayout list = activity.findViewById(R.id.custom_commands_list);

        list.getChildAt(0).findViewById(R.id.custom_command_run).performClick();
        AlertDialog preview = ShadowAlertDialog.getLatestAlertDialog();
        String message = ((TextView) preview.findViewById(android.R.id.message))
            .getText().toString();
        assertTrue(message.contains("hdr@192.168.1.153:22"));
        assertTrue(message.contains("~/project"));
        assertTrue(message.contains("git status --short"));
        assertEquals(null, shadowOf(activity).getNextStartedActivity());

        preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(TermuxActivity.class.getName(), intent.getComponent().getClassName());
        assertTrue(intent.getBooleanExtra(TermuxActivity.EXTRA_NEW_SESSION, false));
        assertTrue(intent.getStringExtra(TermuxActivity.EXTRA_STARTUP_COMMAND)
            .contains("hdr@192.168.1.153"));
    }

    @Test
    public void safeCommandCanRunDirectlyWhenConfiguredForDangerousOnly() {
        CustomCommandStore store = new CustomCommandStore(RuntimeEnvironment.getApplication());
        store.save("workspace-a", new CustomCommand("safe", "查看状态", "git status --short", "",
            "Git", true, CustomCommand.Confirmation.DANGEROUS_ONLY));
        CustomCommandsActivity activity = Robolectric.buildActivity(
            CustomCommandsActivity.class).setup().get();
        LinearLayout list = activity.findViewById(R.id.custom_commands_list);

        assertEquals("git status --short", ((TextView) list.getChildAt(0)
            .findViewById(R.id.custom_command_value)).getText().toString());
        assertEquals(activity.getString(R.string.custom_commands_run_now),
            ((TextView) list.getChildAt(0).findViewById(R.id.custom_command_run)).getText().toString());
        list.getChildAt(0).findViewById(R.id.custom_command_run).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(intent);
        assertTrue(intent.getStringExtra(TermuxActivity.EXTRA_STARTUP_COMMAND)
            .contains("git status --short"));
    }

    @Test
    public void invalidWorkspaceDisablesCreation() {
        RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, 0).edit().clear().commit();
        CustomCommandsActivity activity = Robolectric.buildActivity(
            CustomCommandsActivity.class).setup().get();

        assertFalse(activity.findViewById(R.id.custom_commands_add).isEnabled());
        assertFalse(activity.findViewById(R.id.custom_commands_templates).isEnabled());
        assertEquals(activity.getString(R.string.custom_commands_invalid_workspace),
            ((TextView) activity.findViewById(R.id.custom_commands_empty)).getText().toString());
        assertEquals(View.GONE, activity.findViewById(
            R.id.custom_commands_template_hint).getVisibility());
    }
}
