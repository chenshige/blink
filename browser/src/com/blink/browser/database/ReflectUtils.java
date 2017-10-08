package com.blink.browser.database;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ReflectUtils {

    /**
     * get clazz all filed include super filed
     *
     * @param clazz
     * @return Field Array
     */
    public static Field[] getAllField(Class<?> clazz) {
        ArrayList<Field> fieldList = new ArrayList<Field>();
        Field[] dFields = clazz.getDeclaredFields();
        if (null != dFields && dFields.length > 0) {
            fieldList.addAll(Arrays.asList(dFields));
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != Object.class) {
            Field[] superFields = getAllField(superClass);
            if (null != superFields && superFields.length > 0) {
                for (Field field : superFields) {
                    if (!isContain(fieldList, field)) {
                        fieldList.add(field);
                    }
                }
            }
        }
        Field[] result = new Field[fieldList.size()];
        fieldList.toArray(result);
        return result;
    }

    /**
     * get super all method
     *
     * @param object
     * @param methodName
     * @param parameterTypes
     * @return
     */
    public static Method getAllDeclaredMethod(Object object, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        for (Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                return clazz.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                // this exception no need deal with it, will return null.
            }
        }
        return null;
    }

    private static boolean isContain(ArrayList<Field> fieldList, Field field) {
        for (Field temp : fieldList) {
            if (temp.getName().equals(field.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * get super method type
     *
     * @param method
     * @return
     *
     * method is setImageIcon,return byte[].class;
     * method is setId,return int.class;
     * others is String.class
     */
    public static Class<?> getSuperParamType(String method) {
        if (method.toLowerCase(Locale.ENGLISH).equals("setimageicon")) {
            return byte[].class;
        } else if (method.toLowerCase(Locale.ENGLISH).equals("setid")) {
            return int.class;
        } else {
            return String.class;
        }
    }
}
