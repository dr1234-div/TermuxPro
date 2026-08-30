package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.termux.R;

/** 统一 AI 会话选择弹窗的大字体可读性与品牌色。 */
final class AiSessionDialog {

    private AiSessionDialog() {}

    static void applyReadableStyle(Activity activity, AlertDialog dialog) {
        TermuxProDialogStyle.apply(activity, dialog);

        // 会话选择项本身承担确认动作，底部只有“取消”一个按钮；继续使用品牌主色，
        // 避免套用双按钮弹窗的次要操作色后降低可发现性。
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                ContextCompat.getColor(activity, R.color.tp_primary));
        }

        ListView list = dialog.getListView();
        int minimumEndPadding = Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 32, activity.getResources().getDisplayMetrics()));
        for (int index = 0; index < list.getChildCount(); index++) {
            View row = list.getChildAt(index);
            if (!(row instanceof TextView)) continue;
            TextView label = (TextView) row;
            label.setSingleLine(false);
            label.setMaxLines(2);
            label.setEllipsize(null);
            label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            // 预留明确的行尾换行空间，避免 200% 字体在系统弹窗右边缘只截出半个词。
            label.setPaddingRelative(label.getPaddingStart(), label.getPaddingTop(),
                Math.max(label.getPaddingEnd(), minimumEndPadding), label.getPaddingBottom());
        }
    }
}
