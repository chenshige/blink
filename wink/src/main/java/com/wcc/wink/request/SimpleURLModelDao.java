package com.wcc.wink.request;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wcc.wink.annotations.ModelTable;
import com.wcc.wink.model.AbstractResourceModelDao;
import com.wcc.wink.model.GenericModelFactory;
import com.wcc.wink.model.ModelContract;
import com.wcc.wink.model.ModelDao;
import com.wcc.wink.model.ResourceModelFactory;
import com.wcc.wink.util.Streams;
import com.wcc.wink.util.Utils;
import com.wcc.wink.util.WLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by wenbiao.xie on 2016/10/27.
 */

public class SimpleURLModelDao extends AbstractResourceModelDao<SimpleURLResource> {

    @ModelTable(name="Simple URL Resource", table="simples", model=SimpleURLResource.class)
    public static class Factory implements ResourceModelFactory<String, SimpleURLResource> {

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onOpen(SQLiteDatabase db) {

        }

        @Override
        public ModelDao<String, SimpleURLResource> build(Context context, GenericModelFactory factories) {
            return new SimpleURLModelDao(context);
        }

        @Override
        public void teardown() {

        }
    }

    private SimpleURLModelDao(Context context) {
        super(context);
    }

    @Override
    public SimpleURLResource fromCursor(Cursor cursor) {
        SimpleURLResource sur = new SimpleURLResource();
        sur.setId(cursor.getInt(0));
        sur.mKey = cursor.getString(cursor.getColumnIndex(ModelContract.URLColumns.KEY));
        sur.mTitle = cursor.getString(cursor.getColumnIndex(ModelContract.URLColumns.TITLE));
        sur.mURL = cursor.getString(cursor.getColumnIndex(ModelContract.URLColumns.URL));
        sur.mMimeType = cursor.getString(cursor.getColumnIndex(ModelContract.URLColumns.MIME));
        sur.mExt = cursor.getString(cursor.getColumnIndex(ModelContract.URLColumns.EXT));
        return sur;
    }

    @Override
    public ContentValues fromContent(SimpleURLResource entity) {
        ContentValues cv = new ContentValues();
        cv.put(ModelContract.URLColumns.KEY, entity.mKey);
        cv.put(ModelContract.URLColumns.TITLE, entity.mTitle);
        cv.put(ModelContract.URLColumns.URL, entity.mURL);
        cv.put(ModelContract.URLColumns.MIME, entity.mMimeType);
        cv.put(ModelContract.URLColumns.EXT, entity.mExt);
        return cv;
    }

    @Override
    public boolean insert(SQLiteDatabase db, SimpleURLResource t) {
        ContentValues cv = fromContent(t);
        long id = db.insert(ModelContract.Tables.SIMPLE_TABLE, null, cv);
        if (id != -1) {
            t.setId((int) id);
        }
        return (id != -1);
    }

    @Override
    public boolean update(SQLiteDatabase db, SimpleURLResource t) {
        String where;
        String[] whereArgs;
        if (t.getId() > 0) {
            where = ModelContract.URLColumns._ID + "=?";
            whereArgs = new String[] {String.valueOf(t.getId())};
        } else {
            where = ModelContract.URLColumns.KEY + "=?";
            whereArgs = new String[] {t.getKey()};
        }

        ContentValues cv = fromContent(t);
        cv.remove(ModelContract.URLColumns.KEY);
        int count = db.update(ModelContract.Tables.SIMPLE_TABLE, cv, where, whereArgs);
        return (count > 0);
    }

    @Override
    public boolean exists(SQLiteDatabase db, String key) {
        String where = ModelContract.URLColumns.KEY + "=?";
        String[] whereArgs = new String[] {key};
        Cursor cursor = db.query(ModelContract.Tables.SIMPLE_TABLE,null,
                where, whereArgs, null, null, null);
        try {
            if (cursor != null && cursor.moveToNext()) {
                return cursor.getCount() > 0;
            }
        } finally {
            Streams.safeClose(cursor);
        }

        return false;
    }

    @Override
    public Set<String> allKeys() {
        SQLiteDatabase db = getReadableDatabase();
        final Cursor cursor = db.query(ModelContract.Tables.SIMPLE_TABLE,
                new String[] {ModelContract.URLColumns.KEY}, null, null, null, null, null);

        Set<String> results = null;
        try {

            if (cursor != null && cursor.getCount() > 0) {
                results = new HashSet<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    results.add(cursor.getString(0));
                }
            }
        } finally {
            Streams.safeClose(cursor);
        }
        return results;
    }

    @Override
    public Collection<SimpleURLResource> all() {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM " + ModelContract.Tables.SIMPLE_TABLE;
        Cursor cursor = db.rawQuery(sql, null);
        List<SimpleURLResource> results = null;
        List<SimpleURLResource> deletes = null;
        try {
            if (cursor != null && cursor.moveToNext()) {
                results = new ArrayList<>(cursor.getCount());
                do {

                    SimpleURLResource di = fromCursor(cursor);
                    try {
                        checkResource(di);
                        results.add(di);
                    } catch (Exception e) {
                        WLog.printStackTrace(e);
                        if (deletes == null)
                            deletes = new ArrayList<>();
                        deletes.add(di);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            Streams.safeClose(cursor);
        }

        // 清理一些无效资源
        if (deletes != null) {
            deleteAll(deletes);
        }
        return results;
    }

    @Override
    public SimpleURLResource get(String key) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM " + ModelContract.Tables.SIMPLE_TABLE + " where " +
                ModelContract.URLColumns.KEY + "=?";

        String[] selectionsArgs = new String[] {key};
        Cursor cursor = db.rawQuery(sql, selectionsArgs);
        SimpleURLResource result = null;
        try {
            if (cursor != null && cursor.moveToNext()) {
                result = fromCursor(cursor);
            }
        } finally {
            Streams.safeClose(cursor);
        }
        return result;
    }

    @Override
    public void delete(SimpleURLResource entity) {
        SQLiteDatabase db = getWritableDatabase();
        String where = ModelContract.URLColumns.KEY + "=?";
        String[] whereArgs = new String[] {entity.getKey()};
        db.delete(ModelContract.Tables.SIMPLE_TABLE, where, whereArgs);
    }

    @Override
    public void deleteAll(List<SimpleURLResource> entities) {
        if (Utils.isEmpty(entities))
            return;

        if (entities.size() == 1) {
            delete(entities.get(0));
            return;
        }

        String where =  ModelContract.URLColumns.KEY + "=?";
        String[] whereArgs = new String[1];

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (SimpleURLResource di: entities) {
                whereArgs[0] = di.getKey();
                db.delete(ModelContract.Tables.SIMPLE_TABLE,
                        where, whereArgs);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
