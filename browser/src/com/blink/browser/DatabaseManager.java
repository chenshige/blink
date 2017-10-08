package com.blink.browser;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.util.ArrayMap;

import com.blink.browser.bean.CommonEntity;
import com.blink.browser.database.ClazzCache;
import com.blink.browser.database.DataController;
import com.blink.browser.database.DatabaseUtils;
import com.blink.browser.util.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by chendeqiao on 16/5/27.
 */
public class DatabaseManager implements DataController {
    private static final String TAG = "DatabaseManager";
    private SQLiteDatabase mDb = null;
    private ClazzCache mClazzCache;
    private SQLiteOpenHelper mHelper;

    private Map<Class, List<DataChangeListener>> mMap = new ArrayMap<>();
    private static DatabaseManager sInstance;

    private DatabaseManager(SQLiteOpenHelper helper) {
        mClazzCache = new ClazzCache();
        sInstance = this;
        mHelper = helper;
        mDb = mHelper.getWritableDatabase();
    }

    /**
     * data change listener
     */
    public interface DataChangeListener<T extends CommonEntity> {

        void onInsertToDB(T entity);

        void onUpdateToDB(T entity);

        void onDeleteToDB(T entity);

    }

    /**
     * rigister listener
     */
    public synchronized void registerListener(Class entity, DataChangeListener listener) {
        List<DataChangeListener> list = mMap.get(entity);
        if (list != null) {
            list.add(listener);
        } else {
            list = new ArrayList<>();
            list.add(listener);
            mMap.put(entity, list);
        }
    }

