package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.termux.R;

/** 为产品深色主题弹窗提供不依赖厂商默认值的文字和操作色。 */
final class TermuxProDialogStyle {
    private TermuxProDialogStyle() {}

    static void apply(Activity activity, AlertDialog dialog) {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                ContextCompat.getColor(activity, R.color.tp_primary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                ContextCompat.getColor(activity, R.color.tp_text_secondary));
        }
        int titleId = activity.getResources().getIdentifier("alertTitle", "id", "android");
        TextView title = dialog.findViewById(titleId);
        if (title != null) {
            title.setTextColor(ContextCompat.getColor(activity, R.color.tp_text_primary));
            title.setMaxLines(2);
        }
        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) {
            message.setTextColor(ContextCompat.getColor(activity, R.color.tp_text_primary));
        }
    }
}
