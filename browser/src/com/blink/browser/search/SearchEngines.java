/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blink.browser.search;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.blink.browser.DatabaseManager;
import com.blink.browser.R;
import com.blink.browser.bean.SearchEngineEntity;
import com.blink.browser.util.Logger;
import com.blink.browser.util.ImageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchEngines {

    private static final String TAG = "SearchEngines";
    private static List<SearchEngineEntity> sSearchEntitiesList;
    private static ArrayList<SearchEngineInfo> sSearchEngineInfos; //数据库中引擎集合
    private static ArrayList<SearchEngineInfo> sXmlDefaultEngineInfos; //xml中引擎集合
    private static HashMap<String, SearchEngineEntity> sSearchEngineMap;
    private static List<SearchEngineEntity> sDatabaseEngines; //图片加载成功后取数据用
    private static SearchEngineInfo sDefaultEngineInfo;
    private static SearchEngines sInstance;
    private Context mContext;
    private static List<IDefaultEngineIconUpdateListener> sIconUpdateListenerList;
    private SearchEngines(Context context) {
        mContext = context.getApplicationContext();
        initSearchEngineInfos();
    }

    public static SearchEngines getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SearchEngines(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * 初始化数据库数据
     */
    private void initSearchEngineInfos() {
        sSearchEngineInfos = new ArrayList<>();
        List<SearchEngineEntity> databaseEngines = getDatabaseEngines();
        if (databaseEngines != null && databaseEngines.size() > 0) {
            //读数据库
            for (SearchEngineEntity temp : databaseEngines) {
                SearchEngineInfo info = new SearchEngineInfo(temp);
                if (temp.getImageIcon() == null && temp.getImageUrl() != null) {
                    Bitmap bitmap = ImageUtils.syncDownloadEngineIcon(temp, new ImageUtils.ImageLoadListener() {
                        @Override
                        public void onLoadSuccess(SearchEngineEntity entity) {
                            updateDatabaseIcon(entity, entity.getImageIcon());
                            addEngineInfo(new SearchEngineInfo(entity));
                            if (entity.is_default == 1 && sIconUpdateListenerList != null) {
                               for (IDefaultEngineIconUpdateListener listener : sIconUpdateListenerList) {
                                   if (listener != null) {
                                       //通知UI更新
                                       listener.updateDefaultEngineIcon();
                                   }
                               }
                            }
                        }
                    });
                    if (bitmap == null && temp.is_default == 1) {
                        addEngineInfo(info);
                    }
                }
                if (temp.getImageIcon() != null) {
                    addEngineInfo(info);
                }
            }
        }
        if (sSearchEngineInfos == null || sSearchEngineInfos.size() <= 0) {
            getXmlDefaultEngineInfos();
        }
    }

    private void addEngineInfo(SearchEngineInfo info) {
        boolean isContains = false;
        for (SearchEngineInfo engineInfo : sSearchEngineInfos) {
            if (info.getName().toLowerCase().contains(engineInfo.getName().toLowerCase())) {
                isContains = true;
            }
        }
        if (!isContains) {
            sSearchEngineInfos.add(info);
        }
    }

    private void getXmlDefaultEngineInfos() {
        if (sXmlDefaultEngineInfos == null) {
            sXmlDefaultEngineInfos = new ArrayList<>();
        }
        sXmlDefaultEngineInfos.clear();
        //读xml
        Resources res = mContext.getResources();
        String[] searchEngines = res.getStringArray(R.array.search_engines);
        for (int i = 0; i < searchEngines.length; i++) {
            String name = searchEngines[i];
            SearchEngineInfo info = new SearchEngineInfo(mContext, name);
            sXmlDefaultEngineInfos.add(info);
        }
    }

    /**
     * 跟新数据库图标
     * @param entity
     * @param bitmap //最新加载到的图标
     */
    private void updateDatabaseIcon(SearchEngineEntity entity, byte[] bitmap) {
        ContentValues values = new ContentValues();
        values.put(SearchEngineEntity.Column.ID, entity.getId());
        values.put(SearchEngineEntity.Column.ENGINE_NAME, entity.getTitle());
        values.put(SearchEngineEntity.Column.ENGINE_ICON, bitmap);
        values.put(SearchEngineEntity.Column.ENGINE_QUERY_Url, entity.getEngine_url());
        values.put(SearchEngineEntity.Column.ENGINE_ORDER, entity.getEngine_order());
        values.put(SearchEngineEntity.Column.CREATE_TIME, entity.getCreate_time());
        values.put(SearchEngineEntity.Column.IS_DEFAULT, entity.is_default);
        values.put(SearchEngineEntity.Column.IMAGE_URL, entity.getImageUrl());
        values.put(SearchEngineEntity.Column.ENCODING, entity.getEncoding());

        DatabaseManager.getInstance().updateById(SearchEngineEntity.class, values, entity.getId());
    }

    /**
     * 转换searchEngineEntity 到searchEngineInfo
     * @return
     */
    public List<SearchEngineInfo> getSearchEngineInfos() {
        if (sSearchEntitiesList != null && sSearchEntitiesList.size() > 0) {
            sSearchEngineInfos.clear();
            for (SearchEngineEntity temp : sSearchEntitiesList) {
                sSearchEngineInfos.add(new SearchEngineInfo(temp));
            }
            return sSearchEngineInfos;
        } else if (sSearchEngineInfos != null && sSearchEngineInfos.size() > 0){
            return sSearchEngineInfos;
        } else {
            if (sXmlDefaultEngineInfos == null || (sXmlDefaultEngineInfos != null && sXmlDefaultEngineInfos.size() < 0)) {
                getXmlDefaultEngineInfos();
            }
            return sXmlDefaultEngineInfos;
        }
    }

    /**
     * 获取默认引擎
     * @param context
     * @return
     */
    public SearchEngine getDefaultSearchEngine(Context context) {
        return DefaultSearchEngine.create(context);
    }

    public SearchEngine get(Context context, String name) {
        SearchEngineInfo searchEngineInfo = null;
        if (sDefaultEngineInfo != null && sDefaultEngineInfo.getName()!= null && sDefaultEngineInfo.getName().equals(name)) {
            return new OpenSearchSearchEngine(context, sDefaultEngineInfo);
        }
        if (sSearchEngineMap != null && sSearchEngineMap.containsKey(name)) {
            searchEngineInfo = new SearchEngineInfo(sSearchEngineMap.get(name));
        } else {
            // TODO: cache
            SearchEngine defaultSearchEngine = getDefaultSearchEngine(context);
            if (TextUtils.isEmpty(name)
                    || (defaultSearchEngine != null && name.equals(defaultSearchEngine.getName()))) {
                return defaultSearchEngine;
            }
            searchEngineInfo = getSearchEngineInfo(context, name);
            if (searchEngineInfo == null) return defaultSearchEngine;
        }
        return new OpenSearchSearchEngine(context, searchEngineInfo);
    }

    /**
     * 根据名字获取对应的引擎数据
     * @param context
     * @param name
     * @return
     */
    public SearchEngineInfo getSearchEngineInfo(Context context, String name) {
        if (sSearchEngineMap != null && sSearchEngineMap.containsKey(name)) {
            return new SearchEngineInfo(sSearchEngineMap.get(name));
        }
        if (sDefaultEngineInfo != null && sDefaultEngineInfo.getName()!= null && sDefaultEngineInfo.getName().equals(name)) {
            return sDefaultEngineInfo;
        }
        try {
            return new SearchEngineInfo(context, name);
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Cannot load search engine " + name, exception);
            return null;
        }
    }

    /**
     * 网络获取数据
     * @param list
     */
    public void setSearchEngineEntity( List<SearchEngineEntity> list) {
        if (list == null || list.size() <= 0) {
            return;
        }
        if (sSearchEntitiesList == null) {
            sSearchEntitiesList = new ArrayList<>();
        }
        sSearchEntitiesList.clear();
        for (SearchEngineEntity engineEntity : list) {
            if (engineEntity.getImageIcon() != null) {
                sSearchEntitiesList.add(engineEntity);
            }
        }
        if (sSearchEngineMap == null) {
            sSearchEngineMap = new HashMap<>();
        }
        sSearchEngineMap.clear();
        for (SearchEngineEntity temp : sSearchEntitiesList) {
          sSearchEngineMap.put(temp.getTitle(), temp);
        }
    }

    /**
     * 数据库获取数据
     * @return
     */
    public List<SearchEngineEntity> getDatabaseEngines() {
        List<SearchEngineEntity> searchEngineEntityList = DatabaseManager.getInstance().findByArgs(SearchEngineEntity.class, null,
                null, null, null, SearchEngineEntity.Column.ENGINE_ORDER + " desc");
        if (sSearchEngineMap == null) {
            sSearchEngineMap = new HashMap<>();
        }
        if (searchEngineEntityList == null || searchEngineEntityList.size() <= 0) {
            return null;
        }
        for (SearchEngineEntity temp : searchEngineEntityList) {
            sSearchEngineMap.put(temp.getTitle(), temp);
        }
        Logger.debug(TAG," get SearchEngineEntity from db  "+searchEngineEntityList.size());
        return searchEngineEntityList;
    }

    public HashMap<String, SearchEngineEntity> getSearchEngineMap() {
        return sSearchEngineMap;
    }

    /**
     * 默认引擎
     * @param defaultEngines
     */
    public void setDefaultEngines(SearchEngineEntity defaultEngines) {
        sDefaultEngineInfo = new SearchEngineInfo(defaultEngines);
    }

    /**
     * 获取默认引擎
     * @return
     */
    public SearchEngineInfo getDefaultEngineInfo() {
        return sDefaultEngineInfo;
    }

    public interface IDefaultEngineIconUpdateListener {
        void updateDefaultEngineIcon();
    }
    public static void registerIconUpdateListener(IDefaultEngineIconUpdateListener listener) {
        if (sIconUpdateListenerList == null || listener == null) {
            sIconUpdateListenerList = new ArrayList<>();
        }
        if (!sIconUpdateListenerList.contains(listener)) {
            sIconUpdateListenerList.add(listener);
        }
    }

    /**
     * 记得反注册，不然会泄露
     * @param listener
     */
    public static void unregisterIconUpdateListener(IDefaultEngineIconUpdateListener listener) {
        if (sIconUpdateListenerList == null || listener == null) {
            return;
        }
        if (sIconUpdateListenerList.contains(listener)) {
            sIconUpdateListenerList.remove(listener);
        }
    }

}
