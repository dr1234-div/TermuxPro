package com.termux.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 查看当前远端用户的全部 tmux 会话，并安全进入或管理 TermuxPro 自有会话。 */
public final class TaskSessionsActivity extends AppCompatActivity {

    static final String EXTRA_UI_TEST_SESSIONS = "com.termux.app.extra.UI_TEST_SESSIONS";

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PROJECT_PATH = "project_path";
    private static final String EXTRA_OWNER_TOKEN = "owner_token";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private final List<TmuxSessionInfo> mSessions = new ArrayList<>();
    private String mHost;
    private int mPort;
    private String mProjectPath;
    private String mOwnerToken;
    private ProgressBar mProgress;
    private TextView mStatus;
    private Button mRecovery;
    private ArrayAdapter<TmuxSessionInfo> mAdapter;
    private View mRefresh;
    private Button mCreate;
    private ListView mList;

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String host, int port,
                            @NonNull String projectPath, @NonNull String ownerToken) {
        return new Intent(context, TaskSessionsActivity.class)
            .putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_PROJECT_PATH, projectPath).putExtra(EXTRA_OWNER_TOKEN, ownerToken);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_sessions);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mProjectPath = getIntent().getStringExtra(EXTRA_PROJECT_PATH);
        mOwnerToken = getIntent().getStringExtra(EXTRA_OWNER_TOKEN);
        mProgress = findViewById(R.id.task_sessions_progress);
        mStatus = findViewById(R.id.task_sessions_status);
        mRecovery = findViewById(R.id.task_sessions_recovery_button);
        mCreate = findViewById(R.id.task_sessions_create_button);
        mList = findViewById(R.id.task_sessions_list);
        ((TextView) findViewById(R.id.task_sessions_target)).setText(
            getString(R.string.task_sessions_target, String.valueOf(mHost), mPort,
                String.valueOf(mProjectPath)));
        mAdapter = new ArrayAdapter<TmuxSessionInfo>(this, R.layout.item_termuxpro_list, mSessions) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                TmuxSessionInfo session = getItem(position);
                if (session != null) view.setText(sessionRow(session));
                return view;
            }
        };
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener((parent, view, position, id) -> showActions(mSessions.get(position)));
        findViewById(R.id.task_sessions_back_button).setOnClickListener(view -> finish());
        mRefresh = findViewById(R.id.task_sessions_refresh_button);
        mRefresh.setOnClickListener(view -> loadSessions());
        mCreate.setOnClickListener(view -> showNameDialog(null));
        configureReturnToWorkspace();
        if (mHost == null || mHost.trim().isEmpty() || mProjectPath == null || mProjectPath.trim().isEmpty() ||
            !WorkspaceOwnershipStore.isValid(mOwnerToken) ||
            mPort < 1 || mPort > 65535) {
            showFailure(R.string.task_sessions_invalid_workspace);
            mRefresh.setEnabled(false);
            mCreate.setEnabled(false);
        } else if (getIntent().getBooleanExtra(EXTRA_UI_TEST_SESSIONS, false)) {
            showPreviewForUiTest();
        } else {
            loadSessions();
        }
    }

    private void showPreviewForUiTest() {
        String fingerprint = WorkspaceCommandBuilder.workspaceFingerprint(mHost, mPort, mProjectPath);
        String output = "feature-login\0002\0000\0001788153600\0001788157200\000" + mOwnerToken
            + "\000" + fingerprint + "\000shared-support\0001\0001\0001788067200\0001788150000\000\000\000";
        mProgress.setVisibility(View.GONE);
        mSessions.clear();
        mSessions.addAll(TmuxSessionParser.parse(output, mOwnerToken, fingerprint));
        mAdapter.notifyDataSetChanged();
        mCreate.setVisibility(View.VISIBLE);
        styleCreateButton(false);
        mStatus.setText(R.string.task_sessions_ready);
    }

    private void loadSessions() {
        mRunner.cancel();
        mSessions.clear();
        mAdapter.notifyDataSetChanged();
        mProgress.setVisibility(View.VISIBLE);
        mCreate.setVisibility(View.GONE);
        mList.setEnabled(false);
        mRefresh.setEnabled(false);
        configureReturnToWorkspace();
        mRecovery.setVisibility(View.GONE);
        mStatus.setText(R.string.task_sessions_loading);
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort,
                WorkspaceCommandBuilder.buildListTmuxSessionsRemoteCommand(), 128_000);
            mMainHandler.post(() -> showResult(result));
        });
    }

    private void showResult(RemoteCommandRunner.Result result) {
        if (isFinishing() || isDestroyed()) return;
        mProgress.setVisibility(View.GONE);
        mRefresh.setEnabled(true);
        if (result.exitCode != 0) {
            showFailure(R.string.task_sessions_failed);
            return;
        }
        if (TmuxSessionParser.reportsMissingTmux(result.output)) {
            showMissingTmux();
            return;
        }
        mSessions.addAll(TmuxSessionParser.parse(result.output, mOwnerToken,
            WorkspaceCommandBuilder.workspaceFingerprint(mHost, mPort, mProjectPath)));
        mAdapter.notifyDataSetChanged();
        mCreate.setEnabled(true);
        mCreate.setVisibility(View.VISIBLE);
        mList.setEnabled(true);
        styleCreateButton(mSessions.isEmpty());
        mStatus.setText(mSessions.isEmpty() ? R.string.task_sessions_empty : R.string.task_sessions_ready);
    }

    private void showFailure(int message) {
        mProgress.setVisibility(View.GONE);
        mCreate.setVisibility(View.GONE);
        mRefresh.setEnabled(true);
        mStatus.setText(message);
        mRecovery.setVisibility(View.VISIBLE);
    }

    private void showMissingTmux() {
        mProgress.setVisibility(View.GONE);
        mCreate.setVisibility(View.GONE);
        mRefresh.setEnabled(true);
        mStatus.setText(R.string.task_sessions_tmux_missing);
        mRecovery.setText(R.string.task_sessions_open_plain_ssh);
        mRecovery.setOnClickListener(view -> openPlainSsh());
        mRecovery.setVisibility(View.VISIBLE);
    }

    private void configureReturnToWorkspace() {
        mRecovery.setText(R.string.return_to_workspace);
        mRecovery.setOnClickListener(view -> WorkspaceNavigation.returnToWorkspace(this));
    }

    private void openPlainSsh() {
        String command = WorkspaceCommandBuilder.buildSshCommand(mHost, mPort, mProjectPath, null,
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, command)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }

    private String sessionRow(TmuxSessionInfo session) {
        String state = session.attached ? getString(R.string.task_sessions_attached) :
            getString(R.string.task_sessions_background);
        String ownership = session.managedByTermuxPro ?
            getString(R.string.task_sessions_owned) : getString(R.string.task_sessions_unknown_owner);
        String created = formatSessionTime(session.createdEpochSeconds);
        String activity = formatSessionTime(session.activityEpochSeconds);
        return getString(R.string.task_sessions_row, session.name, session.windows, state, ownership,
            created, activity);
    }

    private String formatSessionTime(long epochSeconds) {
        if (epochSeconds <= 0L) return getString(R.string.task_sessions_time_unknown);
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            .format(new Date(epochSeconds * 1000L));
    }

    private void showActions(TmuxSessionInfo session) {
        String[] actions = session.managedByTermuxPro
            ? new String[]{getString(R.string.task_sessions_attach), getString(R.string.task_sessions_rename)}
            : new String[]{getString(R.string.task_sessions_attach)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(session.name)
            .setMessage(session.managedByTermuxPro
                ? getString(R.string.task_sessions_owned_context, mHost, mPort, mProjectPath)
                : getString(R.string.task_sessions_unknown_owner_warning));
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) attach(session);
            else if (which == 1) showNameDialog(session);
        });
        if (session.managedByTermuxPro) {
            builder.setNeutralButton(R.string.task_sessions_stop, (dialog, which) -> confirmStop(session));
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            TermuxProDialogStyle.apply(this, dialog);
            if (session.managedByTermuxPro) dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setTextColor(ContextCompat.getColor(this, R.color.tp_danger));
        });
        dialog.show();
    }

    private void showNameDialog(TmuxSessionInfo session) {
        View content = getLayoutInflater().inflate(R.layout.dialog_tmux_session_name, null);
        EditText input = content.findViewById(R.id.task_session_name_input);
        if (session != null) input.setText(session.name);
        int title = session == null ? R.string.task_sessions_create_title : R.string.task_sessions_rename_title;
        int action = session == null ? R.string.task_sessions_create : R.string.task_sessions_rename;
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(title).setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(action, null).create();
        dialog.setOnShowListener(ignored -> {
            TermuxProDialogStyle.apply(this, dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String name = input.getText().toString();
                if (!TmuxSessionNameValidator.isValid(name)) {
                    input.setError(getString(R.string.workspace_error_session_name));
                    return;
                }
                if (session != null && session.name.equals(name)) {
                    dialog.dismiss();
                    return;
                }
                dialog.dismiss();
                mutateSession(session == null
                    ? WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
                    name, mOwnerToken, mHost, mPort, mProjectPath)
                    : WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
                    session.name, name, mOwnerToken, mHost, mPort, mProjectPath),
                    session == null ? R.string.task_sessions_create_failed : R.string.task_sessions_rename_failed,
                    session == null
                        ? getString(R.string.task_sessions_creating, name)
                        : getString(R.string.task_sessions_renaming, session.name, name));
            });
            input.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            });
        });
        dialog.show();
    }

    private void mutateSession(String command, int failureMessage, String progressMessage) {
        mProgress.setVisibility(View.VISIBLE);
        mStatus.setText(progressMessage);
        mCreate.setEnabled(false);
        mRefresh.setEnabled(false);
        mList.setEnabled(false);
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort, command, 32_000);
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                mCreate.setEnabled(true);
                mRefresh.setEnabled(true);
                if (result.exitCode == 0) loadSessions();
                else showMutationFailure(failureMessage);
            });
        });
    }

    private void attach(TmuxSessionInfo session) {
        String command = WorkspaceCommandBuilder.buildAttachTaskSessionCommand(mHost, mPort, session.name,
            session.managedByTermuxPro ? mOwnerToken : null, mProjectPath);
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, command)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }

    private void confirmStop(TmuxSessionInfo session) {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.task_sessions_stop_title)
            .setMessage(getString(R.string.task_sessions_stop_message, session.name,
                mHost, mPort, mProjectPath))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.task_sessions_stop, (ignoredDialog, which) -> stop(session))
            .create();
        dialog.setOnShowListener(ignored -> {
            TermuxProDialogStyle.apply(this, dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.tp_danger));
        });
        dialog.show();
    }

    private void stop(TmuxSessionInfo session) {
        mutateSession(WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
            session.name, mOwnerToken, mHost, mPort, mProjectPath), R.string.task_sessions_stop_failed,
            getString(R.string.task_sessions_stopping, session.name));
    }

    /** 仅供真实 Android 截图验收打开完整状态，不执行远端写操作。 */
    void showRenameDialogForTesting() {
        if (!mSessions.isEmpty()) showNameDialog(mSessions.get(0));
    }

    /** 仅供真实 Android 截图验收危险操作的层级与上下文。 */
    void showStopDialogForTesting() {
        if (!mSessions.isEmpty()) confirmStop(mSessions.get(0));
    }

    private void showMutationFailure(int message) {
        mProgress.setVisibility(View.GONE);
        mStatus.setText(getString(R.string.task_sessions_mutation_uncertain, getString(message)));
        mRecovery.setText(R.string.task_sessions_refresh_result);
        mRecovery.setOnClickListener(view -> loadSessions());
        mRecovery.setVisibility(View.VISIBLE);
    }

    private void styleCreateButton(boolean primary) {
        mCreate.setBackgroundTintList(ContextCompat.getColorStateList(this,
            primary ? R.color.tp_primary : R.color.tp_surface_elevated));
        mCreate.setTextColor(ContextCompat.getColor(this,
            primary ? R.color.tp_on_primary : R.color.tp_primary));
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }

}
