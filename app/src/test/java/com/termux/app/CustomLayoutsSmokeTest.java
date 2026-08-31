package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
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
            R.style.Theme_TermuxPro_DayNight_NoActionBar);
        int[] layouts = {
            R.layout.activity_workspace,
            R.layout.activity_git_diff,
            R.layout.activity_remote_files,
            R.layout.activity_remote_file_preview,
            R.layout.activity_project_tasks,
            R.layout.activity_connection_diagnostic,
            R.layout.activity_ssh_keys,
            R.layout.activity_task_sessions,
            R.layout.activity_custom_commands
        };
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int layout : layouts) {
            FrameLayout parent = new FrameLayout(context);
            View view = inflater.inflate(layout, parent, false);
            assertNotNull(view);
        }
        Context terminalContext = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        assertNotNull(LayoutInflater.from(terminalContext).inflate(
            R.layout.activity_termux, new FrameLayout(terminalContext), false));
        assertNotNull(LayoutInflater.from(context).inflate(
            R.layout.dialog_tmux_session_name, new FrameLayout(context), false));
    }

    @Test
    public void sharedListItemUsesExplicitReadableTextColorAndTouchHeight() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxPro_DayNight_NoActionBar);
        TextView item = (TextView) LayoutInflater.from(context)
            .inflate(R.layout.item_termuxpro_list, new FrameLayout(context), false);

        assertEquals(context.getColor(R.color.tp_text_primary), item.getCurrentTextColor());
        float density = context.getResources().getDisplayMetrics().density;
        assertEquals(56, Math.round(item.getMinHeight() / density));
    }

    @Test
    public void terminalFeedbackUsesExplicitReadableColorsAndStartsHidden() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View page = LayoutInflater.from(context).inflate(
            R.layout.activity_termux, new FrameLayout(context), false);
        TextView feedback = page.findViewById(R.id.terminal_feedback_banner);

        assertNotNull(feedback.getBackground());
        assertEquals(context.getColor(R.color.tp_text_primary), feedback.getCurrentTextColor());
        assertEquals(View.GONE, feedback.getVisibility());
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,
            feedback.getAccessibilityLiveRegion());
    }

    @Test
    public void terminalNavigationUsesPersistentLabelsAndAccessibleTargets() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View page = LayoutInflater.from(context).inflate(
            R.layout.activity_termux, new FrameLayout(context), false);
        TextView workbench = page.findViewById(R.id.workspace_home_button);
        TextView sessions = page.findViewById(R.id.workspace_drawer_button);
        ImageButton settings = page.findViewById(R.id.settings_button);
        float density = context.getResources().getDisplayMetrics().density;

        assertEquals(context.getString(R.string.workspace_open_workbench),
            workbench.getText().toString());
        assertEquals(context.getString(R.string.workspace_open_workbench_description),
            workbench.getContentDescription().toString());
        assertEquals(context.getString(R.string.workspace_open_sessions),
            sessions.getContentDescription().toString());
        assertEquals(context.getString(R.string.workspace_sessions_short),
            sessions.getText().toString());
        assertTrue(settings.getLayoutParams().width >= Math.round(48 * density));
        assertTrue(settings.getLayoutParams().height >= Math.round(48 * density));
    }

    @Test
    public void toolbarsAndWorkspaceActionsRemainUsableWithLargeFonts() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxPro_DayNight_NoActionBar);
        LayoutInflater inflater = LayoutInflater.from(context);
        int[] layouts = {
            R.layout.activity_git_diff,
            R.layout.activity_remote_files,
            R.layout.activity_remote_file_preview,
            R.layout.activity_project_tasks,
            R.layout.activity_connection_diagnostic,
            R.layout.activity_task_sessions,
            R.layout.activity_custom_commands
        };
        int[] refreshButtons = {
            R.id.git_diff_refresh_button,
            R.id.remote_files_refresh_button,
            R.id.remote_file_refresh_button,
            R.id.project_tasks_refresh_button,
            R.id.connection_diagnostic_refresh_button,
            R.id.task_sessions_refresh_button,
            R.id.custom_commands_add
        };
        float density = context.getResources().getDisplayMetrics().density;
        for (int index = 0; index < layouts.length; index++) {
            View page = inflater.inflate(layouts[index], new FrameLayout(context), false);
            View refresh = page.findViewById(refreshButtons[index]);
            if (index == 0 || index == layouts.length - 1) {
                assertTrue(refresh instanceof TextView);
                assertTrue(refresh.getMinimumWidth() >= Math.round(48 * density));
            } else {
                assertTrue(refresh instanceof ImageButton);
                assertEquals(48, Math.round(refresh.getLayoutParams().width / density));
            }
            if (index == layouts.length - 1) {
                assertTrue(refresh.getMinimumHeight() >= Math.round(48 * density));
                assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,
                    refresh.getLayoutParams().height);
            } else {
                assertEquals(48, Math.round(refresh.getLayoutParams().height / density));
            }
            View toolbar = (View) refresh.getParent();
            assertEquals(56, Math.round(toolbar.getMinimumHeight() / density));
            assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, toolbar.getLayoutParams().height);
        }
        View sessions = inflater.inflate(R.layout.activity_task_sessions, new FrameLayout(context), false);
        View createSession = sessions.findViewById(R.id.task_sessions_create_button);
        assertTrue(createSession instanceof TextView);
        assertTrue(createSession.getMinimumHeight() >= Math.round(48 * density));

        View workspace = inflater.inflate(R.layout.activity_workspace, new FrameLayout(context), false);
        LinearLayout actions = workspace.findViewById(R.id.workspace_management_actions);
        assertEquals(LinearLayout.VERTICAL, actions.getOrientation());
        View workspaceSelector = workspace.findViewById(R.id.workspace_selector);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,
            workspaceSelector.getLayoutParams().height);
        assertTrue(workspaceSelector.getMinimumHeight() >= Math.round(52 * density));
        assertNotNull(workspace.findViewById(R.id.workspace_connection_policy_selector));
        assertNotNull(workspace.findViewById(R.id.workspace_session_name_input));
    }

    @Test
    public void toolbarNavigationAndActionsStayInsideNarrowScreenAtTwoHundredPercentFont() {
        Configuration configuration = new Configuration(
            RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.fontScale = 2f;
        Context scaled = RuntimeEnvironment.getApplication().createConfigurationContext(configuration);
        Context context = new ContextThemeWrapper(scaled, R.style.Theme_TermuxPro_DayNight_NoActionBar);
        int[] layouts = {
            R.layout.activity_git_diff,
            R.layout.activity_remote_files,
            R.layout.activity_remote_file_preview,
            R.layout.activity_project_tasks,
            R.layout.activity_connection_diagnostic,
            R.layout.activity_task_sessions,
            R.layout.activity_custom_commands
        };
        int[] backButtons = {
            R.id.git_diff_back_button,
            R.id.remote_files_back_button,
            R.id.remote_file_back_button,
            R.id.project_tasks_back_button,
            R.id.connection_diagnostic_back_button,
            R.id.task_sessions_back_button,
            R.id.custom_commands_back
        };
        int[] actionButtons = {
            R.id.git_diff_refresh_button,
            R.id.remote_files_refresh_button,
            R.id.remote_file_refresh_button,
            R.id.project_tasks_refresh_button,
            R.id.connection_diagnostic_refresh_button,
            R.id.task_sessions_refresh_button,
            R.id.custom_commands_add
        };
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(205 * density);
        int height = Math.round(640 * density);
        for (int index = 0; index < layouts.length; index++) {
            View page = LayoutInflater.from(context).inflate(
                layouts[index], new FrameLayout(context), false);
            page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            page.layout(0, 0, width, height);
            View back = page.findViewById(backButtons[index]);
            View action = page.findViewById(actionButtons[index]);
            assertTrue("返回操作不能被超大字体挤出屏幕", back.getLeft() >= 0 && back.getRight() <= width);
            assertTrue("工具栏操作不能被超大字体挤出屏幕",
                action.getLeft() >= 0 && action.getRight() <= width);
            assertEquals(View.VISIBLE, back.getVisibility());
            assertEquals(View.VISIBLE, action.getVisibility());
            if (layouts[index] == R.layout.activity_custom_commands) {
                TextView title = page.findViewById(R.id.custom_commands_title);
                assertEquals("快捷指令标题在超大字体下仍保持单行", 1, title.getMaxLines());
                assertEquals("快捷指令标题应按可用宽度缩放", TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                    title.getAutoSizeTextType());
            }
        }
    }
}
