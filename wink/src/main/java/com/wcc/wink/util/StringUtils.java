package com.wcc.wink.util;


import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class StringUtils {

    public static boolean isNull(String str) {
        return str == null;
    }

    // 字符串为空
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    // 字符串（包括中文）长度
    public static int strLen(String s) {
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255)
                length++;
            else
                length += 2;

        }
        return length;
    }

    public static String substring(String s, int subLen) {
        return substring(s, subLen, "..");
    }

    public static String substring(String s, int subLen, String postfix) {
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255) {
                length++;
            } else {
                length += 2;
            }
            if (length > subLen || (length == subLen && i + 1 != s.length())) {
                return s.substring(0, i).concat(postfix);
            }
        }
        return s;
    }

    public static byte[] getBytesUtf8(String string) {
        return StringUtils.getBytesUnchecked(string, "utf-8");
    }

    public static byte[] getBytesUnchecked(String string, String charsetName) {
        if (string == null) {
            return null;
        }
        try {
            return string.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw StringUtils.newIllegalStateException(charsetName, e);
        }
    }

    private static IllegalStateException newIllegalStateException(String charsetName, UnsupportedEncodingException e) {
        return new IllegalStateException(charsetName + ": " + e);
    }

    public static Date stringToDate(String str) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {

        }
        return date;
    }

    public static String timeToDate(String str) {
        try {
            Date d = new Date(Long.parseLong(str));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(d);
        } catch (Exception e) {

        }
        return "";
    }

    public static String timeToDate(Long time) {
        Date d = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(d);
    }

    /**
     * 阿拉伯语中String.format的时候，会将数字转化成阿拉伯语
     * 为什么呢？Android阿拉伯文机子中时间日期本地语言中没有0-9，设置语言无效
     * <p/>
     * yyyy-MM-DD hh:mm:ss
     *
     * @param time
     * @return
     */
    public static String getTime2GMT(long time) {
        StringBuffer sb = new StringBuffer();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        if (time > 0) {
            calendar.setTimeInMillis(time);
        }


        sb.append(calendar.get(Calendar.YEAR));
        sb.append("-");
        if (calendar.get(Calendar.MONTH) + 1 < 10) {
            sb.append(0); // 补零
        }
        sb.append(calendar.get(Calendar.MONTH) + 1);
        sb.append("-");
        if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
            sb.append(0); // 补零
        }
        sb.append(calendar.get(Calendar.DAY_OF_MONTH));
        sb.append(" ");
        if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
            sb.append(0); // 补零
        }
        sb.append(calendar.get(Calendar.HOUR_OF_DAY));
        sb.append(":");
        if (calendar.get(Calendar.MINUTE) < 10) {
            sb.append(0); // 补零
        }
        sb.append(calendar.get(Calendar.MINUTE));
        sb.append(":");
        if (calendar.get(Calendar.SECOND) < 10) {
            sb.append(0); // 补零
        }
        sb.append(calendar.get(Calendar.SECOND));

        return sb.toString();
    }


    public static String getTime2GMT() {
        return getTime2GMT(0);
    }

    /**
     * 匹配邮箱
     *
     * @param mailAddress
     * @return
     */
    public static boolean mailAddressVerify(String mailAddress) {
        String emailExp = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(emailExp);
        return p.matcher(mailAddress).matches();
    }

    /**
     * 格式化数字
     * 比如 1000 格式成1,000；
     *
     * @param number
     */
    public static String parseNumber(int number) {
        String pattern = "#,###";
        DecimalFormat formatter = new DecimalFormat();
        formatter.applyPattern(pattern);
        return formatter.format(number);
    }

    /**
     * 格式化数字
     * 比如 1000 格式成1,000；
     *
     * @param number
     */
    public static String parseNumber(int number, Locale l) {
        String pattern = "#,###";
        DecimalFormatSymbols value = new DecimalFormatSymbols(l);
        DecimalFormat formatter = new DecimalFormat(pattern, value);
        return formatter.format(number);
    }

    public static String repeat(String str, int repeatTimes) {
        int inputLen = str.length();    //获取字符串的长度
        int outputLen = inputLen * repeatTimes;    //输出字符串的长度
        switch (inputLen) {
            //当长度只有1或2时，选择直接使用字符操作，增加效率
            case 1:
                return repeat(str.charAt(0), repeatTimes);
            case 2:
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char[] output = new char[outputLen];
                //for(int i = repeatTimes * 2 -1;i >=0;){
                for (int i = 0; i < repeatTimes * 2; ) {
                    output[i] = ch0;
                    output[i + 1] = ch1;
                    i += 2;
                }
                //return output.toString();
                return Arrays.toString(output);
            default:
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i <= repeatTimes - 1; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    public static String repeat(char ch, int repeatTimes) {
        char[] buf = new char[repeatTimes];
        for (int i = repeatTimes - 1; i >= 0; i--) {
            //复制repeatTimes
            buf[i] = ch;
        }

        return new String(buf);
    }

    /**
     * 将作为文件名的字符串的特殊字符"\*?:$/'",`^<>+"替换成"_"，以便文件顺利创建成功
     *
     * @param path 原待创建的文件名
     * @return 返回处理后的文件名
     */
    public static String filterForFile(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }
        String need = path.replaceAll(
                "\\\\|\\*|\\?|\\:|\\$|\\/|'|\"|,|`|\\^|<|>|\\+", "_");
        return need;
    }
}
