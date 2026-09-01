package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public final class GitDiffActivityTest {

    @Test
    public void branchDialogsUseReadableTermuxProStyleAndOfferRemoteTracking() throws Exception {
        Intent intent = GitDiffActivity.newIntent(RuntimeEnvironment.getApplication(),
                "hdr@192.168.1.153", 22, "~/repo")
            .putExtra(GitDiffActivity.EXTRA_UI_TEST_OVERVIEW, "TP_OVERVIEW\tdev\t0\t2\t1\t1\t\t\t0\n"
                + "TP_LOCAL\tdev\n"
                + "TP_LOCAL\tmobile-ui\n"
                + "TP_REMOTE\torigin/feature/mobile\n"
                + "TP_REMOTE\torigin/HEAD\n");
        GitDiffActivity activity = Robolectric.buildActivity(GitDiffActivity.class, intent)
            .setup().get();
        activity.showOverviewForTesting("~/repo", intent.getStringExtra(
            GitDiffActivity.EXTRA_UI_TEST_OVERVIEW));

        AlertDialog branches = activity.createBranchesDialog();
        assertNotNull(branches);
        activity.showStyledDialog(branches);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activity.getColor(R.color.tp_text_secondary),
            branches.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
        assertEquals(3, branches.getListView().getAdapter().getCount());
        assertTrue(((TextView) branches.getListView().getAdapter().getView(2, null,
            branches.getListView())).getText().toString().contains("origin/feature/mobile"));

        AlertDialog deleteBranches = activity.createDeleteBranchDialog();
        assertNotNull(deleteBranches);
        activity.showStyledDialog(deleteBranches);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, deleteBranches.getListView().getAdapter().getCount());
        assertTrue(((TextView) deleteBranches.getListView().getAdapter().getView(0, null,
            deleteBranches.getListView())).getText().toString().contains("mobile-ui"));
        assertTrue(!((TextView) deleteBranches.getListView().getAdapter().getView(0, null,
            deleteBranches.getListView())).getText().toString().contains("dev"));

        AlertDialog remote = activity.createTrackRemoteBranchDialog("origin/feature/mobile");
        assertNotNull(remote);
        activity.showStyledDialog(remote);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activity.getString(R.string.git_workbench_track_remote_action),
            remote.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(activity.getColor(R.color.tp_primary),
            remote.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
        assertEquals(activity.getColor(R.color.tp_text_secondary),
            remote.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
        assertTrue(((TextView) remote.findViewById(android.R.id.message)).getText().toString()
            .contains("2 个未提交文件"));

        activity.confirmDeleteLocalBranch("mobile-ui");
        AlertDialog confirmDelete = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(confirmDelete);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activity.getString(R.string.git_workbench_delete_branch_action),
            confirmDelete.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(activity.getColor(R.color.tp_danger),
            confirmDelete.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
        assertTrue(((TextView) confirmDelete.findViewById(android.R.id.message)).getText()
            .toString().contains("不会删除远端分支"));
    }

    @Test
    public void newBranchDialogKeepsReadableStyleAndValidatesInput() {
        Intent intent = GitDiffActivity.newIntent(RuntimeEnvironment.getApplication(),
                "hdr@192.168.1.153", 22, "~/repo")
            .putExtra(GitDiffActivity.EXTRA_UI_TEST_OVERVIEW, "TP_OVERVIEW\tdev\t0\t2\t0\t2\t\t\t0\n"
                + "TP_LOCAL\tdev\n");
        GitDiffActivity activity = Robolectric.buildActivity(GitDiffActivity.class, intent)
            .setup().get();

        AlertDialog dialog = activity.createNewBranchDialog();
        assertNotNull(dialog);
        dialog.show();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activity.getString(R.string.git_workbench_create_branch_action),
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(activity.getColor(R.color.tp_primary),
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
        assertTrue(((TextView) dialog.findViewById(android.R.id.message)).getText().toString()
            .contains("2 个未提交文件"));

        EditText input = dialog.findViewById(android.R.id.edit);
        assertNotNull(input);
        input.setText("bad branch");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertNotNull(input.getError());
        assertTrue(dialog.isShowing());
        dialog.dismiss();
    }

    @Test
    public void overviewShowsIndexSplitAndDisablesUnavailableIndexActions() {
        Intent intent = GitDiffActivity.newIntent(RuntimeEnvironment.getApplication(),
                "hdr@192.168.1.153", 22, "~/repo")
            .putExtra(GitDiffActivity.EXTRA_UI_TEST_OVERVIEW, "TP_OVERVIEW\tdev\t0\t3\t2\t1\t4\t1\t1\torigin/dev\n"
                + "TP_LOCAL\tdev\n");
        GitDiffActivity activity = Robolectric.buildActivity(GitDiffActivity.class, intent)
            .setup().get();

        TextView index = activity.findViewById(R.id.git_overview_index_state);
        TextView sync = activity.findViewById(R.id.git_overview_sync);
        assertEquals("hdr@192.168.1.153:22 · ~/repo",
            ((TextView) activity.findViewById(R.id.git_overview_path)).getText().toString());
        assertTrue(index.getText().toString().contains("已暂存 2"));
        assertTrue(index.getText().toString().contains("未暂存 1"));
        assertTrue(sync.getText().toString().contains("跟踪 origin/dev"));
        assertTrue(sync.getText().toString().contains("领先 4"));
        assertTrue(sync.getText().toString().contains("落后 1"));
        assertTrue(((Button) activity.findViewById(R.id.git_overview_fetch_button)).isEnabled());
        assertTrue(((Button) activity.findViewById(R.id.git_overview_pull_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_push_button)).isEnabled());
        assertTrue(((Button) activity.findViewById(R.id.git_overview_stage_all_button)).isEnabled());
        assertTrue(((Button) activity.findViewById(R.id.git_overview_unstage_all_button)).isEnabled());
        assertTrue(((Button) activity.findViewById(R.id.git_overview_commit_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_delete_branch_button)).isEnabled());

        activity.showOverviewForTesting("~/repo", "TP_OVERVIEW\tdev\t0\t0\t0\t0\t2\t0\t1\torigin/dev\n"
            + "TP_LOCAL\tdev\n"
            + "TP_LOCAL\tmobile-ui\n");
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_pull_button)).isEnabled());
        assertTrue(((Button) activity.findViewById(R.id.git_overview_push_button)).isEnabled());
        assertTrue(((Button) activity.findViewById(R.id.git_overview_delete_branch_button)).isEnabled());

        activity.showOverviewForTesting("~/repo", "TP_OVERVIEW\tdev\t0\t0\t0\t0\t\t\t0\n");
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_fetch_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_pull_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_push_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_stage_all_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_unstage_all_button)).isEnabled());
        assertTrue(!((Button) activity.findViewById(R.id.git_overview_commit_button)).isEnabled());
    }

    @Test
    public void commitDialogExplainsScopeAndValidatesMessage() {
        Intent intent = GitDiffActivity.newIntent(RuntimeEnvironment.getApplication(),
                "hdr@192.168.1.153", 22, "~/repo")
            .putExtra(GitDiffActivity.EXTRA_UI_TEST_OVERVIEW, "TP_OVERVIEW\tdev\t0\t3\t2\t1\t\t\t0\n"
                + "TP_LOCAL\tdev\n");
        GitDiffActivity activity = Robolectric.buildActivity(GitDiffActivity.class, intent)
            .setup().get();

        AlertDialog dialog = activity.createCommitDialog();
        assertNotNull(dialog);
        dialog.show();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activity.getString(R.string.git_workbench_commit_action),
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(activity.getColor(R.color.tp_primary),
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
        assertTrue(((TextView) dialog.findViewById(android.R.id.message)).getText().toString()
            .contains("2 个已暂存文件"));
        assertTrue(((TextView) dialog.findViewById(android.R.id.message)).getText().toString()
            .contains("1 个未暂存文件不会进入本次提交"));

        EditText input = dialog.findViewById(android.R.id.edit);
        assertNotNull(input);
        input.setText("");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertNotNull(input.getError());
        assertTrue(dialog.isShowing());
        dialog.dismiss();
    }
}
