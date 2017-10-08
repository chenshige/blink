
package com.blink.browser.reflection;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

import java.lang.CharSequence;
import java.lang.reflect.Method;

public class NotificationExtension {

    private static class Prototype {

        private static Class<?> mClass;
        private static Method mSetLatestEventInfoMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("android.app.Notification");
                mSetLatestEventInfoMethod = mClass.getDeclaredMethod("setLatestEventInfo",
                        Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public void setLatestEventInfo(Object obj, Context context, CharSequence contentTitle,
                CharSequence contentText, PendingIntent contentIntent) {
            try {
                if (mSetLatestEventInfoMethod == null)
                    throw new NoSuchMethodException("setLatestEventInfo");

                mSetLatestEventInfoMethod.invoke(obj, context, contentTitle, contentText, contentIntent);
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

    public static void setLatestEventInfo(Notification notification, Context context, CharSequence contentTitle,
            CharSequence contentText, PendingIntent contentIntent) {
        getPrototype().setLatestEventInfo(notification, context, contentTitle, contentText, contentIntent);
    }
}
