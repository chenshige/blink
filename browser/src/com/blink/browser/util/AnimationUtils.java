package com.blink.browser.util;

import android.view.View;
import android.view.ViewGroup;

public class AnimationUtils {

    private static final int COLOR_OFFSET = 0xff;
    private static final int COLOR_OFFSET_R = 16;
    private static final int COLOR_OFFSET_G = 8;
    private static final int COLOR_OFFSET_B = 4;
    private static final int ALPHA_OFFSET =24;
    private static final float ALPHA_INIT = 0f;

    /**
     * this is alpha init
     */
    public static void setChildAlpha(ViewGroup viewGroup) {
        viewGroup.setAlpha(ALPHA_INIT);
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                setChildAlpha((ViewGroup) child);
            } else {
                child.setAlpha(ALPHA_INIT);
            }
        }
    }

    /**
     * this is color gradient
     */
    public static int getColor(float fraction, int startValue, int endValue) {
        int startA = (startValue >> ALPHA_OFFSET) & COLOR_OFFSET;
        int startR = (startValue >> COLOR_OFFSET_R) & COLOR_OFFSET;
        int startG = (startValue >> COLOR_OFFSET_G) & COLOR_OFFSET;
        int startB = startValue & COLOR_OFFSET;

        int endA = (endValue >> ALPHA_OFFSET) & COLOR_OFFSET;
        int endR = (endValue >> COLOR_OFFSET_R) & COLOR_OFFSET;
        int endG = (endValue >> COLOR_OFFSET_G) & COLOR_OFFSET;
        int endB = endValue & COLOR_OFFSET;

        return (startA + (int) (fraction * (endA - startA))) << ALPHA_OFFSET |
                ((startR + (int) (fraction * (endR - startR))) << COLOR_OFFSET_R) |
                ((startG + (int) (fraction * (endG - startG))) << COLOR_OFFSET_G) |
                ((startB + (int) (fraction * (endB - startB))));
    }
}
