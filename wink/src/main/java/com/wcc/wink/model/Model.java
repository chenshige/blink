package com.wcc.wink.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wcc.wink.annotations.ModelTable;
import com.wcc.wink.util.Singleton;
import com.wcc.wink.util.Streams;
import com.wcc.wink.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wenbiao.xie on 2016/6/24.
 */
public class Model {

    private static final Singleton<Dao> _instance = new Singleton<Dao>() {
        @Override
        protected Dao create() {
            return new Dao();
        }
    };

    public static Dao get() {
        return _instance.get();
    }

    public int id;
    public String name;
    public final String tableName;
    public final String modelClass;
    public int version;

    Model(String table, String cls) {
        this.tableName = table;
        this.modelClass = cls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Model model = (Model) o;
        return tableName.equals(model.tableName);
    }

    @Override
    public int hashCode() {
        return tableName.hashCode();
    }

    public boolean sameTo(Model other) {
        if (this == other)
            return true;

        if (id != 0 && other.id != 0 && id != other.id)
            return false;

        if (!tableName.equals(other.tableName))
            return false;

        if (!modelClass.equals(other.modelClass))
            return false;

        return this.version == other.version;
    }

    static Model fromCursor(Cursor cursor) {
        String tableName = cursor.getString(cursor.getColumnIndex(ModelContract.ModelColumns.TABLE_NAME));
        String modelClass = cursor.getString(cursor.getColumnIndex(ModelContract.ModelColumns.MODEL_CLASS));
        Model model = new Model(tableName, modelClass);
        model.id = cursor.getInt(cursor.getColumnIndex(ModelContract.ModelColumns._ID));
        model.version = cursor.getInt(cursor.getColumnIndex(ModelContract.ModelColumns.VERSION));
        model.name = cursor.getString(cursor.getColumnIndex(ModelContract.ModelColumns.NAME));
        return model;
    }

    public static Model fromAnnotation(ModelTable table) {
        Model model = new Model(table.table(), table.model().getCanonicalName());
        model.name = table.name();
        model.version = table.version();
        return model;
    }

    static ContentValues toValues(Model model) {
        ContentValues cv = new ContentValues();
        cv.put(ModelContract.ModelColumns.VERSION, model.version);
        cv.put(ModelContract.ModelColumns.NAME, model.name);
        cv.put(ModelContract.ModelColumns.TABLE_NAME, model.tableName);
        cv.put(ModelContract.ModelColumns.MODEL_CLASS, model.modelClass);
        return cv;
    }

    public static class Dao implements ModelDao<String, Model> {
        private WinkSQLiteHelper mHelper;
        private Map<String, Model> mCached;
        private boolean inited;

        private Dao() {
            mHelper = WinkSQLiteHelper.sharedHelper();
            mCached = new HashMap<>();
        }

        public Map<String, Model> caches() {
            return mCached;
        }

        protected SQLiteDatabase getReadableDatabase() {
            return mHelper.getReadableDatabase();
        }

        protected SQLiteDatabase getWritableDatabase() {
            return mHelper.getWritableDatabase();
        }

        @Override
        public void init() throws Exception {
            if (inited)
                return;
            Collection<Model> results = queryAll();
            if (results != null) {
                for (Model m: results) {
                    mCached.put(m.tableName, m);
                }
            }

            inited = true;
        }

        private Collection<Model> queryAll() {
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT * FROM " + ModelContract.Tables.MODEL_TABLE;
            Cursor cursor = db.rawQuery(sql, null);
            List<Model> results = null;
            try {
                if (cursor != null && cursor.moveToNext()) {
                    results = new ArrayList<>(cursor.getCount());
                    do {
                        Model di = fromCursor(cursor);
                        results.add(di);
                    } while (cursor.moveToNext());
                }
            } finally {
                Streams.safeClose(cursor);
            }

            return results;
        }

        @Override
        public Collection<Model> all() {
            if (inited) {
                return mCached.values();
            }

            return queryAll();
        }

