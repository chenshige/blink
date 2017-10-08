package com.blink.browser.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;

/**
 * This class is a set of methods which used to manipulate volume and brightness of device
 */
public class DeviceControlUtils {

    private static final int MIN_VOLUME = 0;
    private static final float MIN_BRIGHTNESS = 0.0f;
    private static final float MAX_BRIGHTNESS = 1.0f;
    private static final int MAX = 100;
    private static final int SIGNAL_LEVELS = 5;
    private static final String TAG = "DeviceControlUtils";

    public static int getCurrentVolume(AudioManager audioManager) {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static void setPlayerVolume(AudioManager audioManager, int volume) {
        int maxVolume = getMaxVolume(audioManager);
        if (volume < MIN_VOLUME) {
            volume = MIN_VOLUME;
        } else if (volume > maxVolume) {
            volume = maxVolume;
        }
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, MIN_VOLUME);
            // Let user to choose safe volume or not
            if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.JELLY_BEAN_MR2 && audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < volume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            }
        } catch (SecurityException e) {
            // Some devices will throw this exception, if user choose safe volume
        }
   }

    public static int getMaxVolume(AudioManager audioManager) {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    // Get the brightness of current window
    public static float getCurrentBrightness(Activity activity) {
        checkNull(activity, "getCurrentBrigtness get a null activity");

        float fBrightness = activity.getWindow().getAttributes().screenBrightness;
        if (fBrightness > MAX_BRIGHTNESS) {
            fBrightness = MAX_BRIGHTNESS;
        } else if (fBrightness < MIN_BRIGHTNESS) {
            fBrightness = MIN_BRIGHTNESS;
        }

        return fBrightness;
    }

    // The max brightness is 255.0F, the min brightness is 0.0F
    public static void setBrightness(Activity activity, float fBrightness) {
        checkNull(activity, "setBrightness get a null activity");

        if (fBrightness > MAX_BRIGHTNESS) {
            fBrightness = MAX_BRIGHTNESS;
        } else if (fBrightness < MIN_BRIGHTNESS) {
            fBrightness = MIN_BRIGHTNESS;
        }

        if (isAutoBrightness(activity)) {
            stopAutoBrightness(activity);
        }

        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = fBrightness;
        activity.getWindow().setAttributes(lp);
    }

    public static boolean isAutoBrightness(Activity activity) {
        checkNull(activity, "isAutoBrightness get a null activity");

        ContentResolver resolver = activity.getContentResolver();
        try {
            return Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void stopAutoBrightness(Activity activity) {
        checkNull(activity, "stopAutoBrightness get a null activity");

        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public static void startAutoBrightness(Activity activity) {
        checkNull(activity, "startAutoBrightness get a null activity");

        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public static int getBatteryVoltagePercent(Activity activity) {
        checkNull(activity, "getBatteryInfo get a null activity");

        Intent batteryInfo = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryInfo.getIntExtra(BatteryManager.EXTRA_LEVEL, MAX);
    }

    private static void checkNull(Context context, String log) {
        if (context == null)
            throw new NullPointerException(log);
    }
}
