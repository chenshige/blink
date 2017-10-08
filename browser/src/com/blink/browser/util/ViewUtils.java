package com.blink.browser.util;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ViewUtils {

    /**
     * 获取控件的高度
     */
    public static int getHeightOfView(View view) {
        if (view == null)
            return -1;
        int w = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
        int height = view.getMeasuredHeight();
        return height;

    }

    /**
     * 获取控件的宽度
     */
    public static int getWidthOfView(View view) {
        if (view == null)
            return -1;
        int w = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
        int width = view.getMeasuredWidth();
        return width;

    }

    /**
     * 控件是否为ImageView
     */
    public boolean isImageView(View view) {
        return view == null ? false : view instanceof ImageView;
    }

    /**
     * 控件是否为TextView
     */
    public boolean isTextView(View view) {
        return view == null ? false : view instanceof TextView;
    }

    /**
     * 控件是否为RelativeLayout
     */
    public boolean isRelativeLayout(View view) {
        return view == null ? false : view instanceof RelativeLayout;
    }

    /**
     * 控件是否为LinearLayout
     */
    public boolean isLinearLayout(View view) {
        return view == null ? false : view instanceof LinearLayout;
    }
}