        public Model getByClass(String cls) {
            if (inited) {

                if (mCached.isEmpty())
                    return null;

                Collection<Model> values = mCached.values();
                for (Model m: values) {
                    if (m.modelClass.equals(cls))
                        return m;
                }

                return null;
            }

            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT * FROM " + ModelContract.Tables.MODEL_TABLE + " where " +
                    ModelContract.ModelColumns.MODEL_CLASS + "=?";

            String[] selectionsArgs = new String[] {cls};
            Cursor cursor = db.rawQuery(sql, selectionsArgs);
            Model result = null;
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
        public Model get(String key) {
            if (inited) {
                return mCached.get(key);
            }

            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT * FROM " + ModelContract.Tables.MODEL_TABLE + " where " +
                    ModelContract.ModelColumns.TABLE_NAME + "=?";

            String[] selectionsArgs = new String[] {key};
            Cursor cursor = db.rawQuery(sql, selectionsArgs);
            Model result = null;
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
        public void delete(Model entity) {
            SQLiteDatabase db = getWritableDatabase();
            String where = ModelContract.ModelColumns.TABLE_NAME + "=?";
            String[] whereArgs = new String[] {entity.tableName};
            db.delete(ModelContract.Tables.MODEL_TABLE, where, whereArgs);

            mCached.remove(entity);
        }

        @Override
        public void deleteAll(List<Model> entities) {
            if (Utils.isEmpty(entities))
                return;

            if (entities.size() == 1) {
                delete(entities.get(0));
                return;
            }

            String where =  ModelContract.ModelColumns.TABLE_NAME + "=?";
            String[] whereArgs = new String[1];

            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                for (Model di: entities) {
                    whereArgs[0] = di.tableName;
                    db.delete(ModelContract.Tables.MODEL_TABLE,
                            where, whereArgs);
                }
                db.setTransactionSuccessful();

                for (Model di: entities) {
                    mCached.remove(di.tableName);
                }
            } finally {
                db.endTransaction();
            }
        }

        public boolean exists(Model model) {
            if (inited) {
                return mCached.containsKey(model.tableName);
            }

            final SQLiteDatabase db = getReadableDatabase();
            String where = ModelContract.ModelColumns.TABLE_NAME + "=?";
            String[] whereArgs = new String[] {model.tableName};
            Cursor cursor = db.query(ModelContract.Tables.MODEL_TABLE,null,
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

        private boolean exists(SQLiteDatabase db, String key) {
            if (inited) {
                return mCached.containsKey(key);
            }

            String where = ModelContract.ModelColumns.TABLE_NAME + "=?";
            String[] whereArgs = new String[] {key};
            Cursor cursor = db.query(ModelContract.Tables.MODEL_TABLE,null,
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

        public boolean insert(SQLiteDatabase db, Model t) {
            ContentValues cv = toValues(t);
            long id = db.insert(ModelContract.Tables.MODEL_TABLE, null, cv);
            if (id != -1) {
                t.id = (int) id;
            }
            return (id > 0);
        }

        public boolean update(SQLiteDatabase db, Model t) {
            String where;
            String[] whereArgs;
            if (t.id > 0) {
                where = ModelContract.ModelColumns._ID + "=?";
                whereArgs = new String[] {String.valueOf(t.id)};
            } else {
                where = ModelContract.ModelColumns.TABLE_NAME + "=?";
                whereArgs = new String[] {t.tableName};
            }

            ContentValues cv = toValues(t);
            cv.remove(ModelContract.ModelColumns.TABLE_NAME);
            int count = db.update(ModelContract.Tables.MODEL_TABLE, cv, where, whereArgs);
            return (count > 0);
        }

        @Override
        public void saveOrUpdate(Model t) {
            SQLiteDatabase db = getWritableDatabase();
            if (t.id== 0 && !exists(db, t.tableName)) {
                insert(db, t);
            } else {
                update(db, t);
            }

            mCached.put(t.tableName, t);
        }

        @Override
        public void saveOrUpdateAll(List<Model> entities) {
            if (Utils.isEmpty(entities))
                return;

            if (entities.size() == 1) {
                saveOrUpdate(entities.get(0));
                return;
            }

            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();

            try {

                for (Model t: entities) {
                    if (t.id == 0 && !exists(db, t.tableName)) {
                        insert(db, t);
                    } else {
                        update(db, t);
                    }
                }
                db.setTransactionSuccessful();
                for (Model t: entities) {
                    mCached.put(t.tableName, t);
                }

            } finally {
                db.endTransaction();
            }
        }
    }
}
