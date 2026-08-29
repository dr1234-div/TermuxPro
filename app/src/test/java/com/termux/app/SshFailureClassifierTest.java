package com.termux.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SshFailureClassifierTest {

    @Test
    public void classifiesOpenSshFailuresWithoutDependingOnExitCode() {
        assertReason(SshFailureClassifier.Reason.DNS_FAILED,
            "ssh: Could not resolve hostname missing: Name or service not known");
        assertReason(SshFailureClassifier.Reason.TIMEOUT,
            "ssh: connect to host 10.0.0.2 port 22: Connection timed out");
        assertReason(SshFailureClassifier.Reason.REFUSED,
            "ssh: connect to host localhost port 2: Connection refused");
        assertReason(SshFailureClassifier.Reason.NO_ROUTE,
            "ssh: connect to host 192.0.2.1 port 22: No route to host");
        assertReason(SshFailureClassifier.Reason.AUTH_FAILED,
            "user@example.com: Permission denied (publickey,password).");
        assertReason(SshFailureClassifier.Reason.HOST_KEY_UNVERIFIED,
            "Host key verification failed.");
        assertReason(SshFailureClassifier.Reason.CONNECTION_CLOSED,
            "kex_exchange_identification: Connection closed by remote host");
    }

    @Test
    public void changedHostKeyTakesPriorityOverGenericVerificationFailure() {
        assertReason(SshFailureClassifier.Reason.HOST_KEY_CHANGED,
            "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED! Host key verification failed.");
    }

    @Test
    public void classifiesLocalRunnerFailures() {
        assertEquals(SshFailureClassifier.Reason.SSH_MISSING,
            classify(RemoteCommandRunner.ERROR_SSH_MISSING, ""));
        assertEquals(SshFailureClassifier.Reason.INTERRUPTED,
            classify(RemoteCommandRunner.ERROR_INTERRUPTED, ""));
        assertEquals(SshFailureClassifier.Reason.PROCESS_ERROR,
            classify(RemoteCommandRunner.ERROR_PROCESS, ""));
    }

    private void assertReason(SshFailureClassifier.Reason expected, String output) {
        assertEquals(expected, classify(255, output));
    }

    private SshFailureClassifier.Reason classify(int exitCode, String output) {
        return SshFailureClassifier.classify(
            new RemoteCommandRunner.Result(exitCode, output, false, null));
    }
}
