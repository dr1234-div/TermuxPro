package com.termux.app;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** tmux 会话中心的安全展示顺序：先让当前工作区上下文可见，再展示只读风险对象。 */
final class TmuxSessionDisplayOrder {
    private TmuxSessionDisplayOrder() {}

    @NonNull
    static List<TmuxSessionInfo> sorted(@NonNull List<TmuxSessionInfo> sessions) {
        List<TmuxSessionInfo> result = new ArrayList<>(sessions);
        Collections.sort(result, Comparator
            .comparingInt(TmuxSessionDisplayOrder::ownershipPriority)
            .thenComparing((TmuxSessionInfo session) -> session.activityEpochSeconds,
                Comparator.reverseOrder())
            .thenComparing((TmuxSessionInfo session) -> session.createdEpochSeconds,
                Comparator.reverseOrder())
            .thenComparing(session -> session.name));
        return result;
    }

    private static int ownershipPriority(@NonNull TmuxSessionInfo session) {
        switch (session.ownershipState) {
            case CURRENT_WORKSPACE:
                return 0;
            case OTHER_WORKSPACE:
                return 1;
            case OTHER_OWNER:
                return 2;
            case INCOMPLETE_MARKER:
                return 3;
            case UNMARKED:
            default:
                return 4;
        }
    }
}
