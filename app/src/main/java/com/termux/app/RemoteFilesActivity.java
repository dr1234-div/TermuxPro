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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 通过 SSH 复用连接浏览远端项目，文件操作保持只读。 */
public final class RemoteFilesActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PROJECT_PATH = "project_path";
    private static final int MAX_LIST_BYTES = 512_000;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private final List<FileEntry> mEntries = new ArrayList<>();
    private ArrayAdapter<FileEntry> mAdapter;
    private ProgressBar mProgress;
    private TextView mPathView;
    private String mHost;
    private int mPort;
    private String mProjectPath;
    private String mCurrentDirectory = ".";

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull String host, int port,
                                   @NonNull String projectPath) {
        return new Intent(context, RemoteFilesActivity.class)
            .putExtra(EXTRA_HOST, host)
            .putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_PROJECT_PATH, projectPath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_files);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mProjectPath = getIntent().getStringExtra(EXTRA_PROJECT_PATH);
        mProgress = findViewById(R.id.remote_files_progress);
        mPathView = findViewById(R.id.remote_files_path);
        ListView list = findViewById(R.id.remote_files_list);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mEntries);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) -> openEntry(mEntries.get(position)));
        findViewById(R.id.remote_files_back_button).setOnClickListener(view -> navigateBack());
        findViewById(R.id.remote_files_refresh_button).setOnClickListener(view -> loadDirectory());

        if (mHost == null || mProjectPath == null || mPort < 1 || mPort > 65535) {
            showError(getString(R.string.remote_files_invalid_workspace));
        } else {
            loadDirectory();
        }
    }

    private void loadDirectory() {
        mRunner.cancel();
        mProgress.setVisibility(View.VISIBLE);
        mPathView.setText(mCurrentDirectory);
        mEntries.clear();
        mAdapter.notifyDataSetChanged();
        final String directory = mCurrentDirectory;
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort,
                WorkspaceCommandBuilder.buildListFilesRemoteCommand(mProjectPath, directory), MAX_LIST_BYTES);
            mMainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) showDirectoryResult(directory, result);
            });
        });
    }

    private void showDirectoryResult(String requestedDirectory, RemoteCommandRunner.Result result) {
        if (!requestedDirectory.equals(mCurrentDirectory)) return;
        mProgress.setVisibility(View.GONE);
        if (result.exitCode != 0) {
            showError(remoteErrorMessage(result));
            return;
        }
        String[] fields = result.output.split("\u0000", -1);
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (fields[index].isEmpty() || fields[index + 1].isEmpty()) continue;
            mEntries.add(new FileEntry(fields[index].charAt(0), fields[index + 1]));
        }
        Collections.sort(mEntries, Comparator
            .comparing((FileEntry entry) -> !entry.isDirectory())
            .thenComparing(entry -> entry.name.toLowerCase(Locale.ROOT)));
        if (result.truncated) {
            mEntries.add(new FileEntry('!', getString(R.string.remote_files_truncated)));
        }
        mAdapter.notifyDataSetChanged();
        if (mEntries.isEmpty()) showError(getString(R.string.remote_files_empty));
    }

    private void openEntry(FileEntry entry) {
        if (entry.type == '!') return;
        String path = childPath(mCurrentDirectory, entry.name);
        if (entry.isDirectory()) {
            mCurrentDirectory = path;
            loadDirectory();
        } else if (entry.isRegularFile()) {
            startActivity(RemoteFilePreviewActivity.newIntent(this, mHost, mPort, mProjectPath, path));
        }
    }

    private String childPath(String directory, String name) {
        return ".".equals(directory) ? "./" + name : directory + "/" + name;
    }

    private void navigateBack() {
        if (".".equals(mCurrentDirectory)) {
            finish();
            return;
        }
        int slash = mCurrentDirectory.lastIndexOf('/');
        mCurrentDirectory = slash <= 1 ? "." : mCurrentDirectory.substring(0, slash);
        loadDirectory();
    }

    @Override
    public void onBackPressed() {
        navigateBack();
    }

    private void showError(String message) {
        mProgress.setVisibility(View.GONE);
        mEntries.clear();
        mEntries.add(new FileEntry('!', message));
        mAdapter.notifyDataSetChanged();
    }

    private String remoteErrorMessage(RemoteCommandRunner.Result result) {
        if (result.exitCode == RemoteCommandRunner.ERROR_SSH_MISSING) {
            return getString(R.string.git_diff_ssh_missing);
        }
        if (result.exitCode == RemoteCommandRunner.ERROR_INTERRUPTED) {
            return getString(R.string.git_diff_cancelled);
        }
        return getString(R.string.remote_files_failed, result.exitCode);
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    private static final class FileEntry {
        final char type;
        final String name;

        FileEntry(char type, @NonNull String name) {
            this.type = type;
            this.name = name;
        }

        boolean isDirectory() {
            return type == 'd';
        }

        boolean isRegularFile() {
            return type == 'f';
        }

        @NonNull
        @Override
        public String toString() {
            if (type == '!') return name;
            String safeName = name.replace("\n", "↵").replace("\t", "⇥");
            String prefix = isDirectory() ? "▸  " : (isRegularFile() ? "   " : "◇  ");
            return prefix + safeName;
        }
    }
}
