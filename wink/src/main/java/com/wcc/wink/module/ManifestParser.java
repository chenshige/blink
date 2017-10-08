package com.wcc.wink.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wenbiao.xie on 2016/6/21.
 */
public final class ManifestParser {
    private static final String WINK_MODULE_VALUE = "WinkModule";

    private final Context context;

    public ManifestParser(Context context) {
        this.context = context;
    }

    public List<WinkModule> parse() {
        List<WinkModule> modules = new ArrayList<WinkModule>();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                for (String key : appInfo.metaData.keySet()) {
                    if (WINK_MODULE_VALUE.equals(appInfo.metaData.get(key))) {
                        modules.add(parseModule(key));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Unable to find metadata to parse WinkModules", e);
        }

        return modules;
    }

    private static WinkModule parseModule(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find WinkModule implementation", e);
        }

        Object module;
        try {
            module = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate WinkModule implementation for " + clazz, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate WinkModule implementation for " + clazz, e);
        }

        if (!(module instanceof WinkModule)) {
            throw new RuntimeException("Expected instanceof WinkModule, but found: " + module);
        }
        return (WinkModule) module;
    }
}
