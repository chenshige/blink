package com.wcc.wink.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.wcc.wink.Resource;
import com.wcc.wink.loader.GenericLoaderFactory;
import com.wcc.wink.loader.ResourceLoader;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.request.Tracer;
import com.wcc.wink.util.Streams;
import com.wcc.wink.util.Utils;
import com.wcc.wink.util.WLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.wcc.wink.model.ModelContract.DownloadColumns;
import static com.wcc.wink.model.ModelContract.Tables;

/**
 * Created by wenbiao.xie on 2016/6/16.
 */
public class SQLiteModelDao extends AbstractResourceModelDao<DownloadInfo> {

    private final GenericLoaderFactory mLoaderFactory;
    private final GenericModelFactory mModelFactory;
    private Model.Dao mResourceDao;

    public SQLiteModelDao(Context context, GenericLoaderFactory factory,
                          GenericModelFactory modelFactory) {
        super(context);
        this.mLoaderFactory = factory;
        this.mModelFactory = modelFactory;
    }

    @Override
    public void init() throws Exception {
        super.init();

        Model.Dao dao = Model.get();
        dao.init();
        mResourceDao = dao;
    }

    public DownloadInfo fromCursor(Cursor cursor) {
        DownloadInfo di = new DownloadInfo();
        di.setId(cursor.getLong(0));
        di.setKey(cursor.getString(cursor.getColumnIndex(DownloadColumns.KEY)));
        di.setTitle(cursor.getString(cursor.getColumnIndex(DownloadColumns.TITLE)));
        di.setTotalSizeInBytes(cursor.getLong(cursor.getColumnIndex(DownloadColumns.TOTAL)));
        di.setDownloadedSizeInBytes(cursor.getLong(cursor.getColumnIndex(DownloadColumns.DOWNLOADED)));
        di.setUrl(cursor.getString(cursor.getColumnIndex(DownloadColumns.URL)));
        di.setLocalFilePath(cursor.getString(cursor.getColumnIndex(DownloadColumns.FILE_PATH_URI)));

        di.setDownloadMode(cursor.getInt(cursor.getColumnIndex(DownloadColumns.MODE)));
        di.setDownloadProgress(cursor.getInt(cursor.getColumnIndex(DownloadColumns.PROGRESS)));
        int state = cursor.getInt(cursor.getColumnIndex(DownloadColumns.STATE));

        switch (state) {
            case ResourceStatus.INIT:
                di.setDownloadState(ResourceStatus.PAUSE);
                break;
            case ResourceStatus.DOWNLOADING:
            case ResourceStatus.WAIT:
                di.setDownloadState(ResourceStatus.DOWNLOAD_FAILED);
                break;
            default:
                di.setDownloadState(state);
        }

        String resourceKey = cursor.getString(cursor.getColumnIndex(DownloadColumns.RESOURCE_KEY));
        di.setResourceKey(resourceKey);

        final Tracer tracer = di.getTracer();
        tracer.startTime = cursor.getLong(cursor.getColumnIndex(DownloadColumns.TRACER_START));
        tracer.endTime = cursor.getLong(cursor.getColumnIndex(DownloadColumns.TRACER_END));
        tracer.connectNetworkUsedTime = cursor.getInt(cursor.getColumnIndex(DownloadColumns.TRACER_CONNECT));
        tracer.usedTime = cursor.getInt(cursor.getColumnIndex(DownloadColumns.TRACER_DURATION));
        tracer.maxSpeed = cursor.getLong(cursor.getColumnIndex(DownloadColumns.TRACER_MAX_SPEED));
        tracer.avgSpeed = cursor.getLong(cursor.getColumnIndex(DownloadColumns.TRACER_AVG_SPEED));
        tracer.tryTimes = cursor.getInt(cursor.getColumnIndex(DownloadColumns.TRACER_TRYS));

        String resourceClass = cursor.getString(cursor.getColumnIndex(DownloadColumns.RESOURCE_CLASS));
        if (!TextUtils.isEmpty(resourceClass)) {
            try {
                Class clz = Class.forName(resourceClass);
                ResourceLoader<Resource, DownloadInfo> loader =
                        mLoaderFactory.buildResourceLoader(clz, DownloadInfo.class);
                di.setLoader(loader);
                ModelDao<String, Resource> dao = mModelFactory.buildResourceModel(String.class, clz);
                di.setResource(dao.get(resourceKey));

            } catch (ClassNotFoundException e) {

            }
        }

        di.setDownloaderType(cursor.getInt(cursor.getColumnIndex(DownloadColumns.DOWNLOADER_TYPE)));
        String sranges = cursor.getString(cursor.getColumnIndex(DownloadColumns.DOWNLOAD_RANGES));
        if (!TextUtils.isEmpty(sranges)) {
            try {
                JSONArray jsonArray = new JSONArray(sranges);
                int size = jsonArray.length();
                if (size > 0) {
                    DownloadInfo.DownloadRange[] ranges =
                            new DownloadInfo.DownloadRange[size];

                    for (int i = 0; i < size; i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        DownloadInfo.DownloadRange range = new DownloadInfo.DownloadRange();
                        range.start = obj.getLong("start");
                        range.length = obj.getLong("length");
                        range.downloaded = obj.getLong("downloaded");
                        ranges[i] = range;
                    }

                    di.setRanges(ranges);
                }

            } catch (JSONException e) {

            }
        }

        return di;
    }

