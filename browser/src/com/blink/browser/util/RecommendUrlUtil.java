package com.blink.browser.util;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Patterns;

import com.blink.browser.DatabaseManager;
import com.blink.browser.R;
import com.blink.browser.bean.RecommendUrlEntity;
import com.blink.browser.database.DataController;
import com.blink.browser.database.SqlBuild;
import com.blink.browser.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_RECOMMEND_WEBSITE;

public class RecommendUrlUtil {
    private static final String TAG = "RecommendUrlUtil";
    private static AtomicBoolean sFlag = new AtomicBoolean(false);

    public static synchronized List<RecommendUrlEntity> getCommonUrlInfos(Context context) {
        if (sFlag.get()) return null;
        sFlag.set(true);

        List<RecommendUrlEntity> recommends = DatabaseManager.getInstance().findByArgs(RecommendUrlEntity.class,
                RecommendUrlEntity.Column.WEIGHT + ">=? OR " + RecommendUrlEntity.Column.LANGUAGE + " IS NULL",
                new String[]{"0"}, null, null, "weight desc limit 0,15");
        Logger.debug(TAG, " get recommends from db  " + recommends.size());
        sFlag.set(false);
        return recommends;
    }

    public static void addContentObserver(DatabaseManager.DataChangeListener listener) {
        DatabaseManager.getInstance().registerListener(RecommendUrlEntity.class, listener);
    }

    public static synchronized List<RecommendUrlEntity> getLocalRecommendInfos() {
        if (sFlag.get()) return null;
        sFlag.set(true);
        final List<RecommendUrlEntity> recommends = new ArrayList<RecommendUrlEntity>();
        DatabaseManager.getInstance().findBySql(new DataController.DataHandler() {
            @Override
            public void handler(Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        RecommendUrlEntity entity = new RecommendUrlEntity();
                        entity.setId(cursor.getLong(cursor.getColumnIndex(RecommendUrlEntity.Column.ID)));
                        entity.setDisplayName(cursor.getString(cursor.getColumnIndex(RecommendUrlEntity.Column.DISPLAY_NAME)));
                        entity.setImageUrl(cursor.getString(cursor.getColumnIndex(RecommendUrlEntity.Column.IMAGE_URL)));
//                        entity.setSid(cursor.getInt(cursor.getColumnIndex(RecommendUrlEntity.Column.SID)));
                        entity.setWeight(cursor.getInt(cursor.getColumnIndex(RecommendUrlEntity.Column.WEIGHT)));
                        entity.setOrd(cursor.getInt(cursor.getColumnIndex(RecommendUrlEntity.Column.ORD)));
                        entity.setUrl(cursor.getString(cursor.getColumnIndex(RecommendUrlEntity.Column.URL)));
                        entity.setImageIcon(cursor.getBlob(cursor.getColumnIndex(RecommendUrlEntity.Column.IMAGE_ICON)));
                        recommends.add(entity);
                    } while (cursor.moveToNext());
                }
            }
        }, SqlBuild.RECOMMEND_URL_SQL, null);

        if (recommends != null && recommends.size() > 0) {
            Logger.debug(TAG, " get recommends from db  " + recommends.size());
            Collections.sort(recommends, new Comparator<RecommendUrlEntity>() {
                @Override
                public int compare(RecommendUrlEntity lhs, RecommendUrlEntity rhs) {
                    return rhs.getOrd() - lhs.getOrd();
                }
            });
        }

        sFlag.set(false);
        return recommends;
    }

    public static List<RecommendUrlEntity> getMoreRecommendUrl(Context context) {
        String language = DeviceInfoUtils.getDefaultLanguage();
        List<RecommendUrlEntity> recommends = DatabaseManager.getInstance().findByArgs(RecommendUrlEntity.class,
                RecommendUrlEntity.Column.WEIGHT + " >= ?",
                new String[]{"0"}, null, null, RecommendUrlEntity.Column.WEIGHT + " desc limit 10," + Integer.MAX_VALUE);
        Logger.debug(TAG, " get more recommends from db  " + recommends.size());
        if (recommends.size() > 0) {
            Collections.sort(recommends, new Comparator<RecommendUrlEntity>() {
                @Override
                public int compare(RecommendUrlEntity lhs, RecommendUrlEntity rhs) {
                    return rhs.getOrd() - lhs.getOrd();
                }
            });
            recommends.get(0).optSupportStatus = RecommendUrlEntity.OPT_SUPPORT_STATUS_UNBOTH;
        }

        return recommends;
    }

    /**
     * 根据网址生成简称，例如www.baidu.com生成ba，如果无法生成则返回Na。
     * 规则是通过 Url提取主机名，然后取最后一个不是域名的字段的前两个字母作为简称。例如www.hbgz.com.cn，应该是Hb,
     * blog.edu.cn应该是Bl，www.blog.edu.cn应该是Bl，www.baidu.com应该是Ba，www.edu.cn应该是Ww
     * 如果是IP地址那么显示IP
     *
     * @param url 用来生成简称的网址
     * @return 返回对应的网址
     */
    public static String getWebSimpleNameByUrl(String url) {
        String webName = getWebNameByUrl(url);
        if (webName.length() == 0) {
            webName = "Na";
        }
        String tk1 = webName.substring(0, 1);
        String tk2 = tk1;
        if (webName.length() > 2) {
            tk2 = webName.substring(1, 2);
        }
        return tk1.toUpperCase() + tk2;
    }

    public static String getWebNameByUrl(String url) {
        String hostName;
        try {
            WebAddress webAddress = new WebAddress(url);
            hostName = webAddress.getHost();
        } catch (Exception e) {
            hostName = "Na.com";
        }

        //IP地址直接显示IP
        if (Patterns.IP_ADDRESS.matcher(hostName).matches()) {
            return "IP";
        }

        if (TextUtils.isEmpty(hostName))
            hostName = "Na.com";
        String[] splitUrl = hostName.split("\\.");
        int strIdx = splitUrl.length;
        String splitStr = "Na";
        //从后向前选择最后一个不是顶级域名的字段
        while (strIdx > 0) {
            strIdx--;
            splitStr = splitUrl[strIdx];
            if (!Patterns.TOP_LEVEL_DOMAIN.matcher(splitStr).matches()) {
                break;
            }
        }
        return splitStr;
    }

    public static void resetDbForChangeLanguage(Context context) {
        Resources res = context.getResources();
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.deleteByWhere(RecommendUrlEntity.class, "weight < 0", null);
        String[] recommendWebsites = res.getStringArray(R.array.recommend_websites);
        int size = recommendWebsites.length / 3;
        for (int i = 0; i < size; i++) {
            RecommendUrlEntity entity = new RecommendUrlEntity();
            entity.setDisplayName(recommendWebsites[3*i]);
            entity.setUrl(recommendWebsites[3*i+1]);
            entity.setImageUrl(recommendWebsites[3*i+2]);
            entity.setWeight(WEIGHT_RECOMMEND_WEBSITE);
            entity.setOrd(size - i);
            dbManager.insert(entity);
        }
    }

}
