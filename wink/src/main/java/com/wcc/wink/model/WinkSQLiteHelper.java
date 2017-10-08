package com.wcc.wink.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.wcc.wink.util.Utils;
import com.wcc.wink.util.WLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wenbiao.xie on 2015/5/4.
 */
public class WinkSQLiteHelper extends SQLiteOpenHelper {

    private static final String TAG = "WinkSQLiteHelper";
    public static final String DB_NAME = "wink_internal.db";
    private static final int DB_VERSION = 1;
    private final Context mContext;
    private static WinkSQLiteHelper __helper = null;

    private final SQLiteInterceptor mInterceptor;
    private static class WrapperErrorHandler implements DatabaseErrorHandler {

        private final DatabaseErrorHandler handler;
        public WrapperErrorHandler(DatabaseErrorHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onCorruption(SQLiteDatabase dbObj) {
            if (handler != null)
                handler.onCorruption(dbObj);
        }
    }

    WinkSQLiteHelper(Context context, DatabaseErrorHandler errorHandler, SQLiteInterceptor interceptor) {
        super(context, DB_NAME, null, DB_VERSION, errorHandler);
        this.mContext = context.getApplicationContext();
        this.mInterceptor = interceptor;
    }

    WinkSQLiteHelper(Context context) {
        this(context, null, null);
    }

    public synchronized static void initDatabase(Context context, SQLiteInterceptor interceptor) {
        if (__helper == null) {
            __helper = new WinkSQLiteHelper(context, null, interceptor);
        }

        __helper.getWritableDatabase();
    }

    public static WinkSQLiteHelper sharedHelper() {
        return __helper;
    }

    public String getPath() {
        return getWritableDatabase().getPath();
    }

    static List<String> readSQLsFrom(InputStream is) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<String> sqls = null;

        StringBuilder builder = null;
        boolean first = true;
        do {
            String s = reader.readLine();
            if (s == null)
                break;

            s = s.trim();
            if (s.length() == 0)
                continue;

            // 如果是注释，直接跳过
            if (s.startsWith("--") || s.startsWith("##"))
                continue;

            if (first) {
                builder = new StringBuilder();
            } else {
                builder.append(" ");
            }

            builder.append(s);

            // 如果是SQL语句结尾
            if (s.endsWith(";")) {
                String sql = builder.toString();
                builder = null;
                if (sqls == null)
                    sqls = new ArrayList<String>();

                //WLog.d(TAG, "sql: %s", sql);
                sqls.add(sql);
                first = true;
            } else {
                first = false;
            }

        }while (true);

        return sqls;
    }

    private List<String> getCreateSQLs() {
        final Context context = mContext;
        AssetManager am = context.getAssets();
        InputStream ins = null;
        try {
            ins = am.open(getSQLAssertPath() + "/db.sql");
            return readSQLsFrom(ins);
        } catch (Exception e) {
            WLog.printStackTrace(e);
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private List<String> getDataSQLs() {
        final Context context = mContext;
        AssetManager am = context.getAssets();
        InputStream ins = null;
        try {
            ins = am.open(getSQLAssertPath() + "/data.sql");
            return readSQLsFrom(ins);
        } catch (Exception e) {
            WLog.printStackTrace(e);
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        WLog.v(TAG, "onCreate");
        final SQLiteInterceptor interceptor = mInterceptor;
        if (interceptor != null) {
            interceptor.beforeCreate(db);
        }

        do {

            List<String> sqls = getCreateSQLs();
            if (Utils.isEmpty(sqls)) {
                WLog.w(TAG, "cannot get the db create sqls");
                break;
            }

            for (String sql : sqls) {
                db.execSQL(sql);
            }

            sqls = getDataSQLs();
            if (Utils.isEmpty(sqls)) {
                WLog.w(TAG, "cannot get the db data sqls");
                break;
            }

            for (String sql : sqls) {
                db.execSQL(sql);
            }

        } while(false);

        if (interceptor != null) {
            interceptor.afterCreated(db);
        }
    }

    protected String getSQLAssertPath() {
        return "wink_db_sqls";
    }

    private List<String> getUpgradeSQL(int oldVersion, int newVersion) {
        final Context context = mContext;
        AssetManager am = context.getAssets();
        InputStream ins = null;
        String upgradeSQLFile = String.format(Locale.US, "%s/db_upgrade_%d_%d.sql",
                getSQLAssertPath(),
                oldVersion, newVersion);

        try {
            ins = am.open(upgradeSQLFile);
            return readSQLsFrom(ins);
        } catch (Exception e) {
            WLog.printStackTrace(e);
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        WLog.v(TAG, "onUpgrade");
        final SQLiteInterceptor interceptor = mInterceptor;
        if (interceptor != null) {
            interceptor.beforeUpgrade(db, oldVersion, newVersion);
        }

        do {
            List<String> sqls = getUpgradeSQL(oldVersion, newVersion);
            if (Utils.isEmpty(sqls)) {
                WLog.w(TAG, "cannot get the db upgrade sqls, maybe no need upgrade it!");
                break;
            }

            for (String sql : sqls) {
                db.execSQL(sql);
            }
        } while (false);

        if (interceptor != null) {
            interceptor.afterUpgraded(db, oldVersion, newVersion);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        WLog.v(TAG, "onOpen");
        super.onOpen(db);

    }
}
