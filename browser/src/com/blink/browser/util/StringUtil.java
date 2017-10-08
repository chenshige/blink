package com.blink.browser.util;

import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.blink.browser.Browser;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StringUtil {
    /**
     * @param str
     * @return
     */
    public static String recoder(String str) {
        try {
            return Charset.forName("ISO-8859-1").newEncoder().canEncode(str) ? new String(str.getBytes("ISO-8859-1"), "GB2312") : str;
        } catch (UnsupportedEncodingException e) {
            //Method is documented to just ignore invalid support encoding
            //recoder will be unchanged
            return str;
        }
    }

    public static String cutString(String string, int endIndes) {
        if (endIndes < string.length()) {
            string = string.substring(0, endIndes) + "...";
        }
        return string;
    }

    /**
     * 判断邮箱是否合法
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        // Pattern p = Pattern.compile("\\w+@(\\w+.)+[a-z]{2,3}"); //简单匹配
        Pattern p = Pattern
                .compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");// 复杂匹配
        Matcher m = p.matcher(email);
        return m.matches();
    }

    public static String getString(@StringRes int strId, Object... objects) {
        return Browser.getInstance().getString(strId, objects);
    }

    public static boolean checkAsciiText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        final char ASCII = 127;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > ASCII) {
                return false;
            }
        }
        return true;
    }

}
