package com.blink.browser.util;

import android.text.TextUtils;

public class SqliteEscape {
    public static String encodeSql(String sql){
        if(TextUtils.isEmpty(sql)) return null;
        StringBuffer buf = new StringBuffer() ;
        int length = sql.length() ;
        for(int i = 0 ;i < length ;i++){
            char c = sql.charAt(i) ;
            switch (c){
                case '/':
                    buf.append("//");
                    break;
                case '\'':
                    buf.append("''");
                    break;
                case '[':
                    buf.append("/[");
                    break;
                case ']':
                    buf.append("/]");
                case '%':
                    buf.append("/%");
                case '&':
                    buf.append("/&");
                case '_':
                    buf.append("/_");
                    break;
                case '(':
                    buf.append("/(");
                    break;
                case ')':
                    buf.append("/)");
                    break;
                default:
                    buf.append(c) ;
            }
        }



        return buf.toString();
    }
}
