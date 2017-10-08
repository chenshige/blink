
package com.blink.browser.reflection;

import android.view.inputmethod.InputMethodManager;
import android.view.View;

import java.lang.reflect.Method;

public class InputMethodManagerExtension {

    private static class Prototype {

        private static Class<?> mClass;
        private static Method mFocusInMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("android.view.inputmethod.InputMethodManager");
                mFocusInMethod = mClass.getDeclaredMethod("focusIn", View.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public void focusIn(Object obj, View view) {
            try {
                if (mFocusInMethod == null)
                    throw new NoSuchMethodException("focusIn");

                mFocusInMethod.invoke(obj, view);
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

    public static void focusIn(InputMethodManager obj, View view) {
        getPrototype().focusIn(obj, view);
    }
}
