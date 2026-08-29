package com.termux.app;

import androidx.annotation.NonNull;

import java.util.Locale;

/** 将 OpenSSH 常见失败输出归类为不泄露凭据、可操作的用户错误。 */
final class SshFailureClassifier {

    enum Reason {
        SSH_MISSING,
        INTERRUPTED,
        PROCESS_ERROR,
        DNS_FAILED,
        TIMEOUT,
        REFUSED,
        NO_ROUTE,
        HOST_KEY_CHANGED,
        HOST_KEY_UNVERIFIED,
        AUTH_FAILED,
        CONNECTION_CLOSED,
        UNKNOWN
    }

    private SshFailureClassifier() {}

    @NonNull
    static Reason classify(@NonNull RemoteCommandRunner.Result result) {
        if (result.exitCode == RemoteCommandRunner.ERROR_SSH_MISSING) return Reason.SSH_MISSING;
        if (result.exitCode == RemoteCommandRunner.ERROR_INTERRUPTED) return Reason.INTERRUPTED;
        if (result.exitCode == RemoteCommandRunner.ERROR_PROCESS) return Reason.PROCESS_ERROR;

        String output = result.output.toLowerCase(Locale.ROOT);
        if (output.contains("remote host identification has changed")) return Reason.HOST_KEY_CHANGED;
        if (output.contains("host key verification failed") || output.contains("no host key is known")) {
            return Reason.HOST_KEY_UNVERIFIED;
        }
        if (output.contains("could not resolve hostname") || output.contains("name or service not known")
            || output.contains("nodename nor servname provided")) return Reason.DNS_FAILED;
        if (output.contains("connection timed out") || output.contains("operation timed out")
            || output.contains("connection timeout")) return Reason.TIMEOUT;
        if (output.contains("connection refused")) return Reason.REFUSED;
        if (output.contains("no route to host") || output.contains("network is unreachable")) {
            return Reason.NO_ROUTE;
        }
        if (output.contains("permission denied") || output.contains("authentication failed")
            || output.contains("too many authentication failures")
            || output.contains("no supported authentication methods available")) {
            return Reason.AUTH_FAILED;
        }
        if (output.contains("connection closed") || output.contains("connection reset")
            || output.contains("kex_exchange_identification")) return Reason.CONNECTION_CLOSED;
        return Reason.UNKNOWN;
    }
}
