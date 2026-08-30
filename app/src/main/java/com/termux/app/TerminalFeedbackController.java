package com.termux.app;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/** 管理终端页应用内反馈，确保文案、显式配色载体和生命周期始终一起更新。 */
final class TerminalFeedbackController {

    static final long SHORT_DURATION_MS = 3500L;
    static final long LONG_DURATION_MS = 6000L;

    private final TextView banner;
    private Runnable hideAction;

    TerminalFeedbackController(TextView banner) {
        this.banner = banner;
    }

    void show(String text, boolean longDuration) {
        if (TextUtils.isEmpty(text) || text.trim().isEmpty()) return;
        if (hideAction != null) banner.removeCallbacks(hideAction);
        banner.setText(text);
        banner.setContentDescription(text);
        banner.setVisibility(View.VISIBLE);
        banner.bringToFront();
        banner.announceForAccessibility(text);
        hideAction = this::hide;
        banner.postDelayed(hideAction,
            longDuration ? LONG_DURATION_MS : SHORT_DURATION_MS);
    }

    void hide() {
        if (hideAction != null) banner.removeCallbacks(hideAction);
        hideAction = null;
        banner.setVisibility(View.GONE);
    }
}
