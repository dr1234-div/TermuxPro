package com.termux.app;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 通过已认证的 OpenSSH 复用连接读取只读 Git diff，并使用原生界面着色展示。 */
public final class GitDiffActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final String EXTRA_PATH = "path";
    private static final int MAX_OUTPUT_BYTES = 1_500_000;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final RemoteCommandRunner mRunner = new RemoteCommandRunner();
    private TextView mContent;
    private ProgressBar mProgress;

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
        mProgress = findViewById(R.id.git_diff_progress);
        findViewById(R.id.git_diff_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.git_diff_refresh_button).setOnClickListener(view -> loadDiff());
        loadDiff();
    }

    private void loadDiff() {
        String host = getIntent().getStringExtra(EXTRA_HOST);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        int port = getIntent().getIntExtra(EXTRA_PORT, 22);
        if (host == null || path == null || port < 1 || port > 65535) {
            showResult(new CommandResult(-1, getString(R.string.git_diff_invalid_workspace), false));
            return;
        }

        mRunner.cancel();
        mProgress.setVisibility(View.VISIBLE);
        mContent.setText(R.string.git_diff_loading);
        mExecutor.execute(() -> showResultOnMain(runGitDiff(host, port, path)));
    }

    @NonNull
    private CommandResult runGitDiff(@NonNull String host, int port, @NonNull String path) {
        RemoteCommandRunner.Result result = mRunner.run(host, port,
            WorkspaceCommandBuilder.buildGitDiffRemoteCommand(path), MAX_OUTPUT_BYTES);
        if (result.exitCode == RemoteCommandRunner.ERROR_SSH_MISSING) {
            return new CommandResult(-1, getString(R.string.git_diff_ssh_missing), false);
        }
        if (result.exitCode == RemoteCommandRunner.ERROR_INTERRUPTED) {
            return new CommandResult(-1, getString(R.string.git_diff_cancelled), false);
        }
        if (result.exitCode == RemoteCommandRunner.ERROR_PROCESS) {
            return new CommandResult(-1, getString(R.string.git_diff_connection_error,
                result.errorType == null ? "Process" : result.errorType), false);
        }
        return new CommandResult(result.exitCode, result.output, result.truncated);
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
            output = getString(R.string.git_diff_failed, result.exitCode) + "\n\n" + output;
        } else if (output.trim().isEmpty()) {
            output = getString(R.string.git_diff_clean);
        }
        if (result.truncated) output += "\n\n" + getString(R.string.git_diff_truncated);
        mContent.setText(colorize(output));
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

        CommandResult(int exitCode, @NonNull String output, boolean truncated) {
            this.exitCode = exitCode;
            this.output = output;
            this.truncated = truncated;
        }
    }
}
