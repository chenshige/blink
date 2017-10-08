
package com.blink.browser.reflection;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.database.Cursor;

import java.lang.reflect.Method;

public class SearchManagerExtension {

    private static class Prototype {

        private static Class<?> mClass;
        private static Method mGetSuggestionsMethod;
        private static Method mGetWebSearchActivityMethod;

        public Prototype() {
            try {
                mClass = getClass().getClassLoader().loadClass("android.app.SearchManager");
                mGetSuggestionsMethod = mClass.getDeclaredMethod("getSuggestions", SearchableInfo.class, String.class);
                mGetWebSearchActivityMethod = mClass.getDeclaredMethod("getWebSearchActivity");
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflect", e);
            }
        }

        public Cursor getSuggestions(Object obj, SearchableInfo searchable, String query) {
            try {
                if (mGetSuggestionsMethod == null)
                    throw new NoSuchMethodException("getSuggestions");

                return (Cursor)mGetSuggestionsMethod.invoke(obj, searchable, query);
            } catch (Exception e) {
                throw new RuntimeException("Invlaid reflect", e);
            }
        }

        public ComponentName getWebSearchActivity(Object obj) {
            try {
                if (mGetWebSearchActivityMethod == null)
                    throw new NoSuchMethodException("getWebSearchActivity");

                return (ComponentName)mGetWebSearchActivityMethod.invoke(obj);
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

    public static Cursor getSuggestions(SearchManager sm, SearchableInfo searchable, String query) {
        return getPrototype().getSuggestions(sm, searchable, query);
    }

    public static ComponentName getWebSearchActivity(SearchManager sm) {
        return getPrototype().getWebSearchActivity(sm);
    }
}
