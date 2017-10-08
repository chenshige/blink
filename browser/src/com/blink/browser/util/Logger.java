package com.blink.browser.util;


import android.util.Log;

import com.blink.browser.BuildConfig;
import com.blink.browser.util.ChannelUtil;

import java.util.Locale;

import static android.util.Log.*;

public class Logger {

    static {
        LOG_ENABLE = BuildConfig.DEBUG;
    }

    public static String TAG_UPGRADE = "";

    private static boolean LOG_ENABLE;

    public static void enableLog(boolean enable) {
        LOG_ENABLE = enable;
    }

    private static void print(int priority, String tag, String msg, Throwable t) {
        if (!LOG_ENABLE) return;
        switch (priority) {
            case VERBOSE:
                Log.v(tag, msg, t);
                break;
            case DEBUG:
                Log.d(tag, msg, t);
                break;
            case INFO:
                Log.i(tag, msg, t);
                break;
            case WARN:
                Log.w(tag, msg, t);
                break;
            case ERROR:
                Log.e(tag, msg, t);
                break;
        }
    }

    public static void verbose(String tag, String msg, Throwable tr) {
        print(VERBOSE, tag, msg, tr);
    }

    public static void verbose(String tag, String msg) {
        verbose(tag, msg, null);
    }

    public static void debug(String tag, String msg, Throwable tr) {
        print(DEBUG, tag, msg, tr);
    }

    public static void debug(String tag, String msg) {
        debug(tag, msg, null);
    }

    public static void info(String tag, String msg, Throwable tr) {
        print(INFO, tag, msg, tr);
    }

    public static void info(String tag, String msg) {
        info(tag, msg, null);
    }

    public static void warn(String tag, String msg, Throwable tr) {
        print(WARN, tag, msg, tr);
    }

    public static void warn(String tag, String msg) {
        warn(tag, msg, null);
    }

    public static void warn(String tag, Throwable tr) {
        warn(tag, null, tr);
    }

    public static void error(String tag, String msg, Throwable tr) {
        print(ERROR, tag, msg, tr);
    }

    public static void error(String tag, String msg) {
        error(tag, msg, null);
    }

    public static void wtf(String tag, String msg, Throwable tr) {
        print(ASSERT, tag, msg, tr);
    }

    public static void wtf(String tag, String msg) {
        wtf(tag, msg, null);
    }

    public static void wtf(String tag, Throwable tr) {
        wtf(tag, null, tr);
    }

    public static void e(Object msg) {
        error(getTag(), String.valueOf(msg));
    }

    public static void d(Object msg) {
        debug(getTag(), String.valueOf(msg));
    }

    public static void i(Object msg) {
        info(getTag(), String.valueOf(msg));
    }

    public static void w(Object msg) {
        warn(getTag(), String.valueOf(msg));
    }

    public static void v(Object msg) {
        verbose(getTag(), String.valueOf(msg));
    }


    private static String getTag() {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
        return String.format(Locale.getDefault(), "%s.%s(%d)", getSimpleClassName(ste.getClassName()), ste.getMethodName(), ste.getLineNumber());
    }

    private static String getSimpleClassName(String path) {
        int index = path.lastIndexOf('.');
        if (index < 0) {
            return path;
        }
        return path.substring(index + 1);
    }
}
