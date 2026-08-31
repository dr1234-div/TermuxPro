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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.termux.R;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 通过已认证的 OpenSSH 复用连接提供 Git 概览、分支切换、修改审查和提交记录。 */
public final class GitDiffActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PATH = "path";
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
        findViewById(R.id.git_overview_changes_button).setOnClickListener(view -> loadDiff());
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
            loadOverview();
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
                    showOverview(target.path, mOverview);
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

    private void showOverview(@NonNull String path, @NonNull GitRepositoryOverview overview) {
        mOverview = overview;
        mProgress.setVisibility(View.GONE);
        mStatusState.setVisibility(View.GONE);
        mContentScroll.setVisibility(View.GONE);
        mOverviewScroll.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.git_overview_head)).setText(getString(
            overview.detached ? R.string.git_workbench_detached : R.string.git_workbench_branch,
            overview.head));
        ((TextView) findViewById(R.id.git_overview_path)).setText(path);
        ((TextView) findViewById(R.id.git_overview_changes)).setText(getResources().getQuantityString(
            R.plurals.git_workbench_changed_files, overview.changedFiles, overview.changedFiles));
        TextView sync = findViewById(R.id.git_overview_sync);
        if (overview.ahead == null || overview.behind == null) {
            sync.setText(R.string.git_workbench_no_upstream);
        } else {
            sync.setText(getString(R.string.git_workbench_sync, overview.ahead, overview.behind));
        }
    }

    /** 模拟器截图只注入脱敏协议数据，仍走与真实 SSH 结果相同的解析和渲染路径。 */
    void showOverviewForTesting(@NonNull String path, @NonNull String protocolOutput) {
        showOverview(path, GitRepositoryOverview.parse(protocolOutput));
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

    private void confirmTrackRemoteBranch(@NonNull String branch) {
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

    void showStyledDialog(@NonNull AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> TermuxProDialogStyle.apply(this, dialog));
        dialog.show();
    }

    private void showCreateBranchDialog() {
        AlertDialog dialog = createNewBranchDialog();
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
        dialog.setOnShowListener(ignored -> {
            TermuxProDialogStyle.apply(this, dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String branch = input.getText().toString().trim();
                if (!WorkspaceCommandBuilder.isSafeGitBranchName(branch)) {
                    input.setError(getString(R.string.git_workbench_create_branch_invalid));
                    return;
                }
                dialog.dismiss();
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
                else showStatus(getString(R.string.git_workbench_switch_failed,
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
            showOverview(path == null ? "" : path, mOverview);
        } else {
            finish();
        }
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
