package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.EditText;


import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class TaskSessionsActivityTest {
    @Test
    public void previewShowsOwnedAndUnknownSessionsWithDiscoverableCreateAction() {
        Intent intent = TaskSessionsActivity.newIntent(RuntimeEnvironment.getApplication(),
            "dev@example.com", 22, "~/project", "11111111-2222-3333-4444-555555555555")
            .putExtra(TaskSessionsActivity.EXTRA_UI_TEST_SESSIONS, true);
        TaskSessionsActivity activity = Robolectric.buildActivity(TaskSessionsActivity.class, intent)
            .setup().get();

        ListView sessions = activity.findViewById(R.id.task_sessions_list);
        View create = activity.findViewById(R.id.task_sessions_create_button);
        assertEquals(2, sessions.getAdapter().getCount());
        assertEquals(View.VISIBLE, create.getVisibility());

        create.performClick();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        assertNotNull(dialog.findViewById(R.id.task_session_name_input));
    }

    @Test
    public void invalidNameKeepsCreateDialogOpenWithInlineError() {
        TaskSessionsActivity activity = previewActivity();
        activity.findViewById(R.id.task_sessions_create_button).performClick();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        EditText input = dialog.findViewById(R.id.task_session_name_input);
        input.setText("bad name");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        assertTrue(dialog.isShowing());
    }

    @Test
    public void ownedSessionSeparatesDangerActionAndUnknownSessionHasAttachOnly() {
        TaskSessionsActivity activity = previewActivity();
        ListView sessions = activity.findViewById(R.id.task_sessions_list);

        sessions.performItemClick(sessions.getAdapter().getView(0, null, sessions), 0, 0);
        AlertDialog owned = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals(2, owned.getListView().getAdapter().getCount());
        assertEquals(activity.getString(R.string.task_sessions_stop),
            owned.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());
        owned.dismiss();

        sessions.performItemClick(sessions.getAdapter().getView(1, null, sessions), 1, 1);
        AlertDialog unknown = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals(1, unknown.getListView().getAdapter().getCount());
        assertTrue(unknown.getButton(AlertDialog.BUTTON_NEUTRAL) == null
            || unknown.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility() != View.VISIBLE
            || unknown.getButton(AlertDialog.BUTTON_NEUTRAL).getText().length() == 0);
        assertTrue(((android.widget.TextView) unknown.findViewById(android.R.id.message))
            .getText().toString().contains("只允许进入"));
    }

    private TaskSessionsActivity previewActivity() {
        Intent intent = TaskSessionsActivity.newIntent(RuntimeEnvironment.getApplication(),
            "dev@example.com", 22, "~/project", "11111111-2222-3333-4444-555555555555")
            .putExtra(TaskSessionsActivity.EXTRA_UI_TEST_SESSIONS, true);
        return Robolectric.buildActivity(TaskSessionsActivity.class, intent).setup().get();
    }
}