    public ContentValues fromContent(DownloadInfo entity) {
        ContentValues cv = new ContentValues();

        cv.put(DownloadColumns.KEY, entity.getKey());
        cv.put(DownloadColumns.TITLE, entity.getTitle());
        cv.put(DownloadColumns.TOTAL, entity.getTotalSizeInBytes());
        cv.put(DownloadColumns.DOWNLOADED, entity.getDownloadedSizeInBytes());
        cv.put(DownloadColumns.MODE, entity.getDownloadMode());
        cv.put(DownloadColumns.PROGRESS, entity.getDownloadProgress());
        cv.put(DownloadColumns.URL, entity.getUrl());
        cv.put(DownloadColumns.STATE, entity.getDownloadState());
        cv.put(DownloadColumns.FILE_PATH_URI, entity.getLocalFilePath());

        if (entity.getResourceClass() != null) {
            String className = entity.getResourceClass().getCanonicalName();
            // 不直接存储classname, 由model进行关联
//            cv.put(DownloadColumns.RESOURCE_CLASS, className);
            cv.put(DownloadColumns.RESOURCE_KEY, entity.getResourceKey());
            Model m = mResourceDao.getByClass(className);
            if (m != null)
                cv.put(DownloadColumns.RESOURCE_MODEL_ID, m.id);
        }

        final Tracer tracer = entity.getTracer();
        cv.put(DownloadColumns.TRACER_START, tracer.startTime);
        cv.put(DownloadColumns.TRACER_END, tracer.endTime);
        cv.put(DownloadColumns.TRACER_DURATION, tracer.usedTime);
        cv.put(DownloadColumns.TRACER_CONNECT, tracer.connectNetworkUsedTime);
        cv.put(DownloadColumns.TRACER_MAX_SPEED, tracer.maxSpeed);
        cv.put(DownloadColumns.TRACER_AVG_SPEED, tracer.avgSpeed);
        cv.put(DownloadColumns.TRACER_TRYS, tracer.tryTimes);

        cv.put(DownloadColumns.DOWNLOADER_TYPE, entity.getDownloaderType());
        final DownloadInfo.DownloadRange[] ranges = entity.getRanges();
        if (ranges != null && ranges.length > 0) {
            JSONArray jsonArray = new JSONArray();
            try {
                for (int i = 0; i < ranges.length; i++) {
                    JSONObject obj = new JSONObject();
                    DownloadInfo.DownloadRange range = ranges[i];
                    obj.put("start", range.start);
                    obj.put("length", range.length);
                    obj.put("downloaded", range.downloaded);
                    jsonArray.put(obj);
                }

                cv.put(DownloadColumns.DOWNLOAD_RANGES, jsonArray.toString());
            } catch (JSONException e) {
            }
        }

        return cv;
    }

