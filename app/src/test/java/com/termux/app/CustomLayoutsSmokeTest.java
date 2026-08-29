package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** 在 Android 资源运行时逐页膨胀自定义界面，捕获 aapt 无法发现的 Drawable/主题错误。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class CustomLayoutsSmokeTest {

    @Test
    public void allCustomActivityLayoutsInflateWithProductionTheme() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        int[] layouts = {
            R.layout.activity_workspace,
            R.layout.activity_git_diff,
            R.layout.activity_remote_files,
            R.layout.activity_remote_file_preview,
            R.layout.activity_project_tasks,
            R.layout.activity_connection_diagnostic,
            R.layout.activity_ssh_keys,
            R.layout.activity_task_sessions,
            R.layout.activity_termux
        };
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int layout : layouts) {
            FrameLayout parent = new FrameLayout(context);
            View view = inflater.inflate(layout, parent, false);
            assertNotNull(view);
        }
    }

    @Test
    public void sharedListItemUsesExplicitReadableTextColorAndTouchHeight() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        TextView item = (TextView) LayoutInflater.from(context)
            .inflate(R.layout.item_termuxpro_list, new FrameLayout(context), false);

        assertEquals(context.getColor(R.color.tp_text_primary), item.getCurrentTextColor());
        float density = context.getResources().getDisplayMetrics().density;
        assertEquals(56, Math.round(item.getMinHeight() / density));
    }

    @Test
    public void toolbarsAndWorkspaceActionsRemainUsableWithLargeFonts() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        LayoutInflater inflater = LayoutInflater.from(context);
        int[] layouts = {
            R.layout.activity_git_diff,
            R.layout.activity_remote_files,
            R.layout.activity_remote_file_preview,
            R.layout.activity_project_tasks,
            R.layout.activity_connection_diagnostic,
            R.layout.activity_task_sessions
        };
        int[] refreshButtons = {
            R.id.git_diff_refresh_button,
            R.id.remote_files_refresh_button,
            R.id.remote_file_refresh_button,
            R.id.project_tasks_refresh_button,
            R.id.connection_diagnostic_refresh_button,
            R.id.task_sessions_refresh_button
        };
        float density = context.getResources().getDisplayMetrics().density;
        for (int index = 0; index < layouts.length; index++) {
            View page = inflater.inflate(layouts[index], new FrameLayout(context), false);
            View refresh = page.findViewById(refreshButtons[index]);
            assertTrue(refresh instanceof ImageButton);
            assertEquals(48, Math.round(refresh.getLayoutParams().width / density));
            assertEquals(48, Math.round(refresh.getLayoutParams().height / density));
            View toolbar = (View) refresh.getParent();
            assertEquals(56, Math.round(toolbar.getMinimumHeight() / density));
            assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, toolbar.getLayoutParams().height);
        }

        View workspace = inflater.inflate(R.layout.activity_workspace, new FrameLayout(context), false);
        LinearLayout actions = workspace.findViewById(R.id.workspace_management_actions);
        assertEquals(LinearLayout.VERTICAL, actions.getOrientation());
        assertNotNull(workspace.findViewById(R.id.workspace_connection_policy_selector));
        assertNotNull(workspace.findViewById(R.id.workspace_session_name_input));
    }
}
