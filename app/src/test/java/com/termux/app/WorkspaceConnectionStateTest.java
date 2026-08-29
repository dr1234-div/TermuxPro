package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

/** 验证连接事实分级和按工作区隔离持久化。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WorkspaceConnectionStateTest {

    @Test
    public void terminalOpenedDoesNotClaimAuthenticationSucceeded() {
        WorkspaceConnectionState state = WorkspaceConnectionState.terminalOpened(1234L);
        assertEquals(WorkspaceConnectionState.Status.TERMINAL_OPENED, state.status);
        assertNull(state.stage);
    }

    @Test
    public void diagnosticStagesPreserveActionAndFailureBoundary() {
        WorkspaceConnectionState action = WorkspaceConnectionState.fromStages(
            SshDiagnosticStages.failure(SshFailureClassifier.Reason.AUTH_FAILED), 2000L);
        assertEquals(WorkspaceConnectionState.Status.ACTION_REQUIRED, action.status);
        assertEquals(SshDiagnosticStages.Stage.AUTHENTICATION, action.stage);

        WorkspaceConnectionState failed = WorkspaceConnectionState.fromStages(
            SshDiagnosticStages.failure(SshFailureClassifier.Reason.REFUSED), 3000L);
        assertEquals(WorkspaceConnectionState.Status.FAILED, failed.status);
        assertEquals(SshDiagnosticStages.Stage.NETWORK, failed.stage);

        WorkspaceConnectionState verified = WorkspaceConnectionState.fromStages(
            SshDiagnosticStages.success(), 4000L);
        assertEquals(WorkspaceConnectionState.Status.VERIFIED, verified.status);
        assertNull(verified.stage);

        WorkspaceConnectionState unknown = WorkspaceConnectionState.fromStages(
            SshDiagnosticStages.failure(SshFailureClassifier.Reason.UNKNOWN), 4500L);
        assertEquals(WorkspaceConnectionState.Status.UNKNOWN, unknown.status);
        assertNull(unknown.stage);

        WorkspaceConnectionState empty = WorkspaceConnectionState.fromStages(
            Collections.emptyList(), 4600L);
        assertEquals(WorkspaceConnectionState.Status.UNKNOWN, empty.status);
    }

    @Test
    public void storeKeepsOnlyStatePerWorkspaceAndCanClearIt() {
        Context context = RuntimeEnvironment.getApplication();
        WorkspaceConnectionStateStore store = new WorkspaceConnectionStateStore(context);
        store.save("workspace-a", new WorkspaceConnectionState(
            WorkspaceConnectionState.Status.FAILED, SshDiagnosticStages.Stage.NETWORK, 5000L));

        WorkspaceConnectionState restored = store.read("workspace-a");
        assertEquals(WorkspaceConnectionState.Status.FAILED, restored.status);
        assertEquals(SshDiagnosticStages.Stage.NETWORK, restored.stage);
        assertEquals(5000L, restored.timestamp);
        assertNull(store.read("workspace-b"));

        store.clear("workspace-a");
        assertNull(store.read("workspace-a"));
    }
}
