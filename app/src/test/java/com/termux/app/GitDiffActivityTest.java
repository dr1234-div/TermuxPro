package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public final class GitDiffActivityTest {

    @Test
    public void branchDialogsUseReadableTermuxProStyleAndOfferRemoteTracking() throws Exception {
        Intent intent = GitDiffActivity.newIntent(RuntimeEnvironment.getApplication(),
                "hdr@192.168.1.153", 22, "~/repo")
            .putExtra(GitDiffActivity.EXTRA_UI_TEST_OVERVIEW, "TP_OVERVIEW\tdev\t0\t2\t\t\t0\n"
                + "TP_LOCAL\tdev\n"
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
        assertEquals(2, branches.getListView().getAdapter().getCount());
        assertTrue(((TextView) branches.getListView().getAdapter().getView(1, null,
            branches.getListView())).getText().toString().contains("origin/feature/mobile"));

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
    }
}
