package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class SshDiagnosticStagesTest {

    @Test
    public void authenticationFailureProvesNetworkAndHostIdentityOnly() {
        List<SshDiagnosticStages.Item> stages = SshDiagnosticStages.failure(
            SshFailureClassifier.Reason.AUTH_FAILED);

        assertEquals(SshDiagnosticStages.State.PASSED, stages.get(0).state);
        assertEquals(SshDiagnosticStages.State.PASSED, stages.get(1).state);
        assertEquals(SshDiagnosticStages.State.ACTION_REQUIRED, stages.get(2).state);
        assertEquals(SshDiagnosticStages.State.PENDING, stages.get(3).state);
        assertTrue(SshDiagnosticStages.canOpenInteractiveConnection(
            SshFailureClassifier.Reason.AUTH_FAILED));
    }

    @Test
    public void networkFailureDoesNotClaimLaterStagesPassed() {
        List<SshDiagnosticStages.Item> stages = SshDiagnosticStages.failure(
            SshFailureClassifier.Reason.REFUSED);

        assertEquals(SshDiagnosticStages.State.FAILED, stages.get(0).state);
        assertEquals(SshDiagnosticStages.State.PENDING, stages.get(1).state);
        assertEquals(SshDiagnosticStages.State.PENDING, stages.get(2).state);
        assertEquals(SshDiagnosticStages.State.PENDING, stages.get(3).state);
        assertFalse(SshDiagnosticStages.canOpenInteractiveConnection(
            SshFailureClassifier.Reason.REFUSED));
    }

    @Test
    public void changedHostKeyBlocksInteractiveShortcut() {
        List<SshDiagnosticStages.Item> stages = SshDiagnosticStages.failure(
            SshFailureClassifier.Reason.HOST_KEY_CHANGED);

        assertEquals(SshDiagnosticStages.State.PASSED, stages.get(0).state);
        assertEquals(SshDiagnosticStages.State.FAILED, stages.get(1).state);
        assertFalse(SshDiagnosticStages.canOpenInteractiveConnection(
            SshFailureClassifier.Reason.HOST_KEY_CHANGED));
    }

    @Test
    public void successfulRemoteCommandProvesAllStages() {
        for (SshDiagnosticStages.Item item : SshDiagnosticStages.success()) {
            assertEquals(SshDiagnosticStages.State.PASSED, item.state);
        }
    }

    @Test
    public void malformedRemoteReportDoesNotHideAuthenticatedConnectionProgress() {
        List<SshDiagnosticStages.Item> stages = SshDiagnosticStages.invalidRemoteEnvironment();

        assertEquals(SshDiagnosticStages.State.PASSED, stages.get(0).state);
        assertEquals(SshDiagnosticStages.State.PASSED, stages.get(1).state);
        assertEquals(SshDiagnosticStages.State.PASSED, stages.get(2).state);
        assertEquals(SshDiagnosticStages.State.FAILED, stages.get(3).state);
    }
}
