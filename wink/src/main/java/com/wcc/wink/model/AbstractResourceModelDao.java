package com.wcc.wink.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.wcc.wink.Resource;
import com.wcc.wink.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by wenbiao.xie on 2016/6/16.
 */
public abstract class AbstractResourceModelDao<E extends Resource> implements ModelDao<String, E> {

    private WinkSQLiteHelper mHelper;
    protected AbstractResourceModelDao(Context context) {
        mHelper = WinkSQLiteHelper.sharedHelper();
    }

    protected SQLiteDatabase getReadableDatabase() {
        return mHelper.getReadableDatabase();
    }

    protected SQLiteDatabase getWritableDatabase() {
        return mHelper.getWritableDatabase();
    }

    protected void checkResource(E t) {
        if (t == null)
            throw new NullPointerException("param t is null");

        if (TextUtils.isEmpty(t.getKey()))
            throw new IllegalStateException("cannot saveOrUpdate "+ t.getClass().getSimpleName()
                    +" with invalid key");
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void saveOrUpdate(E t) {
        SQLiteDatabase db = getWritableDatabase();
        if (!exists(db, t.getKey())) {
            insert(db, t);
        } else {
            update(db, t);
        }
    }

    @Override
    public void saveOrUpdateAll(List<E> entities) {
        if (Utils.isEmpty(entities))
            return;

        if (entities.size() == 1) {
            saveOrUpdate(entities.get(0));
            return;
        }

        List<E> updates = null;
        List<E> inserts = null;

        Set<String> keys = allKeys();
        if (Utils.isEmpty(keys)) {
            inserts = entities;
        }
        else {
            updates = new ArrayList<>();
            inserts = new ArrayList<>();
            for (E entity: entities) {
                if (keys.contains(entity.getKey())) {
                    updates.add(entity);
                } else {
                    inserts.add(entity);
                }
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!Utils.isEmpty(inserts)) {
                for (E entity: inserts) {
                    insert(db, entity);
                }
            }

            if (!Utils.isEmpty(updates)) {
                for (E entity: updates) {
                    update(db, entity);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    public abstract E fromCursor(Cursor cursor);

    public abstract  ContentValues fromContent(E entity);

    public abstract boolean insert(SQLiteDatabase db, E t);

    public abstract boolean update(SQLiteDatabase db, E t);

    public abstract boolean exists(SQLiteDatabase db, String key);

    public abstract Set<String> allKeys();

}
