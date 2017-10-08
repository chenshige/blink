
package com.blink.browser.reflection;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;

import java.lang.reflect.Method;

public class MenuBuilderExtension {

    private static class Prototype {

        private static Class<?> mClass;
        private static Method mSetCurrentMenuInfoMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("com.android.internal.view.menu.MenuBuilder");
                mSetCurrentMenuInfoMethod = mClass.getDeclaredMethod("setCurrentMenuInfo", ContextMenuInfo.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public void setCurrentMenuInfo(ContextMenu menu, ContextMenu.ContextMenuInfo menuInfo) {
            try {
                if (mSetCurrentMenuInfoMethod == null)
                    throw new NoSuchMethodException("setCurrentMenuInfo");

                mSetCurrentMenuInfoMethod.invoke(menu, menuInfo);
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

    public static void setCurrentMenuInfo(ContextMenu menu, ContextMenuInfo menuInfo) {
        getPrototype().setCurrentMenuInfo(menu, menuInfo);
    }
}
