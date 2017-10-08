package com.blink.browser.util;

import android.view.View;

/**
 * Created by Wenhao on 2016/11/22.
 */

public class ClickUtil {
    private final static int TIMES = 2 * 1000;//Click the back button twice the time interva

    private static long mPreClick = 0;

    private static int mViewId = 0;

    public static boolean clickShort(int viewId) {
        long click = System.currentTimeMillis();

        if (mViewId == viewId) {
            if (click - mPreClick > TIMES) {
                mPreClick = click;
                return false;
            } else {
                return true;
            }
        } else {
            mViewId = viewId;
            return false;
        }
    }
}
