package com.termux.app;

import android.app.Activity;
import android.content.Intent;

/** 为远程工具提供一致、可预期的工作区恢复入口。 */
final class WorkspaceNavigation {

    private WorkspaceNavigation() {}

    static void returnToWorkspace(Activity activity) {
        Intent intent = new Intent(activity, WorkspaceActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
