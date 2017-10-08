package com.blink.browser.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blink.browser.R;

public class ToastUtil {

    private static Toast mToast = null;

    public static void showShortToast(Context context, int retId) {
        if (mToast == null) {
            mToast = toastShow(context, retId, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(retId);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }


    public static void showShortToastByString(Context context, String hint) {
        if (mToast == null) {
            mToast = toastShow(context, hint, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(hint);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }


    public static void showLongToast(Context context, int retId) {
        if (mToast == null) {
            mToast = toastShow(context, retId, Toast.LENGTH_LONG);
        } else {
            mToast.setText(retId);
            mToast.setDuration(Toast.LENGTH_LONG);
        }
        mToast.show();
    }


    public static void showLongToastByString(Context context, String hint) {
        if (mToast == null) {
            mToast = toastShow(context, hint, Toast.LENGTH_LONG);
        } else {
            mToast.setText(hint);
            mToast.setDuration(Toast.LENGTH_LONG);
        }
        mToast.show();
    }

    public static Toast toastShow(Context context, int rid, int duration) {

        return toastShow(context, context.getResources().getText(rid), duration);
    }

    public static Toast toastShow(Context context, CharSequence tvString, int duration) {
        View layout = LayoutInflater.from(context).inflate(R.layout.browser_toast, null);
        TextView text = (TextView) layout.findViewById(android.R.id.message);
        text.setText(tvString);
        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setView(layout);
        return toast;
    }
}
