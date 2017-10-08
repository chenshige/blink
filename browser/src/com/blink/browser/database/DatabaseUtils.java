package com.blink.browser.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseUtils {

    /**
     * get database column
     *
     * @param type
     * @return
     */
    public static String getColumnType(String type) {
        String value = null;
        if (type.contains("String")) {
            value = " TEXT ";
        } else if (type.contains("int")) {
            value = " INTEGER ";
        } else if (type.contains("boolean")) {
            value = " boolean ";
        } else if (type.contains("float")) {
            value = " float ";
        } else if (type.contains("double")) {
            value = " double ";
        } else if (type.contains("char")) {
            value = " varchar ";
        } else if (type.contains("long")) {
            value = " long ";
        } else if (type.contains("byte[]")) {
            value = " BLOB ";
        } else {
            value = "";
        }
        return value;
    }

    /**
     * according clazz get table name
     *
     * @param clazz
     * @return
     */
    public static String getTableName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase(Locale.ENGLISH);
    }

    /**
     * get entity from database
     *
     * @param cursor
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> getEntity(Cursor cursor, Class<T> clazz, ClazzCache clazzCache) {
        List<T> list = new ArrayList<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Field[] fields = clazzCache.getKeyValue(clazz);
                    T modeClass = clazz.newInstance();
                    for (Field field : fields) {
                        Class<?> cursorClass = cursor.getClass();
                        String columnMethodName = getColumnMethodName(field.getType());
                        Method cursorMethod = cursorClass.getMethod(columnMethodName, int.class);
                        String fieldName = field.getName().toLowerCase(Locale.ENGLISH);
                        if (fieldName.equals("_id") || fieldName.equals("id")) {
                            fieldName = "_id";
                        }
                        int findIndex = cursor.getColumnIndex(fieldName);
                        if (findIndex == -1) {
                            continue;
                        }
                        Object value = cursorMethod.invoke(cursor, findIndex);
                        if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                            if ("0".equals(String.valueOf(value))) {
                                value = false;
                            } else if ("1".equals(String.valueOf(value))) {
                                value = true;
                            }
                        } else if (field.getType() == char.class || field.getType() == Character.class) {
                            value = ((String) value).charAt(0);
                        } else if (field.getType() == Date.class) {
                            long date = (Long) value;
                            if (date <= 0) {
                                value = null;
                            } else {
                                value = new Date(date);
                            }
                        }
                        String methodName = makeSetterMethodName(field);
                        Method method = ReflectUtils.getAllDeclaredMethod(clazz.newInstance(), methodName, field.getType());
                        method.invoke(modeClass, value);

                    }
                    list.add(modeClass);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * get column Method name
     *
     * @param fieldType
     * @return
     */
    public static String getColumnMethodName(Class<?> fieldType) {
        String typeName;
        if (fieldType.isPrimitive()) {
            typeName = DatabaseUtils.capitalize(fieldType.getName());
        } else {
            typeName = fieldType.getSimpleName();
        }
        String methodName = "get" + typeName;
        if ("getBoolean".equals(methodName)) {
            methodName = "getInt";
        } else if ("getChar".equals(methodName) || "getCharacter".equals(methodName)) {
            methodName = "getString";
        } else if ("getDate".equals(methodName)) {
            methodName = "getLong";
        } else if ("getInteger".equals(methodName)) {
            methodName = "getInt";
        } else if ("getbyte[]".equals(methodName)) {
            methodName = "getBlob";
        }
        return methodName;
    }

    /**
     * get set method's string
     *
     * @param field
     * @return
     */
    public static String makeSetterMethodName(Field field) {
        String setterMethodName;
        String setterMethodPrefix = "set";
        if (isPrimitiveBooleanType(field) && field.getName().matches("^is[A-Z]{1}.*$")) {
            setterMethodName = setterMethodPrefix + field.getName().substring(2);
        } else if (field.getName().matches("^[a-z]{1}[A-Z]{1}.*")) {
            setterMethodName = setterMethodPrefix + field.getName();
        } else {
            setterMethodName = setterMethodPrefix + DatabaseUtils.capitalize(field.getName());
        }
        return setterMethodName;
    }

    public static boolean isPrimitiveBooleanType(Field field) {
        Class<?> fieldType = field.getType();
        return "boolean".equals(fieldType.getName()) ? true : false;
    }

    /**
     * get table name
     *
     * @param clazz the specified class
     * @return sql statements
     */
    public static String getCreateTableSql(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        String tabName = DatabaseUtils.getTableName(clazz);
        sb.append("CREATE TABLE ").append(tabName).append(" (id INTEGER PRIMARY KEY,");
        Field[] fields = clazz.getDeclaredFields();
        for (Field fd : fields) {
            String fieldName = fd.getName();
            String fieldType = fd.getType().getName();
            if (fieldName.equalsIgnoreCase("_id") || fieldName.equalsIgnoreCase("id")) {
                continue;
            } else if (!fieldName.contains("change")) {
                sb.append(fieldName).append(DatabaseUtils.getColumnType(fieldType)).append(",");
            } else {
                sb.append("");
            }
        }
        int len = sb.length();
        sb.replace(len - 2, len, ")");
        return sb.toString();
    }

    /**
     * get object type
     *
     * @param primitiveType
     * @return
     */
    public static Class<?> getObjectType(Class<?> primitiveType) {
        if (primitiveType != null && primitiveType.isPrimitive()) {
            String basicTypeName = primitiveType.getName();
            if ("int".equals(basicTypeName)) {
                return Integer.class;
            } else if ("short".equals(basicTypeName)) {
                return Short.class;
            } else if ("long".equals(basicTypeName)) {
                return Long.class;
            } else if ("float".equals(basicTypeName)) {
                return Float.class;
            } else if ("double".equals(basicTypeName)) {
                return Double.class;
            } else if ("boolean".equals(basicTypeName)) {
                return Boolean.class;
            } else if ("char".equals(basicTypeName)) {
                return Character.class;
            }
        }
        return null;
    }


    /**
     * juge is or not char
     */
    public static boolean isCharType(Field field) {
        String type = field.getType().getName();
        return type.equals("char") || type.endsWith("Character");
    }


    /**
     * Get reflection method of parameter types
     */
    public static Class<?>[] getParameterTypes(Field field, Object fieldValue, Object[] parameters) {
        Class<?>[] parameterTypes;
        if (DatabaseUtils.isCharType(field)) {
            parameters[1] = String.valueOf(fieldValue);
            parameterTypes = new Class[]{String.class, String.class};
        } else if (field.getType().isPrimitive()) {
            parameterTypes = new Class[]{String.class, DatabaseUtils.getObjectType(field.getType())};
        } else if ("java.util.Date".equals(field.getType().getName())) {
            parameterTypes = new Class[]{String.class, Long.class};
        } else {
            parameterTypes = new Class[]{String.class, field.getType()};
        }
        return parameterTypes;
    }

    /**
     * put value to ContentValues for Database
     */
    public static void putValues(ContentValues values, Field fd, Object obj) {
        Class<?> clazz = values.getClass();
        try {
            Object[] parameters = new Object[]{fd.getName(), fd.get(obj)};
            Class<?>[] parameterTypes = DatabaseUtils.getParameterTypes(fd, fd.get(obj), parameters);
            Method method = clazz.getDeclaredMethod("put", parameterTypes);
            method.setAccessible(true);
            method.invoke(values, parameters);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * String change to upper case
     *
     * @param string
     * @return
     */
    public static String capitalize(String string) {
        if (!TextUtils.isEmpty(string)) {
            return string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1);
        }
        return string;
    }

    /**
     * Filter Annotation filter
     *
     * @param fields
     * @return
     */
    public static Field[] filterFiled(Field[] fields) {
        List<Field> list = new ArrayList<>();

        for (Field f : fields) {
            IgnoreField annotation = f.getAnnotation(IgnoreField.class);
            String modifier = Modifier.toString(f.getModifiers());
            if (null == annotation && !modifier.contains("static") && !modifier.contains("final")) {
                list.add(f);
            }
        }
        Field[] field = new Field[list.size()];
        for (int i = 0; i < list.size(); i++) {
            field[i] = list.get(i);
        }
        return field;
    }
}
