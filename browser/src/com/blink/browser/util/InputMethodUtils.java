package com.blink.browser.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class InputMethodUtils {

    /**
     * 隐藏软键盘
     */
    public static void hideKeyboard(Activity activity) {
        if (null != activity && activity.getWindow().getAttributes().softInputMode != WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN && activity.getCurrentFocus() != null) {
            InputMethodManager manager = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(activity.getCurrentFocus()
                    .getWindowToken(), 0);
        }
    }

    /**
     * 显示输入法键盘
     */
    public static void showKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager
                .HIDE_NOT_ALWAYS);
    }

    /**
     * dailog 隐藏软键盘
     *
     * @param dialog
     */
    public static void hideKeyboard(Dialog dialog) {
        if (null != dialog && dialog.getWindow().getAttributes().softInputMode != WindowManager
                .LayoutParams
                .SOFT_INPUT_STATE_HIDDEN && dialog.getCurrentFocus() != null) {
            InputMethodManager manager = (InputMethodManager) dialog.getContext().getApplicationContext()
                    .getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(dialog.getCurrentFocus()
                    .getWindowToken(), 0);
        }
    }

    public static void showKeyboard(Dialog dialog) {
        InputMethodManager imm = (InputMethodManager) dialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInputFromWindow(dialog.getWindow().getDecorView().getWindowToken(), 0, InputMethodManager
                .SHOW_FORCED);
    }

    public static void showKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInputFromWindow(editText.getWindowToken(), 0, InputMethodManager
                .SHOW_FORCED);
    }

    public static void hideKeyboard(EditText editText) {
        InputMethodManager manager = (InputMethodManager) editText.getContext().getApplicationContext()
                .getSystemService(
                        Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
