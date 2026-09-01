package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.R;

/** 为产品深色主题弹窗提供不依赖厂商默认值的文字和操作色。 */
public final class TermuxProDialogStyle {
    private TermuxProDialogStyle() {}

    public interface OnDialogShown {
        void onShown(AlertDialog dialog);
    }

    public static void show(Activity activity, AlertDialog dialog) {
        show(activity, dialog, null);
    }

    public static void show(Activity activity, AlertDialog dialog,
                            @Nullable OnDialogShown onDialogShown) {
        prepare(activity, dialog, onDialogShown);
        dialog.show();
    }

    public static void prepare(Activity activity, AlertDialog dialog,
                               @Nullable OnDialogShown onDialogShown) {
        dialog.setOnShowListener(ignored -> {
            apply(activity, dialog);
            if (onDialogShown != null) onDialogShown.onShown(dialog);
        });
    }

    public static void apply(Activity activity, AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(
                ContextCompat.getColor(activity, R.color.tp_surface_elevated)));
            applyTextColors(activity, window.getDecorView());
        }
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
        ListView list = dialog.getListView();
        if (list != null) {
            list.setBackgroundColor(ContextCompat.getColor(activity, R.color.tp_surface_elevated));
            list.setCacheColorHint(ContextCompat.getColor(activity, R.color.tp_surface_elevated));
            list.setDivider(new ColorDrawable(ContextCompat.getColor(activity, R.color.tp_divider)));
            list.setDividerHeight(1);
            applyListChildrenColors(activity, list);
            list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    applyTextColors(activity, child);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {}
            });
            list.post(() -> applyListChildrenColors(activity, list));
        }
    }

    private static void applyListChildrenColors(Activity activity, ListView list) {
        for (int index = 0; index < list.getChildCount(); index++) {
            applyTextColors(activity, list.getChildAt(index));
        }
    }

    private static void applyTextColors(Activity activity, View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(ContextCompat.getColor(activity, R.color.tp_text_primary));
            if (text instanceof EditText) {
                ((EditText) text).setHintTextColor(
                    ContextCompat.getColor(activity, R.color.tp_text_secondary));
            }
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            applyTextColors(activity, group.getChildAt(index));
        }
    }
}
