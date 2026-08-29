package com.termux.app;

import android.app.AlertDialog;
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

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 识别远端项目并以确认式交互启动常用开发任务。 */
public final class ProjectTasksActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PROJECT_PATH = "project_path";
    private static final int MAX_METADATA_BYTES = 600_000;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private final List<ProjectTaskDetector.Task> mTasks = new ArrayList<>();
    private String mHost;
    private int mPort;
    private String mProjectPath;
    private ProgressBar mProgress;
    private TextView mType;
    private ArrayAdapter<ProjectTaskDetector.Task> mAdapter;

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String host, int port,
                            @NonNull String projectPath) {
        return new Intent(context, ProjectTasksActivity.class)
            .putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_PROJECT_PATH, projectPath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_tasks);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mProjectPath = getIntent().getStringExtra(EXTRA_PROJECT_PATH);
        mProgress = findViewById(R.id.project_tasks_progress);
        mType = findViewById(R.id.project_tasks_type);
        ListView list = findViewById(R.id.project_tasks_list);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTasks);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) -> confirmTask(mTasks.get(position)));
        findViewById(R.id.project_tasks_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.project_tasks_refresh_button).setOnClickListener(view -> detect());
        if (mHost == null || mProjectPath == null || mPort < 1 || mPort > 65535) {
            showError(R.string.project_tasks_invalid_workspace);
        } else {
            detect();
        }
    }

    private void detect() {
        mRunner.cancel();
        mProgress.setVisibility(View.VISIBLE);
        mType.setText(R.string.project_tasks_detecting);
        mTasks.clear();
        mAdapter.notifyDataSetChanged();
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort,
                WorkspaceCommandBuilder.buildProjectMetadataCommand(mProjectPath), MAX_METADATA_BYTES);
            mMainHandler.post(() -> showResult(result));
        });
    }

    private void showResult(RemoteCommandRunner.Result result) {
        if (isFinishing() || isDestroyed()) return;
        mProgress.setVisibility(View.GONE);
        if (result.exitCode != 0) {
            showError(R.string.project_tasks_failed);
            return;
        }
        try {
            ProjectTaskDetector.ProjectInfo info = ProjectTaskDetector.parse(result.output);
            mType.setText(getString(R.string.project_tasks_type, info.type));
            mTasks.addAll(info.tasks);
            mAdapter.notifyDataSetChanged();
            if (mTasks.isEmpty()) showError(R.string.project_tasks_empty);
        } catch (JSONException exception) {
            showError(R.string.project_tasks_invalid_metadata);
        }
    }

    private void showError(int message) {
        mProgress.setVisibility(View.GONE);
        mType.setText(message);
    }

    private void confirmTask(ProjectTaskDetector.Task task) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.project_tasks_confirm_title, task.label))
            .setMessage(getString(R.string.project_tasks_confirm_message, task.command))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.project_tasks_run, (dialog, which) -> runTask(task))
            .show();
    }

    private void runTask(ProjectTaskDetector.Task task) {
        String startup = WorkspaceCommandBuilder.buildSshTaskCommand(
            mHost, mPort, mProjectPath, task.command);
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, startup)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }
}
