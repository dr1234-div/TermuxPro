package com.termux.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.termux.R;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 通过已认证的 OpenSSH 复用连接提供 Git 概览、分支切换、修改审查和提交记录。 */
public final class GitDiffActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PATH = "path";
    static final String EXTRA_START_IN_DIFF = "start_in_diff";
    static final String EXTRA_UI_TEST_OVERVIEW = "ui_test_overview";
    private static final int MAX_OUTPUT_BYTES = 1_500_000;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private TextView mContent;
    private TextView mStatusMessage;
    private ProgressBar mProgress;
    private View mContentScroll;
    private View mOverviewScroll;
    private View mStatusState;
    private View mReturnWorkspace;
    private GitRepositoryOverview mOverview;
    private Mode mMode = Mode.OVERVIEW;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull String host, int port,
                                   @NonNull String path) {
        return new Intent(context, GitDiffActivity.class)
            .putExtra(EXTRA_HOST, host)
            .putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_PATH, path);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_git_diff);
        mContent = findViewById(R.id.git_diff_content);
        mContentScroll = findViewById(R.id.git_diff_scroll);
        mOverviewScroll = findViewById(R.id.git_overview_scroll);
        mStatusState = findViewById(R.id.git_diff_status_state);
        mStatusMessage = findViewById(R.id.git_diff_status_message);
        mReturnWorkspace = findViewById(R.id.git_diff_return_workspace_button);
        mProgress = findViewById(R.id.git_diff_progress);
        findViewById(R.id.git_diff_back_button).setOnClickListener(view -> navigateBack());
        findViewById(R.id.git_diff_refresh_button).setOnClickListener(view -> refreshCurrentMode());
        findViewById(R.id.git_overview_branches_button).setOnClickListener(view -> showBranches());
        findViewById(R.id.git_overview_create_branch_button).setOnClickListener(
            view -> showCreateBranchDialog());
        findViewById(R.id.git_overview_delete_branch_button).setOnClickListener(
            view -> showDeleteBranchDialog());
        findViewById(R.id.git_overview_fetch_button).setOnClickListener(view -> fetchUpstream());
        findViewById(R.id.git_overview_pull_button).setOnClickListener(view -> confirmPullFastForward());
        findViewById(R.id.git_overview_push_button).setOnClickListener(view -> confirmPushUpstream());
        findViewById(R.id.git_overview_changes_button).setOnClickListener(view -> loadDiff());
        findViewById(R.id.git_overview_files_button).setOnClickListener(view -> showChangedFiles());
        findViewById(R.id.git_overview_stash_button).setOnClickListener(view -> showCreateStashDialog());
        findViewById(R.id.git_overview_stashes_button).setOnClickListener(view -> showStashes());
        findViewById(R.id.git_overview_stage_all_button).setOnClickListener(
            view -> confirmStageAll());
        findViewById(R.id.git_overview_unstage_all_button).setOnClickListener(
            view -> confirmUnstageAll());
        findViewById(R.id.git_overview_commit_button).setOnClickListener(
            view -> showCommitDialog());
        findViewById(R.id.git_overview_commits_button).setOnClickListener(view -> showCommits());
        mReturnWorkspace.setOnClickListener(view -> WorkspaceNavigation.returnToWorkspace(this));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });
        String uiTestOverview = getIntent().getStringExtra(EXTRA_UI_TEST_OVERVIEW);
        if (uiTestOverview == null) {
            if (getIntent().getBooleanExtra(EXTRA_START_IN_DIFF, false)) loadDiff();
            else loadOverview();
        } else {
            String path = getIntent().getStringExtra(EXTRA_PATH);
            showOverviewForTesting(path == null ? "" : path, uiTestOverview);
        }
    }

    private void refreshCurrentMode() {
        if (mMode == Mode.DIFF) loadDiff();
        else loadOverview();
    }

    private void loadOverview() {
        ConnectionTarget target = readTarget();
        if (target == null) return;
        mMode = Mode.OVERVIEW;
        beginLoading(getString(R.string.git_workbench_loading));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(target.path), MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode != 0) {
                    showRemoteFailure(result, R.string.git_workbench_not_repository);
                    return;
                }
                if (result.truncated) {
                    showStatus(getString(R.string.git_workbench_overview_truncated), false);
                    return;
                }
                try {
                    mOverview = GitRepositoryOverview.parse(result.output);
                    showOverview(target, mOverview);
                } catch (IllegalArgumentException exception) {
                    showStatus(getString(R.string.git_workbench_invalid_response), true);
                }
            });
        });
    }

    private void loadDiff() {
        ConnectionTarget target = readTarget();
        if (target == null) return;

        mMode = Mode.DIFF;
        beginLoading(getString(R.string.git_diff_loading));
        mExecutor.execute(() -> showResultOnMain(runGitDiff(target.host, target.port, target.path)));
    }

    private void beginLoading(@NonNull String message) {
        mRunner.cancel();
        mProgress.setVisibility(View.VISIBLE);
        showStatus(message, false);
    }

    private ConnectionTarget readTarget() {
        String host = getIntent().getStringExtra(EXTRA_HOST);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        int port = getIntent().getIntExtra(EXTRA_PORT, 22);
        if (host == null || path == null || port < 1 || port > 65535) {
            showStatus(getString(R.string.git_diff_invalid_workspace), true);
            return null;
        }
        return new ConnectionTarget(host, port, path);
    }

    @NonNull
    private CommandResult runGitDiff(@NonNull String host, int port, @NonNull String path) {
        RemoteCommandRunner.Result result = mRunner.run(host, port,
            WorkspaceCommandBuilder.buildGitDiffRemoteCommand(path), MAX_OUTPUT_BYTES);
        if (result.exitCode == RemoteCommandRunner.ERROR_SSH_MISSING) {
            return new CommandResult(-1, getString(R.string.git_diff_ssh_missing), false, true);
        }
        if (result.exitCode == RemoteCommandRunner.ERROR_INTERRUPTED) {
            return new CommandResult(-1, getString(R.string.git_diff_cancelled), false, false);
        }
        if (result.exitCode == RemoteCommandRunner.ERROR_PROCESS) {
            return new CommandResult(-1, getString(R.string.git_diff_connection_error,
                result.errorType == null ? "Process" : result.errorType), false, true);
        }
        return new CommandResult(result.exitCode, result.output, result.truncated, result.exitCode != 0);
    }

    private void showResultOnMain(@NonNull CommandResult result) {
        mMainHandler.post(() -> {
            if (!isFinishing() && !isDestroyed()) showResult(result);
        });
    }

    private void showResult(@NonNull CommandResult result) {
        mProgress.setVisibility(View.GONE);
        String output = result.output;
        if (result.exitCode != 0) {
            showStatus(result.exitCode == -1 && !output.trim().isEmpty()
                ? output : getString(R.string.git_diff_failed), result.recoverable);
            return;
        } else if (output.trim().isEmpty()) {
            showStatus(getString(R.string.git_diff_clean), false);
            return;
        }
        if (result.truncated) output += "\n\n" + getString(R.string.git_diff_truncated);
        mStatusState.setVisibility(View.GONE);
        mOverviewScroll.setVisibility(View.GONE);
        mContentScroll.setVisibility(View.VISIBLE);
        mContent.setText(colorize(output));
    }

    private void showOverview(@NonNull ConnectionTarget target,
                              @NonNull GitRepositoryOverview overview) {
        mOverview = overview;
        mProgress.setVisibility(View.GONE);
        mStatusState.setVisibility(View.GONE);
        mContentScroll.setVisibility(View.GONE);
        mOverviewScroll.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.git_overview_head)).setText(getString(
            overview.detached ? R.string.git_workbench_detached : R.string.git_workbench_branch,
            overview.head));
        ((TextView) findViewById(R.id.git_overview_path)).setText(getString(
            R.string.git_workbench_target, target.host, target.port, target.path));
        ((TextView) findViewById(R.id.git_overview_changes)).setText(getResources().getQuantityString(
            R.plurals.git_workbench_changed_files, overview.changedFiles, overview.changedFiles));
        ((TextView) findViewById(R.id.git_overview_index_state)).setText(getString(
            R.string.git_workbench_index_state, overview.stagedFiles, overview.unstagedFiles));
        Button stageAll = findViewById(R.id.git_overview_stage_all_button);
        stageAll.setEnabled(overview.unstagedFiles > 0);
        stageAll.setAlpha(overview.unstagedFiles > 0 ? 1f : 0.48f);
        Button unstageAll = findViewById(R.id.git_overview_unstage_all_button);
        unstageAll.setEnabled(overview.stagedFiles > 0);
        unstageAll.setAlpha(overview.stagedFiles > 0 ? 1f : 0.48f);
        Button commit = findViewById(R.id.git_overview_commit_button);
        commit.setEnabled(overview.stagedFiles > 0);
        commit.setAlpha(overview.stagedFiles > 0 ? 1f : 0.48f);
        Button files = findViewById(R.id.git_overview_files_button);
        files.setEnabled(!overview.fileChanges.isEmpty());
        files.setAlpha(!overview.fileChanges.isEmpty() ? 1f : 0.48f);
        Button stash = findViewById(R.id.git_overview_stash_button);
        stash.setEnabled(overview.changedFiles > 0);
        stash.setAlpha(overview.changedFiles > 0 ? 1f : 0.48f);
        Button stashes = findViewById(R.id.git_overview_stashes_button);
        stashes.setEnabled(!overview.stashes.isEmpty());
        stashes.setAlpha(!overview.stashes.isEmpty() ? 1f : 0.48f);
        Button deleteBranch = findViewById(R.id.git_overview_delete_branch_button);
        boolean canDeleteBranch = !deletableLocalBranches(overview).isEmpty();
        deleteBranch.setEnabled(canDeleteBranch);
        deleteBranch.setAlpha(canDeleteBranch ? 1f : 0.48f);
        TextView sync = findViewById(R.id.git_overview_sync);
        if (overview.ahead == null || overview.behind == null) {
            sync.setText(R.string.git_workbench_no_upstream);
        } else {
            sync.setText(getString(R.string.git_workbench_sync_with_upstream,
                overview.upstream == null ? getString(R.string.git_workbench_unknown_upstream)
                    : overview.upstream,
                overview.ahead, overview.behind));
        }
        Button fetch = findViewById(R.id.git_overview_fetch_button);
        fetch.setEnabled(overview.upstream != null);
        fetch.setAlpha(overview.upstream != null ? 1f : 0.48f);
        boolean canPull = overview.upstream != null && overview.behind != null && overview.behind > 0;
        Button pull = findViewById(R.id.git_overview_pull_button);
        pull.setEnabled(canPull);
        pull.setAlpha(canPull ? 1f : 0.48f);
        boolean canPush = overview.upstream != null && overview.ahead != null
            && overview.ahead > 0 && (overview.behind == null || overview.behind == 0)
            && !overview.detached;
        Button push = findViewById(R.id.git_overview_push_button);
        push.setEnabled(canPush);
        push.setAlpha(canPush ? 1f : 0.48f);
    }

    /** 模拟器截图只注入脱敏协议数据，仍走与真实 SSH 结果相同的解析和渲染路径。 */
    void showOverviewForTesting(@NonNull String path, @NonNull String protocolOutput) {
        showOverview(overviewTarget(path), GitRepositoryOverview.parse(protocolOutput));
    }

    @NonNull
    GitRepositoryOverview mOverviewForTesting() {
        return mOverview;
    }

    private void showCommits() {
        if (mOverview == null) return;
        mMode = Mode.COMMITS;
        mOverviewScroll.setVisibility(View.GONE);
        mStatusState.setVisibility(View.GONE);
        mContentScroll.setVisibility(View.VISIBLE);
        if (mOverview.commits.isEmpty()) {
            mContent.setText(R.string.git_workbench_no_commits);
            return;
        }
        StringBuilder text = new StringBuilder();
        for (GitRepositoryOverview.Commit commit : mOverview.commits) {
            if (text.length() > 0) text.append("\n\n");
            text.append(commit.shortHash).append("  ").append(commit.relativeTime)
                .append('\n').append(commit.subject);
        }
        mContent.setText(text.toString());
    }

    private void showBranches() {
        AlertDialog dialog = createBranchesDialog();
        if (dialog != null) showStyledDialog(dialog);
    }

    private void showChangedFiles() {
        AlertDialog dialog = createChangedFilesDialog();
        if (dialog != null) showStyledDialog(dialog);
    }

    private void showStashes() {
        AlertDialog dialog = createStashesDialog();
        if (dialog != null) showStyledDialog(dialog);
    }

    @Nullable
    AlertDialog createStashesDialog() {
        if (mOverview == null) return null;
        if (mOverview.stashes.isEmpty()) {
            showStatus(getString(R.string.git_workbench_no_stashes), false);
            return null;
        }
        String[] labels = new String[mOverview.stashes.size()];
        for (int index = 0; index < mOverview.stashes.size(); index++) {
            labels[index] = stashLabel(mOverview.stashes.get(index));
        }
        return new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_stashes)
            .setAdapter(new ArrayAdapter<>(this, R.layout.item_termuxpro_list, labels),
                (selectionDialog, which) -> showStashActions(mOverview.stashes.get(which)))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    }

    @NonNull
    private String stashLabel(@NonNull GitRepositoryOverview.StashEntry stash) {
        return getString(R.string.git_workbench_stash_item, stash.ref, stash.relativeTime,
            stash.subject);
    }

    private void showStashActions(@NonNull GitRepositoryOverview.StashEntry stash) {
        String[] actions = {
            getString(R.string.git_workbench_stash_apply),
            getString(R.string.git_workbench_stash_drop)
        };
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(stash.ref)
            .setMessage(stash.subject)
            .setAdapter(new ArrayAdapter<>(this, R.layout.item_termuxpro_list, actions),
                (selectionDialog, which) -> {
                    if (which == 0) confirmApplyStash(stash);
                    else confirmDropStash(stash);
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    @Nullable
    AlertDialog createChangedFilesDialog() {
        if (mOverview == null) return null;
        if (mOverview.fileChanges.isEmpty()) {
            showStatus(getString(R.string.git_workbench_no_file_changes), false);
            return null;
        }
        String[] labels = new String[mOverview.fileChanges.size()];
        for (int index = 0; index < mOverview.fileChanges.size(); index++) {
            labels[index] = fileChangeLabel(mOverview.fileChanges.get(index));
        }
        return new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_files)
            .setAdapter(new ArrayAdapter<>(this, R.layout.item_termuxpro_list, labels),
                (selectionDialog, which) -> showFileIndexActions(mOverview.fileChanges.get(which)))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    }

    @NonNull
    private String fileChangeLabel(@NonNull GitRepositoryOverview.FileChange change) {
        return getString(R.string.git_workbench_file_change,
            fileChangeScope(change), change.path);
    }

    @NonNull
    private String fileChangeScope(@NonNull GitRepositoryOverview.FileChange change) {
        if (change.hasStagedChange() && change.hasUnstagedChange()) {
            return getString(R.string.git_workbench_file_scope_mixed);
        }
        if (change.hasStagedChange()) return getString(R.string.git_workbench_file_scope_staged);
        return getString(R.string.git_workbench_file_scope_unstaged);
    }

    private void showFileIndexActions(@NonNull GitRepositoryOverview.FileChange change) {
        List<String> actions = new ArrayList<>();
        List<Boolean> stageActions = new ArrayList<>();
        if (change.hasUnstagedChange()) {
            actions.add(getString(R.string.git_workbench_stage_file_action));
            stageActions.add(true);
        }
        if (change.hasStagedChange()) {
            actions.add(getString(R.string.git_workbench_unstage_file_action));
            stageActions.add(false);
        }
        if (actions.isEmpty()) return;
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(change.path)
            .setMessage(R.string.git_workbench_file_action_message)
            .setAdapter(new ArrayAdapter<>(this, R.layout.item_termuxpro_list,
                actions.toArray(new String[0])), (selectionDialog, which) ->
                runFileIndexOperation(stageActions.get(which), change.path))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    @Nullable
    AlertDialog createBranchesDialog() {
        if (mOverview == null || (mOverview.localBranches.isEmpty()
            && mOverview.remoteBranches.isEmpty())) {
            showStatus(getString(R.string.git_workbench_no_local_branches), false);
            return null;
        }
        int localCount = mOverview.localBranches.size();
        String[] labels = new String[localCount + mOverview.remoteBranches.size()];
        for (int index = 0; index < localCount; index++) {
            String branch = mOverview.localBranches.get(index);
            labels[index] = branch.equals(mOverview.head)
                ? getString(R.string.git_workbench_current_branch, branch)
                : getString(R.string.git_workbench_local_branch, branch);
        }
        for (int index = 0; index < mOverview.remoteBranches.size(); index++) {
            labels[localCount + index] = getString(R.string.git_workbench_remote_branch,
                mOverview.remoteBranches.get(index));
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_switch_branch)
            .setAdapter(new ArrayAdapter<>(this, R.layout.item_termuxpro_list, labels),
                (selectionDialog, which) -> {
                if (which < localCount) confirmSwitch(mOverview.localBranches.get(which));
                else confirmTrackRemoteBranch(mOverview.remoteBranches.get(which - localCount));
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        return dialog;
    }

    private void showDeleteBranchDialog() {
        AlertDialog dialog = createDeleteBranchDialog();
        if (dialog != null) showStyledDialog(dialog);
    }

    @Nullable
    AlertDialog createDeleteBranchDialog() {
        if (mOverview == null) return null;
        List<String> branches = deletableLocalBranches(mOverview);
        if (branches.isEmpty()) {
            showStatus(getString(R.string.git_workbench_no_deletable_local_branches), false);
            return null;
        }
        String[] labels = new String[branches.size()];
        for (int index = 0; index < branches.size(); index++) {
            labels[index] = getString(R.string.git_workbench_local_branch, branches.get(index));
        }
        return new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_delete_branch)
            .setAdapter(new ArrayAdapter<>(this, R.layout.item_termuxpro_list, labels),
                (selectionDialog, which) -> confirmDeleteLocalBranch(branches.get(which)))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    }

    @NonNull
    private static List<String> deletableLocalBranches(@NonNull GitRepositoryOverview overview) {
        List<String> branches = new ArrayList<>();
        for (String branch : overview.localBranches) {
            if (!overview.detached && branch.equals(overview.head)) continue;
            if (!WorkspaceCommandBuilder.isSafeGitBranchName(branch)) continue;
            branches.add(branch);
        }
        return branches;
    }

    void confirmDeleteLocalBranch(@NonNull String branch) {
        if (mOverview == null || !mOverview.localBranches.contains(branch)
            || (!mOverview.detached && branch.equals(mOverview.head))
            || !WorkspaceCommandBuilder.isSafeGitBranchName(branch)) {
            showStatus(getString(R.string.git_workbench_delete_branch_current_blocked), false);
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_delete_branch)
            .setMessage(getString(R.string.git_workbench_delete_branch_message, branch))
            .setPositiveButton(R.string.git_workbench_delete_branch_action,
                (selectionDialog, which) -> deleteLocalBranch(branch))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showDangerDialog(dialog);
    }

    private void showDangerDialog(@NonNull AlertDialog dialog) {
        TermuxProDialogStyle.show(this, dialog, shownDialog -> {
            if (shownDialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                shownDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(this, R.color.tp_danger));
            }
        });
    }

    private void confirmTrackRemoteBranch(@NonNull String branch) {
        if (mOverview != null && mOverview.changedFiles > 0) {
            showStatus(getString(R.string.git_workbench_switch_dirty_blocked,
                mOverview.changedFiles), false);
            return;
        }
        AlertDialog dialog = createTrackRemoteBranchDialog(branch);
        if (dialog != null) showStyledDialog(dialog);
    }

    @Nullable
    AlertDialog createTrackRemoteBranchDialog(@NonNull String branch) {
        if (mOverview == null || GitRepositoryOverview.isRemoteHead(branch)) return null;
        int message = mOverview.changedFiles > 0
            ? R.string.git_workbench_track_remote_dirty_message
            : R.string.git_workbench_track_remote_message;
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(branch)
            .setMessage(getString(message, mOverview.head, branch, mOverview.changedFiles))
            .setPositiveButton(R.string.git_workbench_track_remote_action,
                (selectionDialog, which) -> trackRemoteBranch(branch))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        return dialog;
    }

    private void confirmSwitch(@NonNull String branch) {
        if (mOverview == null || branch.equals(mOverview.head)) return;
        if (mOverview.changedFiles > 0) {
            showStatus(getString(R.string.git_workbench_switch_dirty_blocked,
                mOverview.changedFiles), false);
            return;
        }
        int message = mOverview.changedFiles > 0
            ? R.string.git_workbench_switch_dirty_message : R.string.git_workbench_switch_message;
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_switch_branch)
            .setMessage(getString(message, mOverview.head, branch, mOverview.changedFiles))
            .setPositiveButton(R.string.git_workbench_switch_action,
                (selectionDialog, which) -> switchBranch(branch))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    private void confirmStageAll() {
        if (mOverview == null || mOverview.unstagedFiles <= 0) {
            showStatus(getString(R.string.git_workbench_no_unstaged_changes), false);
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_stage_all)
            .setMessage(getString(R.string.git_workbench_stage_all_message,
                mOverview.unstagedFiles))
            .setPositiveButton(R.string.git_workbench_stage_all_action,
                (selectionDialog, which) -> runIndexOperation(true))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    private void confirmUnstageAll() {
        if (mOverview == null || mOverview.stagedFiles <= 0) {
            showStatus(getString(R.string.git_workbench_no_staged_changes), false);
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_unstage_all)
            .setMessage(getString(R.string.git_workbench_unstage_all_message,
                mOverview.stagedFiles))
            .setPositiveButton(R.string.git_workbench_unstage_all_action,
                (selectionDialog, which) -> runIndexOperation(false))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    private void showCommitDialog() {
        showPreparedDialog(createCommitDialog());
    }

    private void showCreateStashDialog() {
        showPreparedDialog(createStashDialog());
    }

    @Nullable
    AlertDialog createStashDialog() {
        if (mOverview == null) return null;
        if (mOverview.changedFiles <= 0) {
            showStatus(getString(R.string.git_workbench_no_stash_changes), false);
            return null;
        }
        EditText input = new EditText(this);
        input.setId(android.R.id.edit);
        input.setSingleLine(true);
        input.setHint(R.string.git_workbench_stash_hint);
        input.setTextColor(ContextCompat.getColor(this, R.color.tp_text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.tp_text_secondary));
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_stash_save)
            .setMessage(getString(R.string.git_workbench_stash_message,
                mOverview.changedFiles, mOverview.stagedFiles, mOverview.unstagedFiles))
            .setView(input)
            .setPositiveButton(R.string.git_workbench_stash_save_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        TermuxProDialogStyle.prepare(this, dialog, shownDialog -> {
            shownDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String message = input.getText().toString();
                if (!WorkspaceCommandBuilder.isSafeGitCommitMessage(message)) {
                    input.setError(getString(R.string.git_workbench_stash_invalid));
                    return;
                }
                shownDialog.dismiss();
                stashChanges(message.trim());
            });
        });
        return dialog;
    }

    @Nullable
    AlertDialog createCommitDialog() {
        if (mOverview == null) return null;
        if (mOverview.stagedFiles <= 0) {
            showStatus(getString(R.string.git_workbench_no_staged_changes), false);
            return null;
        }
        EditText input = new EditText(this);
        input.setId(android.R.id.edit);
        input.setSingleLine(true);
        input.setHint(R.string.git_workbench_commit_hint);
        input.setTextColor(ContextCompat.getColor(this, R.color.tp_text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.tp_text_secondary));
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_commit)
            .setMessage(getString(R.string.git_workbench_commit_message, mOverview.stagedFiles,
                mOverview.unstagedFiles))
            .setView(input)
            .setPositiveButton(R.string.git_workbench_commit_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        TermuxProDialogStyle.prepare(this, dialog, shownDialog -> {
            shownDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String message = input.getText().toString();
                if (!WorkspaceCommandBuilder.isSafeGitCommitMessage(message)) {
                    input.setError(getString(R.string.git_workbench_commit_invalid));
                    return;
                }
                shownDialog.dismiss();
                commitStaged(message.trim());
            });
        });
        return dialog;
    }

    void showStyledDialog(@NonNull AlertDialog dialog) {
        TermuxProDialogStyle.show(this, dialog);
    }

    private void showCreateBranchDialog() {
        showPreparedDialog(createNewBranchDialog());
    }

    private void showPreparedDialog(@Nullable AlertDialog dialog) {
        // createXxxDialog() 已通过 TermuxProDialogStyle.prepare 绑定产品样式与输入校验。
        if (dialog != null) dialog.show();
    }

    @Nullable
    AlertDialog createNewBranchDialog() {
        if (mOverview == null) return null;
        EditText input = new EditText(this);
        input.setId(android.R.id.edit);
        input.setSingleLine(true);
        input.setHint(R.string.git_workbench_create_branch_hint);
        input.setTextColor(ContextCompat.getColor(this, R.color.tp_text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.tp_text_secondary));
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_create_branch)
            .setMessage(getString(mOverview.changedFiles > 0
                ? R.string.git_workbench_create_branch_dirty_message
                : R.string.git_workbench_create_branch_message, mOverview.head,
                mOverview.changedFiles))
            .setView(input)
            .setPositiveButton(R.string.git_workbench_create_branch_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        TermuxProDialogStyle.prepare(this, dialog, shownDialog -> {
            shownDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String branch = input.getText().toString().trim();
                if (!WorkspaceCommandBuilder.isSafeGitBranchName(branch)) {
                    input.setError(getString(R.string.git_workbench_create_branch_invalid));
                    return;
                }
                shownDialog.dismiss();
                createBranch(branch);
            });
        });
        return dialog;
    }

    private void switchBranch(@NonNull String branch) {
        if (mOverview == null || !mOverview.localBranches.contains(branch)) return;
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_switching, branch));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitSwitchBranchRemoteCommand(target.path, branch),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 77) showStatus(getString(
                    R.string.git_workbench_switch_dirty_blocked_remote), false);
                else showStatus(getString(R.string.git_workbench_switch_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void createBranch(@NonNull String branch) {
        if (mOverview == null || !WorkspaceCommandBuilder.isSafeGitBranchName(branch)) return;
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_creating_branch, branch));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitCreateBranchRemoteCommand(target.path, branch),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 74) showStatus(getString(
                    R.string.git_workbench_create_branch_conflict, branch), false);
                else showStatus(getString(R.string.git_workbench_create_branch_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void deleteLocalBranch(@NonNull String branch) {
        if (mOverview == null || !mOverview.localBranches.contains(branch)
            || (!mOverview.detached && branch.equals(mOverview.head))
            || !WorkspaceCommandBuilder.isSafeGitBranchName(branch)) {
            return;
        }
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_deleting_branch, branch));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand(target.path, branch),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 79) showStatus(
                    getString(R.string.git_workbench_delete_branch_current_blocked), false);
                else if (result.exitCode == 80) showStatus(getString(
                    R.string.git_workbench_delete_branch_missing, branch), false);
                else showStatus(getString(R.string.git_workbench_delete_branch_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void trackRemoteBranch(@NonNull String branch) {
        if (mOverview == null || !mOverview.remoteBranches.contains(branch)
            || GitRepositoryOverview.isRemoteHead(branch)) {
            return;
        }
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_switching, branch));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitTrackRemoteBranchCommand(target.path, branch),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 74) showStatus(getString(
                    R.string.git_workbench_track_remote_conflict, branch), false);
                else if (result.exitCode == 77) showStatus(getString(
                    R.string.git_workbench_switch_dirty_blocked_remote), false);
                else showStatus(getString(R.string.git_workbench_switch_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void runIndexOperation(boolean stage) {
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(stage
            ? R.string.git_workbench_staging_all
            : R.string.git_workbench_unstaging_all));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                stage
                    ? WorkspaceCommandBuilder.buildGitStageAllRemoteCommand(target.path)
                    : WorkspaceCommandBuilder.buildGitUnstageAllRemoteCommand(target.path),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 75) showStatus(getString(stage
                    ? R.string.git_workbench_no_unstaged_changes
                    : R.string.git_workbench_no_staged_changes), false);
                else showStatus(getString(stage
                    ? R.string.git_workbench_stage_all_failed
                    : R.string.git_workbench_unstage_all_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void runFileIndexOperation(boolean stage, @NonNull String filePath) {
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(stage
            ? R.string.git_workbench_staging_file
            : R.string.git_workbench_unstaging_file, filePath));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                stage
                    ? WorkspaceCommandBuilder.buildGitStageFileRemoteCommand(target.path, filePath)
                    : WorkspaceCommandBuilder.buildGitUnstageFileRemoteCommand(target.path, filePath),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 75) showStatus(getString(stage
                    ? R.string.git_workbench_no_unstaged_file_change
                    : R.string.git_workbench_no_staged_file_change), false);
                else showStatus(getString(stage
                    ? R.string.git_workbench_stage_file_failed
                    : R.string.git_workbench_unstage_file_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void commitStaged(@NonNull String message) {
        if (mOverview == null || mOverview.stagedFiles <= 0
            || !WorkspaceCommandBuilder.isSafeGitCommitMessage(message)) {
            return;
        }
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_committing));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitCommitStagedRemoteCommand(target.path, message),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 75) showStatus(
                    getString(R.string.git_workbench_no_staged_changes), false);
                else showStatus(getString(R.string.git_workbench_commit_failed,
                    result.output.trim()), false);
            });
        });
    }

    void confirmApplyStash(@NonNull GitRepositoryOverview.StashEntry stash) {
        if (mOverview == null || !mOverview.stashes.contains(stash)
            || !WorkspaceCommandBuilder.isSafeGitStashRef(stash.ref)) {
            return;
        }
        if (mOverview.changedFiles > 0) {
            showStatus(getString(R.string.git_workbench_stash_apply_dirty_blocked,
                mOverview.changedFiles), false);
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_stash_apply)
            .setMessage(getString(R.string.git_workbench_stash_apply_message,
                stash.ref, stash.subject))
            .setPositiveButton(R.string.git_workbench_stash_apply_action,
                (selectionDialog, which) -> applyStash(stash.ref))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    void confirmDropStash(@NonNull GitRepositoryOverview.StashEntry stash) {
        if (mOverview == null || !mOverview.stashes.contains(stash)
            || !WorkspaceCommandBuilder.isSafeGitStashRef(stash.ref)) {
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_stash_drop)
            .setMessage(getString(R.string.git_workbench_stash_drop_message,
                stash.ref, stash.subject))
            .setPositiveButton(R.string.git_workbench_stash_drop_action,
                (selectionDialog, which) -> dropStash(stash.ref))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showDangerDialog(dialog);
    }

    private void stashChanges(@NonNull String message) {
        if (mOverview == null || mOverview.changedFiles <= 0
            || !WorkspaceCommandBuilder.isSafeGitCommitMessage(message)) {
            return;
        }
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_stashing));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitStashPushRemoteCommand(target.path, message),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 75) showStatus(
                    getString(R.string.git_workbench_no_stash_changes), false);
                else showStatus(getString(R.string.git_workbench_stash_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void applyStash(@NonNull String stashRef) {
        if (mOverview == null || mOverview.changedFiles > 0
            || !WorkspaceCommandBuilder.isSafeGitStashRef(stashRef)) {
            return;
        }
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_stash_applying, stashRef));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitStashApplyRemoteCommand(target.path, stashRef),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 77) showStatus(getString(
                    R.string.git_workbench_stash_apply_dirty_remote_blocked), false);
                else if (result.exitCode == 81) showStatus(getString(
                    R.string.git_workbench_stash_missing, stashRef), false);
                else showStatus(getString(R.string.git_workbench_stash_apply_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void dropStash(@NonNull String stashRef) {
        if (!WorkspaceCommandBuilder.isSafeGitStashRef(stashRef)) return;
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_stash_dropping, stashRef));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitStashDropRemoteCommand(target.path, stashRef),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 81) showStatus(getString(
                    R.string.git_workbench_stash_missing, stashRef), false);
                else showStatus(getString(R.string.git_workbench_stash_drop_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void fetchUpstream() {
        if (mOverview == null || mOverview.upstream == null) {
            showStatus(getString(R.string.git_workbench_no_upstream), false);
            return;
        }
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_fetching, mOverview.upstream));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitFetchUpstreamRemoteCommand(target.path),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 76) showStatus(
                    getString(R.string.git_workbench_no_upstream), false);
                else showStatus(getString(R.string.git_workbench_fetch_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void confirmPullFastForward() {
        if (mOverview == null || mOverview.upstream == null) {
            showStatus(getString(R.string.git_workbench_no_upstream), false);
            return;
        }
        if (mOverview.behind == null || mOverview.behind <= 0) {
            showStatus(getString(R.string.git_workbench_pull_not_needed), false);
            return;
        }
        if (mOverview.changedFiles > 0) {
            showStatus(getString(R.string.git_workbench_pull_dirty_blocked,
                mOverview.changedFiles), false);
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_pull)
            .setMessage(getString(R.string.git_workbench_pull_message,
                mOverview.behind, mOverview.upstream))
            .setPositiveButton(R.string.git_workbench_pull_action,
                (selectionDialog, which) -> pullFastForward())
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    private void pullFastForward() {
        if (mOverview == null || mOverview.upstream == null) return;
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_pulling, mOverview.upstream));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitPullFastForwardRemoteCommand(target.path),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 76) showStatus(
                    getString(R.string.git_workbench_no_upstream), false);
                else if (result.exitCode == 77) showStatus(
                    getString(R.string.git_workbench_pull_dirty_remote_blocked), false);
                else showStatus(getString(R.string.git_workbench_pull_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void confirmPushUpstream() {
        if (mOverview == null || mOverview.upstream == null) {
            showStatus(getString(R.string.git_workbench_no_upstream), false);
            return;
        }
        if (mOverview.detached) {
            showStatus(getString(R.string.git_workbench_push_detached_blocked), false);
            return;
        }
        if (mOverview.ahead == null || mOverview.ahead <= 0) {
            showStatus(getString(R.string.git_workbench_push_not_needed), false);
            return;
        }
        if (mOverview.behind != null && mOverview.behind > 0) {
            showStatus(getString(R.string.git_workbench_push_behind_blocked,
                mOverview.behind), false);
            return;
        }
        int message = mOverview.changedFiles > 0
            ? R.string.git_workbench_push_dirty_message
            : R.string.git_workbench_push_message;
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.git_workbench_push)
            .setMessage(getString(message, mOverview.ahead, mOverview.upstream,
                mOverview.changedFiles))
            .setPositiveButton(R.string.git_workbench_push_action,
                (selectionDialog, which) -> pushUpstream())
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        showStyledDialog(dialog);
    }

    private void pushUpstream() {
        if (mOverview == null || mOverview.upstream == null) return;
        ConnectionTarget target = readTarget();
        if (target == null) return;
        beginLoading(getString(R.string.git_workbench_pushing, mOverview.upstream));
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(target.host, target.port,
                WorkspaceCommandBuilder.buildGitPushUpstreamRemoteCommand(target.path),
                MAX_OUTPUT_BYTES);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.exitCode == 0) loadOverview();
                else if (result.exitCode == 76) showStatus(
                    getString(R.string.git_workbench_no_upstream), false);
                else if (result.exitCode == 78) showStatus(
                    getString(R.string.git_workbench_push_detached_blocked), false);
                else showStatus(getString(R.string.git_workbench_push_failed,
                    result.output.trim()), false);
            });
        });
    }

    private void showRemoteFailure(@NonNull RemoteCommandRunner.Result result, int commandFailure) {
        mProgress.setVisibility(View.GONE);
        if (result.exitCode == RemoteCommandRunner.ERROR_SSH_MISSING) {
            showStatus(getString(R.string.git_diff_ssh_missing), true);
        } else if (result.exitCode == RemoteCommandRunner.ERROR_PROCESS) {
            showStatus(getString(R.string.git_diff_connection_error,
                result.errorType == null ? "Process" : result.errorType), true);
        } else if (result.exitCode == RemoteCommandRunner.ERROR_INTERRUPTED) {
            showStatus(getString(R.string.git_diff_cancelled), false);
        } else {
            showStatus(getString(commandFailure), false);
        }
    }

    private void navigateBack() {
        if (mMode != Mode.OVERVIEW && mOverview != null) {
            mMode = Mode.OVERVIEW;
            String path = getIntent().getStringExtra(EXTRA_PATH);
            showOverview(overviewTarget(path == null ? "" : path), mOverview);
        } else {
            finish();
        }
    }

    @NonNull
    private ConnectionTarget overviewTarget(@NonNull String fallbackPath) {
        String host = getIntent().getStringExtra(EXTRA_HOST);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        int port = getIntent().getIntExtra(EXTRA_PORT, 22);
        return new ConnectionTarget(host == null ? "" : host, port,
            path == null ? fallbackPath : path);
    }

    private void showStatus(@NonNull String message, boolean recoverable) {
        mContentScroll.setVisibility(View.GONE);
        mOverviewScroll.setVisibility(View.GONE);
        mStatusState.setVisibility(View.VISIBLE);
        mStatusMessage.setText(message);
        mReturnWorkspace.setVisibility(recoverable ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private SpannableStringBuilder colorize(@NonNull String output) {
        SpannableStringBuilder styled = new SpannableStringBuilder();
        String[] lines = output.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int start = styled.length();
            styled.append(line);
            if (index < lines.length - 1) styled.append('\n');
            int color;
            switch (DiffLineClassifier.classify(line)) {
                case HEADER: color = Color.rgb(125, 183, 255); break;
                case HUNK: color = Color.rgb(208, 167, 255); break;
                case ADDITION: color = Color.rgb(112, 225, 161); break;
                case DELETION: color = Color.rgb(255, 138, 128); break;
                default: color = Color.rgb(216, 228, 236); break;
            }
            styled.setSpan(new ForegroundColorSpan(color), start, styled.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    private static final class CommandResult {
        final int exitCode;
        final String output;
        final boolean truncated;
        final boolean recoverable;

        CommandResult(int exitCode, @NonNull String output, boolean truncated, boolean recoverable) {
            this.exitCode = exitCode;
            this.output = output;
            this.truncated = truncated;
            this.recoverable = recoverable;
        }
    }

    private enum Mode { OVERVIEW, DIFF, COMMITS }

    private static final class ConnectionTarget {
        @NonNull final String host;
        final int port;
        @NonNull final String path;

        ConnectionTarget(@NonNull String host, int port, @NonNull String path) {
            this.host = host;
            this.port = port;
            this.path = path;
        }
    }
}
