package com.blink.browser.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.blink.browser.Browser;

import java.util.Map;

public class SharedPreferencesUtils {
    /**
     * 保存在手机里的文件名
     */
    public static final String FILE_NAME = "blink_data";

    /**
     * 广告过滤时间戳
     */
    public static final String ADBLOCK_UPDATE_TIMESTAMP = "adblock_update_timestamp";
    public static final String EASYLIST_UPDATE_TIMESTAMP = "easylist_update_timestamp";

    public static final String SHOW_LEADPAGE = "show_leadpage";

    public static final String OPEN_DEVELOPER_OPTIONS = "open_developer_options";//是否开启开发者选项



    /**
     * 保存数据
     *
     * @param key key
     * @param obj value
     */
    public static void put(String key, Object obj) {
        SharedPreferences sp = Browser.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_APPEND);
        SharedPreferences.Editor editor = sp.edit();

        if (obj instanceof Boolean) {
            editor.putBoolean(key, (Boolean) obj);
        } else if (obj instanceof Float) {
            editor.putFloat(key, (Float) obj);
        } else if (obj instanceof Integer) {
            editor.putInt(key, (Integer) obj);
        } else if (obj instanceof Long) {
            editor.putLong(key, (Long) obj);
        } else {
            editor.putString(key, (String) obj);
        }
        editor.commit();
    }

    /**
     * 获取指定的数据
     */
    public static Object get(String key, Object defaultObj) {
        SharedPreferences sp = Browser.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Object obj = null;
        if (defaultObj instanceof Boolean) {
            obj = sp.getBoolean(key, (Boolean) defaultObj);
        } else if (defaultObj instanceof Float) {
            obj = sp.getFloat(key, (Float) defaultObj);
        } else if (defaultObj instanceof Integer) {
            obj = sp.getInt(key, (Integer) defaultObj);
        } else if (defaultObj instanceof Long) {
            obj = sp.getLong(key, (Long) defaultObj);
        } else {
            obj = sp.getString(key, (String) defaultObj);
        }
        return obj == null ? defaultObj : obj;
    }

    /**
     * 删除指定的数据
     */
    public static void remove(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.commit();
    }

    /**
     * 返回所有的键值对
     */
    public static Map<String, ?> getAll() {
        SharedPreferences sp = Browser.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Map<String, ?> map = sp.getAll();
        return map;
    }

    /**
     * 清除所有数据
     */
    public static void clear() {
        SharedPreferences sp = Browser.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * 检查是否存在此key对应的数据
     */
    public static boolean contains(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sp.contains(key);
    }

}
