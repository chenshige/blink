package com.blink.browser.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;

import com.blink.browser.R;
import com.blink.browser.bean.RecommendUrlEntity;
import com.blink.browser.util.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_RECOMMEND_WEBSITE;

public class BrowserSQLiteHelper {
    private static final String TAG = "BrowserSQLiteHelper";
    public static String TABLE_RECOMMEND_WEB_URL = "recommendurlentity";
    public static String TABLE_HOTURL = "hoturlentity";
    public static String TABLE_INPUTURL = "inputurlentity";
    public static String TABLE_DOWNLOAD_URL = "downloadurlentity";
    public static String TABLE_COUNTRY_CODE = "countrycode";
    public static String TALBE_HISTORY_URL = "history";
    public static String TABLE_BOOK_MARK_URL = "bookmarks";
    public static String TABLE_SEARCH_ENGINE = "searchengineentity";

    private Context mContext;

    public BrowserSQLiteHelper(Context context) {
        this.mContext = context;
    }

    public void onCreate(SQLiteDatabase db) {
        String commandUrl = "CREATE TABLE " + TABLE_HOTURL + "("
                + "_id INTEGER PRIMARY KEY,"
                + " url TEXT NOT NULL,"
                + " title TEXT,"
                + " displayname TEXT,"
                + " weight INTEGER,"
                + " uid INTEGER,"
                + " imageicon BLOB DEFAULT NULL,"
                + " imageurl TEXT DEFAULT NULL);";
        db.execSQL(commandUrl);

        String recommendWebUrl = "CREATE TABLE " + TABLE_RECOMMEND_WEB_URL + "("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " url TEXT NOT NULL,"
                + " title TEXT,"
                + " displayname TEXT,"
                + " language TEXT DEFAULT NULL,"
                + " weight INTERGER DEFAULT -1,"
                + " sid INTERGER ,"
                + " ord INTERGER DEFAULT -1,"  // 避开order关键字
                + " imageicon BLOB DEFAULT NULL,"
                + " imageurl TEXT DEFAULT NULL);";
        db.execSQL(recommendWebUrl);

        createInputUrl(db);
        createDownloadTable(db);
        createSearchEngineTable(db);
    }

    public void initData(SQLiteDatabase db) {
        initRecommendData(db);
    }

    private void initRecommendData(SQLiteDatabase db) {
        Resources res = mContext.getResources();
        String[] recommendWebsites = res.getStringArray(R.array.recommend_websites);
        int size = recommendWebsites.length / 3;
        for (int i = 0; i < size; i++) {
            ContentValues values = new ContentValues();
            values.put(RecommendUrlEntity.Column.DISPLAY_NAME, recommendWebsites[3*i]);
            values.put(RecommendUrlEntity.Column.URL, recommendWebsites[3*i+1]);
            values.put(RecommendUrlEntity.Column.IMAGE_URL, recommendWebsites[3*i+2]);
            values.put(RecommendUrlEntity.Column.WEIGHT, WEIGHT_RECOMMEND_WEBSITE);
            values.put(RecommendUrlEntity.Column.ORD, size - i);
            db.insert(TABLE_RECOMMEND_WEB_URL, null, values);
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: Do something for upgrade
        if (oldVersion <= 32) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN ord INTERGER DEFAULT -1", TABLE_RECOMMEND_WEB_URL));
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOAD_URL);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH_ENGINE);
            createDownloadTable(db);
            createSearchEngineTable(db);
        }

        if (oldVersion <= 33) {
            db.execSQL(String.format("DELETE FROM %s", TABLE_RECOMMEND_WEB_URL));
            // 初始化新的数据
            initRecommendData(db);
        }
        if (oldVersion <= 36) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_INPUTURL);
            createInputUrl(db);
        }
    }

    private void createInputUrl(SQLiteDatabase db) {
        String inputUrlSql = "CREATE TABLE " + TABLE_INPUTURL + "("
                + "_id INTEGER PRIMARY KEY,"
                + " url TEXT NOT NULL,"
                + " title TEXT,"
                + " imageurl TEXT DEFAULT NULL,"
                + " count int,"
                + " imageicon BLOB,"
                + " inputword TEXT,"
                + " modifiedtime INTEGER,"
                + " inputtime INTEGER);";
        db.execSQL(inputUrlSql);
    }

    private void createSearchEngineTable(SQLiteDatabase db) {
        String searchEngineTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SEARCH_ENGINE + "("
                + "_id INTEGER PRIMARY KEY,"
                + " title TEXT UNIQUE,"
                + " engine_url TEXT NOT NULL,"
                + " imageurl TEXT DEFAULT NULL,"
                + " imageicon BLOB DEFAULT NULL,"
                + " engine_order INTERGER DEFAULT -1,"
                + " is_default INTERGER DEFAULT 0,"
                + " url TEXT DEFAULT NULL,"
                + " encoding TEXT DEFAULT NULL,"
                + " create_time INTERGER DEFAULT -1);";
        db.execSQL(searchEngineTable);
    }

    private void createDownloadTable(SQLiteDatabase db) {
        String downloadUrl = "CREATE TABLE " + TABLE_DOWNLOAD_URL + "("
                + "_id INTEGER PRIMARY KEY,"
                + " url TEXT NOT NULL,"
                + " title TEXT,"
                + " status int,"
                + " size int,"
                + " refrence int,"
                + " mimetype TEXT DEFAULT NULL,"
                + " imageicon BLOB DEFAULT NULL,"
                + " imageurl TEXT DEFAULT NULL,"
                + " originurl TEXT DEFAULT NULL,"
                + " time TEXT);";
        db.execSQL(downloadUrl);
    }
}
