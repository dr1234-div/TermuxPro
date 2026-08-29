package com.termux.app;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

/** 根据 OpenSSH 的可证明进度生成分层状态，不把未知阶段误报为成功。 */
final class SshDiagnosticStages {

    enum Stage { NETWORK, HOST_IDENTITY, AUTHENTICATION, REMOTE_ENVIRONMENT }
    enum State { PASSED, ACTION_REQUIRED, FAILED, PENDING }

    static final class Item {
        final Stage stage;
        final State state;

        Item(Stage stage, State state) {
            this.stage = stage;
            this.state = state;
        }
    }

    private SshDiagnosticStages() {}

    @NonNull
    static List<Item> success() {
        return stages(State.PASSED, State.PASSED, State.PASSED, State.PASSED);
    }

    @NonNull
    static List<Item> invalidRemoteEnvironment() {
        return stages(State.PASSED, State.PASSED, State.PASSED, State.FAILED);
    }

    @NonNull
    static List<Item> failure(@NonNull SshFailureClassifier.Reason reason) {
        switch (reason) {
            case DNS_FAILED:
            case TIMEOUT:
            case REFUSED:
            case NO_ROUTE:
                return stages(State.FAILED, State.PENDING, State.PENDING, State.PENDING);
            case HOST_KEY_UNVERIFIED:
                return stages(State.PASSED, State.ACTION_REQUIRED, State.PENDING, State.PENDING);
            case HOST_KEY_CHANGED:
                return stages(State.PASSED, State.FAILED, State.PENDING, State.PENDING);
            case AUTH_FAILED:
                return stages(State.PASSED, State.PASSED, State.ACTION_REQUIRED, State.PENDING);
            case CONNECTION_CLOSED:
                return stages(State.PASSED, State.FAILED, State.PENDING, State.PENDING);
            default:
                return stages(State.PENDING, State.PENDING, State.PENDING, State.PENDING);
        }
    }

    static boolean canOpenInteractiveConnection(@NonNull SshFailureClassifier.Reason reason) {
        return reason == SshFailureClassifier.Reason.HOST_KEY_UNVERIFIED
            || reason == SshFailureClassifier.Reason.AUTH_FAILED;
    }

    private static List<Item> stages(State network, State hostIdentity, State authentication,
                                     State remoteEnvironment) {
        return Arrays.asList(
            new Item(Stage.NETWORK, network),
            new Item(Stage.HOST_IDENTITY, hostIdentity),
            new Item(Stage.AUTHENTICATION, authentication),
            new Item(Stage.REMOTE_ENVIRONMENT, remoteEnvironment)
        );
    }
}
