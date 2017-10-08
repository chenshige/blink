package com.blink.browser.util;

import com.blink.browser.bean.WebTaskInfo;

import java.util.List;

public class WebTaskDbUtil {

    public static void add(WebTaskInfo info) {
        if (info != null) {
        }
    }


    public static void delete(int id) {
    }

    public static void update(WebTaskInfo info) {
        WebTaskInfo newInfo = info.clone();
    }

    public static void updatePath (String path) {
        List<WebTaskInfo> lists = findAll();
        for (int i=0;i< lists.size();i++) {
            lists.get(i).setLocalFile(path);
            update(lists.get(i));
        }
    }

    public static List<WebTaskInfo> findAll() {
        return null;
    }
}
