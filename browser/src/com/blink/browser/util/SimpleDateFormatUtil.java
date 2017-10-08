package com.blink.browser.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleDateFormatUtil {
    private static ThreadLocal<SimpleDateFormat> DateLocal = new ThreadLocal<>();

    public final static SimpleDateFormat DATE_FORMAT_YYYY_MM_dd_HH_mm = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm", Locale.getDefault());

    public static SimpleDateFormat getDateFormat(SimpleDateFormat simpleDateFormat) {
        if (null == DateLocal.get()) {
            DateLocal.set(simpleDateFormat);
        }
        return DateLocal.get();
    }

    public static String getDateToSimpleDateFormat_YYYY_MM_dd_HH_mm(
            String milliseconds) {
        Date date = new Date(Long.valueOf(milliseconds));

        return getDateFormat(DATE_FORMAT_YYYY_MM_dd_HH_mm).format(date);
    }


}
