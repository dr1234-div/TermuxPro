package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.termux.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class AiCliSessionCenterActivityTest {

    @Before
    public void setUp() {
        RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void showsSafeEmptyStateWithoutReadingPrivateAiHistory() {
        AiCliSessionCenterActivity activity = Robolectric.buildActivity(
            AiCliSessionCenterActivity.class).setup().get();

        assertEquals("AI CLI 会话中心", text(activity, R.id.ai_cli_center_title));
        assertEquals("未选择有效远程工作区", text(activity, R.id.ai_cli_center_target));
        assertTrue(text(activity, R.id.ai_cli_center_target_detail).contains("请先回到工作台"));
        assertTrue(text(activity, R.id.ai_cli_center_summary).contains("不读取 AI 私有历史"));
        assertTrue(text(activity, R.id.ai_cli_center_claude_commands).contains("claude --resume"));
        assertTrue(text(activity, R.id.ai_cli_center_codex_commands).contains("codex resume"));

        activity.findViewById(R.id.ai_cli_center_claude_new).performClick();
        assertNextActivity(activity, WorkspaceActivity.class);
    }

    @Test
    public void showsActiveWorkspaceAndRoutesToValueAddedTools() {
        RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"workspace-a\",\"name\":\"远程开发\",\"host\":\"hdr@192.168.1.153\",\"port\":\"22\",\"path\":\"~/project\",\"remotePort\":\"5173\",\"localPort\":\"5173\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "workspace-a")
            .commit();
        AiCliSessionCenterActivity activity = Robolectric.buildActivity(
            AiCliSessionCenterActivity.class).setup().get();

        assertEquals("远程开发", text(activity, R.id.ai_cli_center_target));
        assertEquals("hdr@192.168.1.153:22 · ~/project",
            text(activity, R.id.ai_cli_center_target_detail));

        activity.findViewById(R.id.ai_cli_center_open_workspace).performClick();
        assertNextActivity(activity, WorkspaceActivity.class);

        activity.findViewById(R.id.ai_cli_center_open_templates).performClick();
        assertNextActivity(activity, CustomCommandsActivity.class);

        activity.findViewById(R.id.ai_cli_center_open_tmux).performClick();
        assertNextActivity(activity, TaskSessionsActivity.class);

        activity.findViewById(R.id.ai_cli_center_open_git).performClick();
        Intent gitIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(GitDiffActivity.class.getName(), gitIntent.getComponent().getClassName());
        assertTrue(!gitIntent.getBooleanExtra(GitDiffActivity.EXTRA_START_IN_DIFF, true));
    }

    @Test
    public void aiActionsOpenIndependentSshOnlyTerminalWithoutAutoTmux() {
        RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"workspace-a\",\"name\":\"远程开发\",\"host\":\"hdr@192.168.1.153\",\"port\":\"22\",\"path\":\"~/project\",\"remotePort\":\"5173\",\"localPort\":\"5173\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "workspace-a")
            .commit();
        AiCliSessionCenterActivity activity = Robolectric.buildActivity(
            AiCliSessionCenterActivity.class).setup().get();

        activity.findViewById(R.id.ai_cli_center_claude_history).performClick();

        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(TermuxActivity.class.getName(), intent.getComponent().getClassName());
        Bundle extras = intent.getExtras();
        assertTrue(extras.getBoolean(TermuxActivity.EXTRA_NEW_SESSION));
        String startup = extras.getString(TermuxActivity.EXTRA_STARTUP_COMMAND);
        assertTrue(startup.contains("claude --resume"));
        assertTrue(startup.contains("ssh -t"));
        assertTrue(startup.contains("hdr@192.168.1.153"));
        assertTrue(startup.contains("exec claude --resume"));
        assertTrue(!startup.contains("tmux attach-session"));
        assertTrue(!startup.contains("tmux new-session"));
    }

    @Test
    public void tmuxActionFallsBackToWorkbenchWhenWorkspaceIsIncomplete() {
        AiCliSessionCenterActivity activity = Robolectric.buildActivity(
            AiCliSessionCenterActivity.class).setup().get();

        activity.findViewById(R.id.ai_cli_center_open_tmux).performClick();

        assertNextActivity(activity, WorkspaceActivity.class);
    }

    @Test
    public void gitActionFallsBackToWorkbenchWhenWorkspaceIsIncomplete() {
        AiCliSessionCenterActivity activity = Robolectric.buildActivity(
            AiCliSessionCenterActivity.class).setup().get();

        activity.findViewById(R.id.ai_cli_center_open_git).performClick();

        assertNextActivity(activity, WorkspaceActivity.class);
    }

    private static String text(AiCliSessionCenterActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }

    private static void assertNextActivity(AiCliSessionCenterActivity activity,
                                           Class<?> expectedClass) {
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedClass.getName(), intent.getComponent().getClassName());
    }
}
