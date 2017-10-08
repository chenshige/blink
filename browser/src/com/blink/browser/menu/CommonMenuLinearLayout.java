package com.blink.browser.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class CommonMenuLinearLayout extends LinearLayout{
    public CommonMenuLinearLayout(Context context) {
        super(context);
    }

    public CommonMenuLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int count = getChildCount();
        int parentWidth = getWidth();
        int width = getChildAt(0).getMeasuredWidth();
        int height = getChildAt(0).getMeasuredHeight();
        int margin = (parentWidth - getChildAt(0).getMeasuredWidth() * count) / (count - 1);
        for (int i = 0; i < count; i++) {
            getChildAt(i).layout(i * (width + margin), 0, width * (i + 1) + i * margin, height);
        }
    }
}
