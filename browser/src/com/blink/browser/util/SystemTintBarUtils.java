package com.blink.browser.util;

import android.app.Activity;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;

import com.blink.browser.R;

public class SystemTintBarUtils {

    public static void setSystemBarColor(Activity activity) {
        setSystemBarColor(activity, R.color.settings_actionbar_background);
    }

    public static void setSystemBarColor(Activity activity, int colorId) {
        if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.LOLLIPOP) {
            return;
        }
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(activity,colorId));
    }

    public static void setSystemBarColorByValue(Activity activity, int color) {
        //由于4.4以下的状态栏设置以及横竖屏切换问题比较多,暂时参考上面的方式不设置状态栏颜色
        if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
            return;
        }
        if(Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            Window window = activity.getWindow();
//            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            WindowManager.LayoutParams params = window.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            params.flags |= bits;
            window.setAttributes(params);
        }

        SystemBarTintManager tintManager = new SystemBarTintManager(activity);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setTintColor(color);
    }

    public static void cancelSystemBarImmersive(Activity activity) {
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        } else if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
}
