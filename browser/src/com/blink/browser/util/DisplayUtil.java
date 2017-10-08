package com.blink.browser.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.provider.Settings;
import android.support.annotation.DimenRes;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.UrlUtils;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

import java.lang.reflect.Method;

public class DisplayUtil {

    private static int TAB_COUNT_TO_CHANGE_SIZE = 10;
    public static float DEFAULT_BRIGHTNESS = -1.0f;

    /**
     * 将px 的值转换为dip或 dp值，保证尺寸大小不变
     *
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将dip 或dp值转换为px值，保证尺寸大小不变
     *
     * @param context
     * @param dipValue
     * @return
     */
    public static int dip2px(Context context, float dipValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     * @param pxValue
     * @param （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @param （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    private static int sRealHeight;

    public static int getRealSize(Context context) {
        if (context != null) {
            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            try {
                Class<?> disPlayClass = Class.forName("android.view.Display");
                Point realSize = new Point();
                Method method = disPlayClass.getMethod("getRealSize", Point.class);
                method.invoke(display, realSize);
                sRealHeight = realSize.y;
            } catch (Exception e) {
                sRealHeight = getScreenHeight(context);
            }
        }
        return sRealHeight;
    }

    /**
     * 获取底部导航栏高度
     *
     * @param context
     * @return
     */
    public static int getNavBarHeight(Context context) {
        return getRealSize(context) - getScreenHeight(context);
    }

    /**
     * 获取屏幕的宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 获取屏幕的高度
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    public static int getDimenPxValue(Context context, @DimenRes int dimenRes) {
        return context.getResources().getDimensionPixelSize(dimenRes);
    }

    /**
     * 获取DisplayMetrics
     *
     * @param context
     * @return
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    public static void resetTabSwitcherTextSize(TextView tv, int count) {
        int textSize;
        if (count < TAB_COUNT_TO_CHANGE_SIZE) {
            textSize = tv.getContext().getResources().getInteger(R.integer.tab_count_less_than_ten);
        } else {
            textSize = tv.getContext().getResources().getInteger(R.integer.tab_count_greater_than_ten);
        }

        String tabCount = (count <= 0 ? 1 : count) + "";
        if (!TextUtils.isEmpty(tv.getText().toString())) {
            int before = Integer.parseInt(tv.getText().toString());
            if (before != count) {
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS, AnalyticsSettings
                        .ID_AMOUNT, tabCount);
            }
        }

        tv.setTextSize(textSize);
        tv.setText(tabCount);
    }

    public static String getDispalyText(Context context, EditText textView, String input) {
        if (!UrlUtils.isSearch(input)) {
            return input;
        }

        float urlinputOtherWidth = context.getResources().getDimension(R.dimen.activity_horizontal_margin) * 2 + context
                .getResources().getDimension(R.dimen.tab_button_width) * 2 + context
                .getResources().getDimension(R.dimen.tab_button_horizontal_margin);
        float tvWidth = getScreenWidth(context) - urlinputOtherWidth;
        return TextUtils.ellipsize(input, textView.getPaint(), tvWidth, TextUtils.TruncateAt.END).toString();
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    public static boolean isScreenPortrait(Context context) {
        Configuration configuration = context.getResources().getConfiguration(); //获取设置的配置信息
        return configuration.orientation == Configuration.ORIENTATION_PORTRAIT; //获取屏幕方向
    }

    /**
     * 设置屏幕亮度
     *
     * @param activity
     * @param brightness 0 最暗　1 最亮
     */
    public static void setScreenBrightness(Activity activity, float brightness) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = brightness;
        activity.getWindow().setAttributes(lp);
    }

    /**
     * 获取屏幕亮度
     *
     * @param activity
     */
    public static float getScreenBrightness(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        return lp.screenBrightness;
    }

    /**
     * 如果当前的处于夜间模式就会变更屏幕亮度
     *
     * @param activity
     */
    public static void changeScreenBrightnessIfNightMode(Activity activity) {
        float value = BrowserSettings.getInstance().getBrightness();
        if (BrowserSettings.getInstance().getNightMode() && value != DEFAULT_BRIGHTNESS) {
            DisplayUtil.setScreenBrightness(activity, BrowserSettings.getInstance().getBrightness());
        }
    }

    /**
     * 用于没有保存设置亮度时，获取当前系统亮度值0-255，为了适配版本升级转化为float(0-1)
     *
     * @param activity
     * @return
     */
    public static float getSystemBrightness(Activity activity) {
        int systemBrightness = 0;
        try {
            systemBrightness = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness / (float) 255;
    }
}