    @Override
    public Collection<DownloadInfo> all() {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM " + Tables.DOWNLOAD_INFO_VIEW;
        Cursor cursor = db.rawQuery(sql, null);
        List<DownloadInfo> results = null;
        List<DownloadInfo> deletes = null;
        try {
            if (cursor != null && cursor.moveToNext()) {
                results = new ArrayList<>(cursor.getCount());
                do {

                    DownloadInfo di = fromCursor(cursor);
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
    public DownloadInfo get(String key) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM " + Tables.DOWNLOAD_INFO_VIEW + " where " +
                DownloadColumns.KEY + "=?";

        String[] selectionsArgs = new String[] {key};
        Cursor cursor = db.rawQuery(sql, selectionsArgs);
        DownloadInfo result = null;
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
    public void delete(DownloadInfo entity) {
        SQLiteDatabase db = getWritableDatabase();
        String where = DownloadColumns.KEY + "=?";
        String[] whereArgs = new String[] {entity.getKey()};
        db.beginTransaction();
        try {
            db.delete(Tables.DOWNLOADS_TABLE, where, whereArgs);

            if (entity.getResource() != null) {
                ModelDao<String, Resource> dao = mModelFactory.buildResourceModel(String.class,
                        entity.getResourceClass());
                dao.delete(entity.getResource());
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

    }

    @Override
    public void deleteAll(List<DownloadInfo> entities) {
        if (Utils.isEmpty(entities))
            return;

        if (entities.size() == 1) {
            delete(entities.get(0));
            return;
        }

        String where =  DownloadColumns.KEY + "=?";
        String[] whereArgs = new String[1];

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (DownloadInfo di: entities) {
                whereArgs[0] = di.getKey();
                db.delete(Tables.DOWNLOADS_TABLE,
                        where, whereArgs);
                if (di.getResource() != null) {
                    ModelDao<String, Resource> dao = mModelFactory.buildResourceModel(String.class,
                            di.getResourceClass());
                    dao.delete(di.getResource());
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean exists(SQLiteDatabase db, String key) {
        String where = DownloadColumns.KEY + "=?";
        String[] whereArgs = new String[] {key};
        Cursor cursor = db.query(Tables.DOWNLOAD_INFO_VIEW,null,
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
        final Cursor cursor = db.query(Tables.DOWNLOAD_INFO_VIEW, new String[] {DownloadColumns.KEY}, null, null, null, null, null);

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

    public boolean insert(SQLiteDatabase db, DownloadInfo t) {
        ContentValues cv = fromContent(t);
        long id = db.insert(Tables.DOWNLOADS_TABLE, null, cv);
        if (id != -1) {
            t.setId(id);
        }
        return (id != -1);
    }

    public boolean update(SQLiteDatabase db, DownloadInfo t) {

        String where;
        String[] whereArgs;
        if (t.getId() > 0) {
            where = DownloadColumns._ID + "=?";
            whereArgs = new String[] {String.valueOf(t.getId())};
        } else {
            where = DownloadColumns.KEY + "=?";
            whereArgs = new String[] {t.getKey()};
        }

        ContentValues cv = fromContent(t);
        cv.remove(DownloadColumns.KEY);
        int count = db.update(Tables.DOWNLOADS_TABLE, cv, where, whereArgs);
        return (count > 0);
    }


    @Override
    public void saveOrUpdate(DownloadInfo t) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (t.getId() == 0 && !exists(db, t.getKey())) {
                insert(db, t);
            } else {
                update(db, t);
            }

            if (t.getResource() != null) {
                ModelDao<String, Resource> dao = mModelFactory.buildResourceModel(String.class,
                        t.getResourceClass());
                dao.saveOrUpdate(t.getResource());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void saveOrUpdateAll(List<DownloadInfo> entities) {
        if (Utils.isEmpty(entities))
            return;

        if (entities.size() == 1) {
            saveOrUpdate(entities.get(0));
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {

            for (DownloadInfo t: entities) {
                if (t.getId() == 0 && !exists(db, t.getKey())) {
                    insert(db, t);
                } else {
                    update(db, t);
                }

                if (t.getResource() != null) {
                    ModelDao<String, Resource> dao = mModelFactory.buildResourceModel(String.class,
                            t.getResourceClass());
                    dao.saveOrUpdate(t.getResource());
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    protected void checkResource(DownloadInfo di) {
        super.checkResource(di);
        if (di.getDownloadState() == ResourceStatus.DOWNLOADED ) {
            File file = new File(di.getLocalFilePath());
            if (!file.exists() || file.length() != di.getTotalSizeInBytes()) {
                throw new IllegalStateException("downloaded file deleted or length not match");
            }
        }

        else if (di.getDownloadState() == ResourceStatus.DELETED) {
            throw new IllegalStateException("database record already mark deleted");
        }

        else if (di.getDownloadedSizeInBytes() > 0) {
            File file = new File(di.getLocalFilePath());
            if (!file.exists() || file.length() != di.getTotalSizeInBytes()) {
                throw new IllegalStateException("downloading file deleted or length not match");
            }
        }
    }
}
