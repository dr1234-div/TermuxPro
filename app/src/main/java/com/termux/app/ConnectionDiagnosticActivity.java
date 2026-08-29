package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
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
        ListView list = findViewById(R.id.connection_diagnostic_list);
        mAdapter = new ArrayAdapter<>(this, R.layout.item_termuxpro_list, mItems);
        list.setAdapter(mAdapter);
        findViewById(R.id.connection_diagnostic_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.connection_diagnostic_refresh_button).setOnClickListener(view -> diagnose());
        diagnose();
    }

    private void diagnose() {
        mRunner.cancel();
        mItems.clear();
        mAdapter.notifyDataSetChanged();
        mProgress.setVisibility(View.VISIBLE);
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
            mStatus.setText(getString(R.string.connection_diagnostic_failed, result.exitCode));
            return;
        }
        mItems.addAll(ConnectionDiagnosticReport.parse(result.output));
        mAdapter.notifyDataSetChanged();
        mStatus.setText(mItems.isEmpty() ? R.string.connection_diagnostic_invalid :
            R.string.connection_diagnostic_success);
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }
}
