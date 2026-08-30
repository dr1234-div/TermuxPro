package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** 验证远程工具失败时使用可读状态和就地恢复动作，不暴露内部退出码。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class RemoteToolRecoveryTest {

    @Test
    public void gitDiffInvalidWorkspaceShowsReadableRecoveryState() {
        Intent intent = GitDiffActivity.newIntent(
            RuntimeEnvironment.getApplication(), "invalid", 0, "~/project");
        GitDiffActivity activity = Robolectric.buildActivity(GitDiffActivity.class, intent).setup().get();

        assertRecoveryState(activity.findViewById(R.id.git_diff_status_state),
            activity.findViewById(R.id.git_diff_scroll),
            activity.findViewById(R.id.git_diff_status_message),
            activity.findViewById(R.id.git_diff_return_workspace_button));
        activity.findViewById(R.id.git_diff_return_workspace_button).performClick();
        assertTrue(activity.isFinishing());
        assertEquals(WorkspaceActivity.class.getName(),
            shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @Test
    public void remoteFileInvalidWorkspaceShowsReadableRecoveryState() {
        RemoteFilePreviewActivity activity = Robolectric.buildActivity(
            RemoteFilePreviewActivity.class, new Intent()).setup().get();

        assertRecoveryState(activity.findViewById(R.id.remote_file_status_state),
            activity.findViewById(R.id.remote_file_scroll),
            activity.findViewById(R.id.remote_file_status_message),
            activity.findViewById(R.id.remote_file_return_workspace_button));
    }

    @Test
    public void remoteFilesInvalidWorkspaceShowsReadableRecoveryState() {
        RemoteFilesActivity activity = Robolectric.buildActivity(
            RemoteFilesActivity.class, new Intent()).setup().get();

        assertRecoveryState(activity.findViewById(R.id.remote_files_status_state),
            activity.findViewById(R.id.remote_files_list),
            activity.findViewById(R.id.remote_files_status_message),
            activity.findViewById(R.id.remote_files_return_workspace_button));
    }

    private void assertRecoveryState(View state, View content, TextView message, View recovery) {
        assertTrue(state.getVisibility() == View.VISIBLE);
        assertTrue(content.getVisibility() == View.GONE);
        assertTrue(recovery.getVisibility() == View.VISIBLE);
        assertFalse(message.getText().toString().isEmpty());
        assertFalse(message.getText().toString().contains("退出码"));
    }
}
