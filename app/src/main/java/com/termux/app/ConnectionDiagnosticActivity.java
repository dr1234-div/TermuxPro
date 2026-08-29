package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 通过已认证 SSH 连接执行只读环境诊断。 */
public final class ConnectionDiagnosticActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PROJECT_PATH = "project_path";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private final List<ConnectionDiagnosticReport.Item> mItems = new ArrayList<>();
    private String mHost;
    private int mPort;
    private String mProjectPath;
    private ProgressBar mProgress;
    private TextView mStatus;
    private Button mInteractiveConnection;
    private ArrayAdapter<ConnectionDiagnosticReport.Item> mAdapter;

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String host, int port,
                            @NonNull String projectPath) {
        return new Intent(context, ConnectionDiagnosticActivity.class)
            .putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_PROJECT_PATH, projectPath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_diagnostic);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mProjectPath = getIntent().getStringExtra(EXTRA_PROJECT_PATH);
        mProgress = findViewById(R.id.connection_diagnostic_progress);
        mStatus = findViewById(R.id.connection_diagnostic_status);
        mInteractiveConnection = findViewById(R.id.connection_diagnostic_interactive_button);
        ListView list = findViewById(R.id.connection_diagnostic_list);
        mAdapter = new ArrayAdapter<>(this, R.layout.item_termuxpro_list, mItems);
        list.setAdapter(mAdapter);
        findViewById(R.id.connection_diagnostic_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.connection_diagnostic_refresh_button).setOnClickListener(view -> diagnose());
        mInteractiveConnection.setOnClickListener(view -> openInteractiveConnection());
        diagnose();
    }

    private void diagnose() {
        mRunner.cancel();
        mItems.clear();
        mAdapter.notifyDataSetChanged();
        mProgress.setVisibility(View.VISIBLE);
        mInteractiveConnection.setVisibility(View.GONE);
        mStatus.setText(R.string.connection_diagnostic_running);
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort,
                WorkspaceCommandBuilder.buildConnectionDiagnosticCommand(mProjectPath), 128_000);
            mMainHandler.post(() -> showResult(result));
        });
    }

    private void showResult(RemoteCommandRunner.Result result) {
        if (isFinishing() || isDestroyed()) return;
        mProgress.setVisibility(View.GONE);
        if (result.exitCode != 0) {
            SshFailureClassifier.Reason reason = SshFailureClassifier.classify(result);
            addStages(SshDiagnosticStages.failure(reason));
            mAdapter.notifyDataSetChanged();
            mInteractiveConnection.setVisibility(
                SshDiagnosticStages.canOpenInteractiveConnection(reason) ? View.VISIBLE : View.GONE);
            mStatus.setText(messageForFailure(reason, result.exitCode));
            return;
        }
        List<ConnectionDiagnosticReport.Item> remoteItems =
            ConnectionDiagnosticReport.parse(result.output);
        if (remoteItems.isEmpty()) {
            addStages(SshDiagnosticStages.invalidRemoteEnvironment());
            mAdapter.notifyDataSetChanged();
            mStatus.setText(R.string.connection_diagnostic_invalid);
            return;
        }
        addStages(SshDiagnosticStages.success());
        mItems.addAll(remoteItems);
        mAdapter.notifyDataSetChanged();
        mStatus.setText(R.string.connection_diagnostic_success);
    }

    private void addStages(List<SshDiagnosticStages.Item> stages) {
        for (SshDiagnosticStages.Item stage : stages) {
            mItems.add(new ConnectionDiagnosticReport.Item(
                getString(stageLabel(stage.stage)), getString(stageState(stage.state)),
                stage.state == SshDiagnosticStages.State.PASSED));
        }
    }

    private int stageLabel(SshDiagnosticStages.Stage stage) {
        switch (stage) {
            case NETWORK: return R.string.connection_stage_network;
            case HOST_IDENTITY: return R.string.connection_stage_host_identity;
            case AUTHENTICATION: return R.string.connection_stage_authentication;
            default: return R.string.connection_stage_remote_environment;
        }
    }

    private int stageState(SshDiagnosticStages.State state) {
        switch (state) {
            case PASSED: return R.string.connection_stage_passed;
            case ACTION_REQUIRED: return R.string.connection_stage_action_required;
            case FAILED: return R.string.connection_stage_failed;
            default: return R.string.connection_stage_pending;
        }
    }

    private void openInteractiveConnection() {
        String command = WorkspaceCommandBuilder.buildSshCommand(mHost, mPort, mProjectPath, null,
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, command)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }

    private String messageForFailure(SshFailureClassifier.Reason reason, int exitCode) {
        switch (reason) {
            case SSH_MISSING: return getString(R.string.connection_error_ssh_missing);
            case INTERRUPTED: return getString(R.string.connection_error_interrupted);
            case PROCESS_ERROR: return getString(R.string.connection_error_process);
            case DNS_FAILED: return getString(R.string.connection_error_dns);
            case TIMEOUT: return getString(R.string.connection_error_timeout);
            case REFUSED: return getString(R.string.connection_error_refused);
            case NO_ROUTE: return getString(R.string.connection_error_no_route);
            case HOST_KEY_CHANGED: return getString(R.string.connection_error_host_key_changed);
            case HOST_KEY_UNVERIFIED: return getString(R.string.connection_error_host_key_unverified);
            case AUTH_FAILED: return getString(R.string.connection_error_auth);
            case CONNECTION_CLOSED: return getString(R.string.connection_error_closed);
            default: return getString(R.string.connection_diagnostic_failed, exitCode);
        }
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }
}
