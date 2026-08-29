package com.termux.app;

import androidx.annotation.NonNull;

import com.termux.shared.termux.TermuxConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 使用已认证的 OpenSSH 复用连接执行有限输出的远端只读命令。 */
final class RemoteCommandRunner {

    static final int ERROR_SSH_MISSING = -1001;
    static final int ERROR_INTERRUPTED = -1002;
    static final int ERROR_PROCESS = -1003;

    private volatile Process mProcess;

    @NonNull
    Result run(@NonNull String host, int port, @NonNull String remoteCommand, int maxOutputBytes) {
        File ssh = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "ssh");
        if (!ssh.canExecute()) return new Result(ERROR_SSH_MISSING, "", false, null);

        List<String> command = new ArrayList<>();
        command.add(ssh.getAbsolutePath());
        command.add("-T");
        command.add("-o"); command.add("BatchMode=yes");
        command.add("-o"); command.add("ControlMaster=auto");
        command.add("-o"); command.add("ControlPersist=600");
        command.add("-o"); command.add("ControlPath=" + WorkspaceCommandBuilder.CONTROL_PATH);
        command.add("-o"); command.add("ConnectTimeout=8");
        command.add("-p"); command.add(String.valueOf(port));
        command.add("--"); command.add(host);
        command.add(remoteCommand);

        try {
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            Map<String, String> environment = builder.environment();
            environment.put("HOME", TermuxConstants.TERMUX_HOME_DIR_PATH);
            environment.put("PREFIX", TermuxConstants.TERMUX_PREFIX_DIR_PATH);
            environment.put("TMPDIR", TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH);
            environment.put("PATH", TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + ":/system/bin");
            mProcess = builder.start();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            boolean truncated = false;
            try (InputStream stream = mProcess.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) >= 0) {
                    int remaining = maxOutputBytes - output.size();
                    if (remaining > 0) output.write(buffer, 0, Math.min(count, remaining));
                    if (count > remaining) truncated = true;
                }
            }
            int exitCode = mProcess.waitFor();
            return new Result(exitCode, output.toString(StandardCharsets.UTF_8.name()), truncated, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result(ERROR_INTERRUPTED, "", false, null);
        } catch (Exception exception) {
            return new Result(ERROR_PROCESS, "", false, exception.getClass().getSimpleName());
        } finally {
            mProcess = null;
        }
    }

    void cancel() {
        Process process = mProcess;
        if (process != null) process.destroy();
    }

    static final class Result {
        final int exitCode;
        final String output;
        final boolean truncated;
        final String errorType;

        Result(int exitCode, @NonNull String output, boolean truncated, String errorType) {
            this.exitCode = exitCode;
            this.output = output;
            this.truncated = truncated;
            this.errorType = errorType;
        }
    }
}
