package com.blink.browser.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.blink.browser.bean.CommonEntity;

import java.util.List;

public interface DataController {
    long insert(CommonEntity t);

    <T> T findById(Class<T> clazz, long id);

    <T> List<T> findAll(Class<T> clazz);

    <T> List<T> findByArgs(Class<T> clazz, String select, String[] selectArgs);

    <T> List<T> findByArgs(Class<T> clazz, String select, String[] selectArgs, String groupBy, String having, String orderBy);

    int deleteById(Class<? extends CommonEntity> clazz, long id);

    <T> void deleteByColumn(Class<?> clazz, String column, T t);

    int deleteByWhere(Class<?> clazz, String where, String[] whereArgs);

    void deleteAllData(Class<?> clazz);

    Cursor findBySql(String sql, String[] Args);

    void findBySql(DataHandler handler,String sql, String[] Args);

    int updateById(Class<? extends CommonEntity> clazz, ContentValues values, long id);

    int updateBy(CommonEntity obj);

    <T> int updateByColumn(Class<?> clazz, ContentValues values, String column, T t);

    void deleteTable(Class<?> clazz);

    void closeDataBase();


    /**
     * data 内嵌处理
     */
    public interface DataHandler {
        void handler(Cursor cursor);
    }
}
