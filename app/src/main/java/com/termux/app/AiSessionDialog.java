package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.termux.R;

/** 统一 AI 会话选择弹窗的大字体可读性与品牌色。 */
final class AiSessionDialog {

    private AiSessionDialog() {}

    static void applyReadableStyle(Activity activity, AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(activity, R.color.tp_primary));

        ListView list = dialog.getListView();
        for (int index = 0; index < list.getChildCount(); index++) {
            View row = list.getChildAt(index);
            if (!(row instanceof TextView)) continue;
            TextView label = (TextView) row;
            label.setSingleLine(false);
            label.setMaxLines(2);
            label.setEllipsize(null);
            label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
    }
}