    /**
     * remove listener
     */
    public synchronized void removeListener(Class entity, DataChangeListener listener) {
        List<DataChangeListener> list = mMap.get(entity);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                mMap.remove(entity);
            }
        }
    }

    private void onUpdateData(CommonEntity obj) {
        List<DataChangeListener> dl = mMap.get(obj.getClass());
        if (dl != null) {
            for (DataChangeListener dataChangeListener : dl) {
                dataChangeListener.onUpdateToDB(obj);
            }
        }
    }

    private void onInsertData(CommonEntity obj) {
        List<DataChangeListener> dl = mMap.get(obj.getClass());
        if (dl != null) {
            for (DataChangeListener dataChangeListener : dl) {
                dataChangeListener.onInsertToDB(obj);
            }
        }
    }

    private void onDeleteData(CommonEntity obj) {
        List<DataChangeListener> dl = mMap.get(obj.getClass());
        if (dl != null) {
            for (DataChangeListener dataChangeListener : dl) {
                dataChangeListener.onDeleteToDB(obj);
            }
        }
    }

    /**
     * get DatabaseManager object
     */
    public static DatabaseManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("db manager is not init !");
        }
        return sInstance;
    }

    public static void init(SQLiteOpenHelper helper) {
        new DatabaseManager(helper);
    }

    /**
     * close database
     */
    @Override
    public void closeDataBase() {
        mDb.close();
    }

    /**
     * insert data
     *
     * @param obj
     * @return fail:-1 else success
     * @throws IllegalAccessException
     */
    @Override
    public long insert(CommonEntity obj) {
        openDatabase();
        Class<?> modeClass = obj.getClass();
        Field[] fields = mClazzCache.getKeyValue(modeClass);
        ContentValues values = new ContentValues();
        Field.setAccessible(fields, true);
        for (Field fd : fields) {
            String fieldName = fd.getName();
            if (fieldName.equalsIgnoreCase("id") || fieldName.equalsIgnoreCase("_id")) {
                continue;
            }
            DatabaseUtils.putValues(values, fd, obj);
        }
        long state = mDb.insert(DatabaseUtils.getTableName(modeClass), null, values);
        if (state >= 1) {
            obj.setId(state);
            onInsertData(obj);
        }
        return state;
    }

    /**
     * query database all data
     *
     * @param clazz
     * @param <T>   return list of database all data
     * @return list collect
     */
    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        openDatabase();
        Cursor cursor = null;
        try {
            cursor = mDb.query(DatabaseUtils.getTableName(clazz), null, null, null, null, null, null);
            return DatabaseUtils.getEntity(cursor, clazz, mClazzCache);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * according id return data
     *
     * @param select     条件语句 ：（"id>？"）
     * @param selectArgs 条件(new String[]{"0"}) 查询id=0的记录
     * @param <T>        类型
     * @return 返回满足条件的list集合
     */
    @Override
    public <T> List<T> findByArgs(Class<T> clazz, String select, String[] selectArgs) {
        openDatabase();
        Cursor cursor = mDb.query(clazz.getSimpleName(), null, select, selectArgs, null, null, null);
        Logger.debug(TAG, "Query sql : select * from  " + clazz.getSimpleName()
                + " where " + select);
        try {
            return DatabaseUtils.getEntity(cursor, clazz, mClazzCache);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public <T> List<T> findByArgs(Class<T> clazz, String select, String[] selectArgs, String groupBy, String having, String orderBy) {
        openDatabase();
        Cursor cursor = null;
        try {
            cursor = mDb.query(clazz.getSimpleName(), null, select, selectArgs, groupBy, having, orderBy);
            Logger.debug(TAG, "Query sql : select * from  " + clazz.getSimpleName()
                    + " where " + select + " " + orderBy);
            return DatabaseUtils.getEntity(cursor, clazz, mClazzCache);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * through id return data
     */
    @Override
    public <T> T findById(Class<T> clazz, long id) {
        openDatabase();
        Cursor cursor = null;
        try {
            cursor = mDb.query(DatabaseUtils.getTableName(clazz), null, "_id=" + id, null, null, null, null);
            List<T> list = DatabaseUtils.getEntity(cursor, clazz, mClazzCache);
            return list != null && !list.isEmpty() ? list.get(0) : null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * accroding sql get cursor
     *
     * @param sql
     * @return
     */

    @Override
    public Cursor findBySql(String sql, String[] Args) {
        openDatabase();
        return mDb.rawQuery(sql, Args);
    }

    @Override
    public void findBySql(DataHandler handler, String sql, String[] Args) {
        openDatabase();
        Cursor cursor = null;
        try {
            cursor = mDb.rawQuery(sql, Args);
            handler.handler(cursor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * delete a data for id
     */
    @Override
    public int deleteById(Class<? extends CommonEntity> clazz, long id) {
        openDatabase();
        CommonEntity obj = findById(clazz, (int) id);
        int state = mDb.delete(DatabaseUtils.getTableName(clazz), "_id=" + id, null);
        if (state >= 1) {
            onDeleteData(obj);
        }
        return state;
    }

    /**
     * delete a data for id
     */
    @Override
    public void deleteAllData(Class<?> clazz) {
        openDatabase();
        mDb.execSQL("delete from " + DatabaseUtils.getTableName(clazz));
    }

    /**
     * according column of value delete data
     */
    @Override
    public <T> void deleteByColumn(Class<?> clazz, String column, T t) {
        openDatabase();
        mDb.delete(DatabaseUtils.getTableName(clazz), column + "=" + t, null);
    }

    @Override
    public int deleteByWhere(Class<?> clazz, String where, String[] whereArgs) {
        openDatabase();
        return mDb.delete(DatabaseUtils.getTableName(clazz), where, whereArgs);
    }

    /**
     * delete table of class
     */
    @Override
    public void deleteTable(Class<?> clazz) {
        openDatabase();
        mDb.execSQL("DROP TABLE IF EXISTS" + DatabaseUtils.getTableName(clazz));
    }

    /**
     * according id update data from class
     */
    @Override
    public int updateById(Class<? extends CommonEntity> clazz, ContentValues values, long id) {
        openDatabase();
        int state = mDb.update(DatabaseUtils.getTableName(clazz), values, "_id=" + id, null);
        if (state >= 1) {
            CommonEntity obj = findById(clazz, (int) id);
            onUpdateData(obj);
        }
        return state;
    }

    @Override
    public int updateBy(CommonEntity obj) {
        openDatabase();
        if (obj == null) return 0;

        Field[] fields = mClazzCache.getKeyValue(obj.getClass());
        ContentValues values = new ContentValues();
        Field.setAccessible(fields, true);
        for (Field fd : fields) {
            String fieldName = fd.getName().toLowerCase();
            if (fieldName.equalsIgnoreCase("id") || fieldName.equalsIgnoreCase("_id")) {
                continue;
            }
            DatabaseUtils.putValues(values, fd, obj);
        }

        int state = mDb.update(DatabaseUtils.getTableName(obj.getClass()), values, "_id=" + obj.getId(), null);
        if (state >= 1) {
            onUpdateData(obj);
        }
        return state;
    }


    /**
     * according column of value update data
     */
    @Override
    public <T> int updateByColumn(Class<?> clazz, ContentValues values, String column, T t) {
        openDatabase();
        return mDb.update(DatabaseUtils.getTableName(clazz), values, column + "=" + t, null);
    }

    private void openDatabase() {
        if (mDb == null || !mDb.isOpen()) {
            mDb = mHelper.getWritableDatabase();
        }
    }
}
