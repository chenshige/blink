package com.blink.browser.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 获取设备相关信息
 */
public class DeviceInfoUtils {

    private static int sNavBarHeight = -1;
    public static String PACKAGE_NAME = "com.blink.browser";

    //客户端信息
    public interface DeviceInfo {
        public static String BRAND = Build.BRAND; //手机品牌
        public static String MODEL = Build.MODEL; //手机型号
        public static String HADRWARENO = Build.HARDWARE;
        public static String OSVERSION = Build.BOARD;
        public static String PRODUCTCODE = Build.DEVICE;
        public static String OFFICEVERSION = Build.FINGERPRINT;
        public static String PRODUCTMODE = Build.PRODUCT;
        public static String SERIAL_NO = Build.SERIAL;
    }

    /**
     * 获取应用版本
     */
    public static String getAppVersionName(Context context) {
        String packageName = context.getPackageName();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取屏幕尺寸
     */
    public static String getScreenSize(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return String.valueOf(dm.widthPixels) + "*" + String.valueOf(dm.heightPixels);
    }

    /**
     * 获取应用versioncode
     */
    public static int getAppVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 获取应用包名
     */
    public static String getAppPackageName(Context context) {
        return context.getPackageName();
    }

    public static String[] getDeviceInfo(Context c) {
        String deviceId = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        String imsi = tm.getSubscriberId();
        String operatorName = tm.getSimOperatorName(); // 运营商
        String officeVersion = Build.FINGERPRINT;
        String hwInfo = String.format("%1$s|%2$s|%3$s", deviceId, imei, imsi);
        String cpuSerial = "";//Cannot get
        String cpu = "";//Cannot get
        WifiManager wifiMng = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfor = wifiMng.getConnectionInfo();
        String mac = wifiInfor.getMacAddress();
        String country = Locale.getDefault().getCountry();
        String language = Locale.getDefault().getLanguage();
        String timezone = TimeZone.getDefault().getDisplayName();
        String usrInfo = String.format("%1$s|%2$s|%3$s|%4$s|%5$s|%6$s|%7$s|%8$s|%9$s|%10$s|%11$s|%12$s|%13$s|%14$s" +
                        "|%15$s|%16$s|%17$s",
                DeviceInfoUtils.DeviceInfo.MODEL, imei, mac, country, language, DeviceInfoUtils.DeviceInfo.BRAND,
                officeVersion, DeviceInfoUtils.DeviceInfo.PRODUCTCODE,
                DeviceInfoUtils.DeviceInfo.OSVERSION, DeviceInfoUtils.DeviceInfo.HADRWARENO,
                DeviceInfoUtils.DeviceInfo.PRODUCTMODE, DeviceInfoUtils.DeviceInfo.SERIAL_NO, cpu, cpuSerial,
                deviceId, timezone, operatorName);
        return new String[]{hwInfo, usrInfo};
    }

    public static boolean isAppOnForeground(Context context) {
        // Returns a list of application processes that are running on the
        // device

        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context
                .ACTIVITY_SERVICE);
        String packageName = context.getApplicationContext().getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }

        return false;
    }

    public static String getDefaultLanguage() {
        String language = Locale.getDefault().toString();
        return TextUtils.isEmpty(language) ? "en_US" : language;
    }

    public static String getCountry() {
        return Locale.getDefault().getCountry();
    }

}
