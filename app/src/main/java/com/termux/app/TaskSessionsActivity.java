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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 查看当前远端用户的全部 tmux 会话，并安全进入或管理 TermuxPro 自有会话。 */
public final class TaskSessionsActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private final List<TmuxSessionInfo> mSessions = new ArrayList<>();
    private String mHost;
    private int mPort;
    private ProgressBar mProgress;
    private TextView mStatus;
    private ArrayAdapter<TmuxSessionInfo> mAdapter;

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String host, int port) {
        return new Intent(context, TaskSessionsActivity.class)
            .putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_sessions);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mProgress = findViewById(R.id.task_sessions_progress);
        mStatus = findViewById(R.id.task_sessions_status);
        ListView list = findViewById(R.id.task_sessions_list);
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
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) -> showActions(mSessions.get(position)));
        findViewById(R.id.task_sessions_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.task_sessions_refresh_button).setOnClickListener(view -> loadSessions());
        loadSessions();
    }

    private void loadSessions() {
        mRunner.cancel();
        mSessions.clear();
        mAdapter.notifyDataSetChanged();
        mProgress.setVisibility(View.VISIBLE);
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
        if (result.exitCode != 0) {
            mStatus.setText(getString(R.string.task_sessions_failed, result.exitCode));
            return;
        }
        if (TmuxSessionParser.reportsMissingTmux(result.output)) {
            mStatus.setText(R.string.task_sessions_tmux_missing);
            return;
        }
        mSessions.addAll(TmuxSessionParser.parse(result.output));
        mAdapter.notifyDataSetChanged();
        mStatus.setText(mSessions.isEmpty() ? R.string.task_sessions_empty : R.string.task_sessions_ready);
    }

    private String sessionRow(TmuxSessionInfo session) {
        String state = session.attached ? getString(R.string.task_sessions_attached) :
            getString(R.string.task_sessions_background);
        String ownership = session.managedByTermuxPro ?
            getString(R.string.task_sessions_owned) : getString(R.string.task_sessions_unknown_owner);
        return getString(R.string.task_sessions_row, session.name, session.windows, state, ownership);
    }

    private void showActions(TmuxSessionInfo session) {
        String[] actions = session.managedByTermuxPro ? new String[]{
            getString(R.string.task_sessions_attach), getString(R.string.task_sessions_stop)
        } : new String[]{getString(R.string.task_sessions_attach)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(session.name);
        if (!session.managedByTermuxPro) {
            builder.setMessage(R.string.task_sessions_unknown_owner_warning);
        }
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) attach(session);
            else confirmStop(session);
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void attach(TmuxSessionInfo session) {
        String command = WorkspaceCommandBuilder.buildAttachTaskSessionCommand(mHost, mPort, session.name);
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, command)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }

    private void confirmStop(TmuxSessionInfo session) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.task_sessions_stop_title)
            .setMessage(getString(R.string.task_sessions_stop_message, session.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.task_sessions_stop, (dialog, which) -> stop(session))
            .show();
    }

    private void stop(TmuxSessionInfo session) {
        mProgress.setVisibility(View.VISIBLE);
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort,
                WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(session.name), 32_000);
            mMainHandler.post(() -> {
                if (result.exitCode == 0) loadSessions();
                else {
                    mProgress.setVisibility(View.GONE);
                    mStatus.setText(getString(R.string.task_sessions_failed, result.exitCode));
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }

}
