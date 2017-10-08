// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.analytics;

import android.content.Context;
import android.os.Bundle;

import com.blink.browser.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FbAnalytics {
    private static class Prototype {
        private static Class<?> mClass;
        private static Method mGetInstance;
        private static Method mLogEventMethod;

        public Prototype() {
            try {
                mClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics");
                mGetInstance = mClass.getDeclaredMethod("getInstance", Context.class);
                mLogEventMethod = mClass.getDeclaredMethod("logEvent", String.class, Bundle.class);
            } catch (Exception e) {
            }
        }

        public Object getInstance(Context context) {
            try {
                return mGetInstance.invoke(mClass, context);
            } catch (Exception e) {
            }

            return null;
        }

        public Boolean getExist() {
            return mClass != null && mGetInstance != null && mLogEventMethod != null;
        }

        public void logEvent(Object obj, String eventType, Bundle bundle) {
            try {
                if (mGetInstance == null || mLogEventMethod == null) {
                    return;
                }
                mLogEventMethod.invoke(obj, eventType, bundle);
                Logger.e(eventType + ":" + bundle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Prototype sPrototype = null;
    private static Object mObject;

    private Prototype getPrototype() {
        if (sPrototype == null) {
            sPrototype = new Prototype();
        }
        return sPrototype;
    }

    public FbAnalytics(Context context) {
        mObject = getPrototype().getInstance(context);
    }

    public Boolean isExist() {
        return getPrototype().getExist();
    }

    public void logEvent(String eventType, Bundle bundle) {
        if (getPrototype() == null || mObject == null || !isExist()) return;
        getPrototype().logEvent(mObject, eventType, bundle);
    }
}
