package com.blink.browser.database;

import android.support.v4.util.ArrayMap;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

public class ClazzCache<T> {
    private static Map map = new ArrayMap();

    public Field[] getKeyValue(Class<T> clazz) {
        Field[] field = null;
        String mClazzName = clazzToClass(clazz);
        Iterator iterator = map.keySet().iterator();

        while (iterator.hasNext()) {
            if (mClazzName.equals(iterator.next())) {
                field = (Field[]) map.get(mClazzName);
            }
        }
        if (null == field) {
            return setKeyValue(clazz);
        }
        return field;
    }

    private Field[] setKeyValue(Class<T> clazz) {
        Field[] field = DatabaseUtils.filterFiled(ReflectUtils.getAllField(clazz));
        map.put(clazzToClass(clazz), field);
        return field;
    }

    private String clazzToClass(Class<?> clazz) {
        return DatabaseUtils.getTableName(clazz);
    }
}
