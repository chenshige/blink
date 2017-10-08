
package com.blink.browser.reflection;

import android.content.Context;

import java.lang.reflect.Method;
import java.io.File;

public class ContextExtension {

    private static class Prototype {

        private static Class<?> mClass;
        private static Method mGetSharedPrefsFileMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("android.content.Context");
                mGetSharedPrefsFileMethod = mClass.getDeclaredMethod("getSharedPrefsFile", String.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public File getSharedPrefsFile(Object obj, String name) {
            try {
                if (mGetSharedPrefsFileMethod == null)
                    throw new NoSuchMethodException("getSharedPrefsFile");

                return (File)mGetSharedPrefsFileMethod.invoke(obj, name);
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

    public static File getSharedPrefsFile(Context context, String name) {
        return getPrototype().getSharedPrefsFile(context, name);
    }

}
