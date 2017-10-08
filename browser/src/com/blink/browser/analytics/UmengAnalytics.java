// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.analytics;

import android.content.Context;

import com.blink.browser.Browser;
import com.blink.browser.R;
import com.blink.browser.util.ChannelUtil;
import com.blink.browser.util.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public class UmengAnalytics {

    private static class Prototype {
        private static Class<?> mMobclickAgent;
        private static Class<?> mUMAnalyticsConfigClass;
        private static Method mStartWithConfigure;
        private static Method mLogEventMethod;
        private static Method mResume;
        private static Method mPause;
        private static Constructor mConstructor;

        public Prototype() {
            try {
                mMobclickAgent = Class.forName("com.umeng.analytics.MobclickAgent");
                mUMAnalyticsConfigClass = Class.forName("com.umeng.analytics.MobclickAgent$UMAnalyticsConfig");
                mConstructor = mUMAnalyticsConfigClass.getConstructor(Context.class,
                        String.class, String.class);
                mStartWithConfigure = mMobclickAgent.getDeclaredMethod("startWithConfigure", mUMAnalyticsConfigClass);
                mLogEventMethod = mMobclickAgent.getDeclaredMethod("onEvent", Context.class, String.class, Map.class);
                mResume = mMobclickAgent.getDeclaredMethod("onResume", Context.class);
                mPause = mMobclickAgent.getDeclaredMethod("onPause", Context.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect ", e);
            }
        }

        public Boolean getExist() {
            return mMobclickAgent != null && mUMAnalyticsConfigClass != null && mConstructor != null &&
                    mStartWithConfigure != null && mLogEventMethod != null && mResume != null && mPause != null;
        }

        public void init(Context context) {
            try {
                if (mMobclickAgent == null || mStartWithConfigure == null || mConstructor == null) {
                    return;
                }
                String appkey = Browser.getInstance().getApplicationContext().getString(R.string.umeng_appkey);
                Object config = mConstructor.newInstance(context, appkey, ChannelUtil.getChannelId());
                mStartWithConfigure.invoke(mMobclickAgent, config);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public void logEvent(Context context, String eventType, Map<String, String> map) {
            try {
                if (mLogEventMethod == null || mMobclickAgent == null) {
                    return;
                }

                mLogEventMethod.invoke(mMobclickAgent, context, eventType, map);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public void onResume(Context context) {
            try {
                if (mResume == null) {
                    throw new RuntimeException("Invalid reflect");
                }

                mResume.invoke(mMobclickAgent, context);
            } catch (Exception e) {

            }
        }

        public void onPause(Context context) {
            try {
                if (mPause == null) {
                    throw new RuntimeException("Invalid reflect");
                }

                mPause.invoke(mMobclickAgent, context);
            } catch (Exception e) {

            }
        }
    }


    private static Prototype sPrototype = null;

    private Prototype getPrototype() {
        if (sPrototype == null) {
            sPrototype = new Prototype();
        }
        return sPrototype;
    }

    public UmengAnalytics(Context context) {
        getPrototype();
        if (isExist()) getPrototype().init(context);
    }

    public Boolean isExist() {
        return getPrototype().getExist();
    }

    public void logEvent(Context context, String eventType, Map<String, String> map) {
        if (getPrototype() == null || !isExist()) return;
        getPrototype().logEvent(context, eventType, map);
    }

    public void onResume(Context context) {
        if (getPrototype() == null || !isExist() || ChannelUtil.isOpenPlatform()) return;
        getPrototype().onResume(context);
    }

    public void onPause(Context context) {
        if (getPrototype() == null || !isExist() || ChannelUtil.isOpenPlatform()) return;
        getPrototype().onPause(context);
    }

}
