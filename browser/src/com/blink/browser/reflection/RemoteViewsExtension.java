
package com.blink.browser.reflection;

import android.graphics.PorterDuff;
import android.widget.RemoteViews;

import java.lang.reflect.Method;

public class RemoteViewsExtension {

    private static class Prototype {

        private static Class<?> mClass;
        private static Method mSetDrawableParametersMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("android.widget.RemoteViews");
                mSetDrawableParametersMethod = mClass.getDeclaredMethod("setDrawableParameters",
                        int.class, boolean.class, int.class, int.class, PorterDuff.Mode.class, int.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public void setDrawableParameters(Object obj, int viewId, boolean targetBackground, int alpha,
                int colorFilter, PorterDuff.Mode mode, int level) {
            try {
                if (mSetDrawableParametersMethod == null)
                    throw new NoSuchMethodException("setDrawableParameters");

                mSetDrawableParametersMethod.invoke(obj, viewId, targetBackground, alpha, colorFilter, mode, level);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }
    }

    private static Prototype sPrototype;

    private static Prototype getPrototype() {
        if (sPrototype == null) {
            sPrototype = new Prototype();
        }

        return sPrototype;
    }

    public static void setDrawableParameters(RemoteViews remoteViews, int viewId, boolean targetBackground, int alpha,
            int colorFilter, PorterDuff.Mode mode, int level) {
        getPrototype().setDrawableParameters(remoteViews, viewId, targetBackground, alpha, colorFilter, mode, level);
    }

}
