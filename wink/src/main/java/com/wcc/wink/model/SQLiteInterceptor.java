package com.wcc.wink.model;

import android.database.sqlite.SQLiteDatabase;

/**
 * SQL语句插入器
 * Created by wenbiao.xie on 2016/10/27.
 */

public interface SQLiteInterceptor {
    void beforeCreate(SQLiteDatabase db);
    void afterCreated(SQLiteDatabase db);

    void beforeUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    void afterUpgraded(SQLiteDatabase db, int oldVersion, int newVersion);
}
