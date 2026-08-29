package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 远端文本文件只读预览，正文只保存在内存中。 */
public final class RemoteFilePreviewActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PROJECT_PATH = "project_path";
    private static final String EXTRA_FILE_PATH = "file_path";
    private static final int MAX_RESULT_BYTES = 1_100_000;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private ProgressBar mProgress;
    private TextView mContent;
    private String mHost;
    private int mPort;
    private String mProjectPath;
    private String mFilePath;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull String host, int port,
                                   @NonNull String projectPath, @NonNull String filePath) {
        return new Intent(context, RemoteFilePreviewActivity.class)
            .putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_PROJECT_PATH, projectPath).putExtra(EXTRA_FILE_PATH, filePath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_file_preview);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mProjectPath = getIntent().getStringExtra(EXTRA_PROJECT_PATH);
        mFilePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        mProgress = findViewById(R.id.remote_file_progress);
        mContent = findViewById(R.id.remote_file_content);
        ((TextView) findViewById(R.id.remote_file_title)).setText(mFilePath);
        findViewById(R.id.remote_file_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.remote_file_refresh_button).setOnClickListener(view -> loadFile());
        loadFile();
    }

    private void loadFile() {
        if (mHost == null || mProjectPath == null || mFilePath == null) {
            mContent.setText(R.string.remote_files_invalid_workspace);
            return;
        }
        mRunner.cancel();
        mProgress.setVisibility(View.VISIBLE);
        mContent.setText(R.string.remote_file_loading);
        mExecutor.execute(() -> {
            RemoteCommandRunner.Result result = mRunner.run(mHost, mPort,
                WorkspaceCommandBuilder.buildReadFileRemoteCommand(mProjectPath, mFilePath), MAX_RESULT_BYTES);
            mMainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) showResult(result);
            });
        });
    }

    private void showResult(RemoteCommandRunner.Result result) {
        mProgress.setVisibility(View.GONE);
        if (result.exitCode != 0) {
            mContent.setText(getString(R.string.remote_file_failed, result.exitCode));
            return;
        }
        int markerEnd = result.output.indexOf('\u0000');
        String marker = markerEnd < 0 ? "" : result.output.substring(0, markerEnd);
        if ("BINARY".equals(marker)) {
            mContent.setText(R.string.remote_file_binary);
        } else if ("TEXT".equals(marker)) {
            String text = result.output.substring(markerEnd + 1);
            if (text.length() >= 1_000_000) text += "\n\n" + getString(R.string.remote_file_truncated);
            mContent.setText(text);
        } else {
            mContent.setText(R.string.remote_file_failed_format);
        }
    }

    @Override
    protected void onDestroy() {
        mRunner.cancel();
        mExecutor.shutdownNow();
        super.onDestroy();
    }
}
