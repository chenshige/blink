/*
 * Copyright (c) 1998-2012 TENCENT Inc. All Rights Reserved.
 *
 * FileName: QTLog.java
 *
 * Description: 日志输出工具类文件
 *
 * History:
 * 1.0 devilxie 2012-09-05 Create
 */
package com.wcc.wink.util;

import android.util.Log;


/**
 * 日志输出类，可控制调试与文件日志的控制
 *
 * @author devilxie
 * @version 1.0
 */
public final class WLog {

    private final static Logger DEFAULT = new Logger() {

        @Override
        public boolean isDebug() {
            return false;
        }

        @Override
        public void v(String tag, String format, Object... args) {

        }

        @Override
        public void i(String tag, String format, Object... args) {

        }

        @Override
        public void d(String tag, String format, Object... args) {

        }

        @Override
        public void w(String tag, String format, Object... args) {

        }

        @Override
        public void e(String tag, String format, Object... args) {

        }
    };

    static Logger logger = DEFAULT;

    public static void setLogger(Logger l) {
        if (l == null) {
            logger = DEFAULT;
        } else {
            logger = l;
        }
    }

    public static boolean isDebug() {
        return logger.isDebug();
    }

    public static void v(String tag, String format, Object... args) {
        logger.v(tag, format, args);
    }

    public static void vIf(boolean condition, String tag, String format, Object... args) {
        if (condition) {
            logger.v(tag, format, args);
        }
    }

    public static void i(String tag, String format, Object... args) {
        logger.i(tag, format, args);
    }

    public static void iIf(boolean condition, String tag, String format, Object... args) {
        if (condition) {
            logger.i(tag, format, args);
        }
    }

    public static void d(String tag, String format, Object... args) {
        logger.d(tag, format, args);
    }

    public static void dIf(boolean condition, String tag, String format, Object... args) {
        if (condition) {
            logger.d(tag, format, args);
        }
    }

    public static void w(String tag, String format, Object... args) {
        logger.w(tag, format, args);
    }

    public static void wIf(boolean condition, String tag, String format, Object... args) {
        if (condition) {
            logger.w(tag, format, args);
        }
    }

    public static void e(String tag, String format, Object... args) {
        logger.e(tag, format, args);
    }

    public static void e(String tag, Throwable e) {
        if (logger.isDebug()) {
            String s = Log.getStackTraceString(e);
            logger.e(tag, s);
        }
    }

    public static void eIf(boolean condition, String tag, String format, Object... args) {
        if (condition) {
            logger.e(tag, format, args);
        }
    }

    public static void printStackTrace(Throwable e) {
        if (logger.isDebug()) {
            String s = Log.getStackTraceString(e);
            logger.e("WinkException", s);
        }
    }

    public interface Logger {
        boolean isDebug();

        void v(String tag, String format, Object... args);

        void i(String tag, String format, Object... args);

        void d(String tag, String format, Object... args);

        void w(String tag, String format, Object... args);

        void e(String tag, String format, Object... args);
    }
}
