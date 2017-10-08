// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.reflection;

import java.lang.reflect.Method;

public class ActivityManager {

    private static class Prototype {
        private Class<?> mClass;
        private Method mStaticGetMemoryClassMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("android.app.ActivityManager");
                mStaticGetMemoryClassMethod = mClass.getDeclaredMethod("staticGetMemoryClass");
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public int staticGetMemoryClass() {
            try {
                if (mStaticGetMemoryClassMethod == null)
                    throw new NoSuchMethodException("staticGetMemoryClass");

                return (Integer)mStaticGetMemoryClassMethod.invoke(null);
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

    public static int staticGetMemoryClass() {
        return getPrototype().staticGetMemoryClass();
    }
}
