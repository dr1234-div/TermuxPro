package com.termux.shared.logger;

/** 由前台页面实现，以应用自有样式承接可见反馈；后台 Context 继续使用系统渠道。 */
public interface ForegroundFeedbackHost {
    void showForegroundFeedback(String text, boolean longDuration);
}
