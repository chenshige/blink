// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.analytics;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.blink.browser.Browser;
import com.blink.browser.util.DeviceInfoUtils;

import java.util.HashMap;
import java.util.Map;


public class BrowserAnalytics {
    private static UmengAnalytics mUmengAnalytics = null;

    public static class Event {

        public static final String SELECT_CONTENT = "select_content";
        public static final String FUNCTION_EVENT = "function_event";
        public static final String DOWNLOAD_EVENTS = "download_events";
        public static final String START_EVENTS = "start_events";

        public static final String HOMEPAGE_EVENTS = "Homepage_events";
        public static final String SEARCH_EVENTS = "Search_events";
        public static final String INPUTASSIS_EVENTS = "inputassis_events";
        public static final String SEARCHENGINESWITCH_EVENTS = "searchengineswitch_events";
        public static final String QUICKLINK_EVENTS = "quicklink_events";
        public static final String QLMORE_EVENTS = "QLmore_events";
        public static final String PAGE_EVENTS = "page_events";
        public static final String PICTURE_EVENTS = "picture_events";
        public static final String LINKS_EVENTS = "links_events";
        public static final String TEXT_EVENTS = "text_events";
        public static final String VIDEO_EVENTS = "video_events";
        public static final String SAVEIMAGE_EVENTS = "saveimage_events";
        public static final String SETWALLPAPER_EVENTS = "setwallpaper_events";
        public static final String SHAREPICTURE_EVENTS = "sharepicture_events";
        public static final String SHARELINKS_EVENTS = "sharelinks_events";
        public static final String SHARETEXT_EVENTS = "sharetext_events";
        public static final String SHAREVIDEO_EVENTS = "sharevideo_events";
        public static final String TOOLS_EVENTS = "Tools_events";
        public static final String WINDOWS_EVENTS = "windows_events";
        public static final String MENU_EVENTS = "menu_events";
        public static final String INCOGNITO_EVENTS = "Incognito_events";
        public static final String HISTORY_EVENTS = "history_events";
        public static final String BOOKMARK_EVENTS = "bookmark_events";
        public static final String HISTORYCLICK_EVENTS = "historyclick_events";
        public static final String BOOKMARKCLICK_EVENTS = "bookmarkclick_events";
        public static final String HISTORYDELETE_EVENTS = "historydelete_events";
        public static final String BOOKMARKMENU_EVENTS = "bookmarkmenu_events";
        public static final String SWITCH_EVENTS = "switch_events";
        public static final String TOOLBOX_EVENTS = "Toolbox_events";
        public static final String SAVEDPAGE_EVENTS = "savedpage_events";
        public static final String SAVEDPAGECLICK_EVENTS = "savedpageclick_events";
        public static final String SAVEDPAGEDELETE_EVENTS = "savedpagedelete_events";
        public static final String SETTING_EVENT = "setting_events";
        public static final String ADBLOCK_EVENTS = "adblock_events";
        public static final String CONFIRMEXIT_EVENTS = "confirmexit_events";
        public static final String STATUSDISPLAYED_EVENTS = "statusdisplayed_events";
        public static final String TEXTSIZE_EVENTS = "textsize_events";
        public static final String PRIVACYSECURITY_EVENTS = "privacysecurity_events";
        public static final String CLEARDATA_EVENTS = "cleardata_events";
        public static final String ADVANCED_EVENTS = "advanced_events";
        public static final String DEFAULTBROWSER_EVENTS = "defaultbrowser_events";
        public static final String FEEDBACK_EVENTS = "feedback_events";
        public static final String ABOUT_EVENTS = "about_events";
        public static final String FIVESTAR_EVENTS = "fivestar_events"; //五星弹框
        public static final String UPDATEAPK_EVENTS = "updateapk_events";//版本更新弹框
        public static final String UPDATEAPKMUST_EVENTS = "updateapkmust_events";//版本更新弹框（必须更新）
        public static final String DOWNLOADING_EVENTS = "downloading_events";
        public static final String DOWNLOADED_EVENTS = "downloaded_events";
        public static final String DOWNLOADEDMENU_EVENTS = "downloadedmenu_events";
        public static final String DOWNLOADFAIL_EVENTS = "downloadfail_events";
        public static final String RETRY_EVENTS = "retry_events";
        public static final String DOWNLOADINGDELETE_EVENTS = "downloadingdelete_events";
        public static final String DOWNLOADEDCLICK_EVENTS = "downloadedclick_events";
        public static final String DOWNLOADEDSHARE_EVENTS = "downloadedshare_events";
        public static final String DOWNLOADEDDELETE_EVENTS = "downloadeddelete_events";
        public static final String NOTIFICATIONSEARCH_EVENTS = "notificationsearch_events";
        public static final String NETWORKSWITCH_EVENTS = "networkswitch_events";
        public static final String NOTIFCLOSEINCOGNITO_EVENTS = "notifcloseIncognito_events";
        public static final String VISITURL_EVENTS = "visitURL_events";

        protected Event() {
        }
    }

    public static class Param {
        public static final String CONTENT_TYPE = "content_type"; // Type of content selected (String).
        public static final String ITEM_ID = "item_id"; // Item ID (String).

        public static final String VALUE = "value"; // A context-specific numeric value which is accumulated
        public static final String TITLE = "title";
        public static final String URL = "url";
        public static final String EVENT = "et";
        public static final String EVENT_TIME = "ett";
        public static final String EVENT_NAME = "en";
        public static final String EVENT_PROP = "prop";
        public static final String EVENT_RESULT = "rlt";
        public static final String EVENT_FILTER = "fir";
        public static final String EVENT_INFO = "io";
        public static final String RESULT_NAME = "nae";
        public static final String RESULT_VALUE = "vae";
        public static final String RESULT_VALUE_TYPE = "vpe";

        protected Param() {
        }
    }

    public static void createAnalytics(Context context) {
        //TODO: If Debug build, disable analytics.

        try {
            if (mUmengAnalytics == null) mUmengAnalytics = new UmengAnalytics(context);
            if (!mUmengAnalytics.isExist()) {
                mUmengAnalytics = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void onResume(Context context) {
        try {
            if (mUmengAnalytics != null) mUmengAnalytics.onResume(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onPause(Context context) {
        try {
            if (mUmengAnalytics != null) mUmengAnalytics.onPause(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackEvent(String content_type, String key) {
        trackEvent(content_type, key, null);
    }

    public static void trackEvent(String event, String key, String value) {
        if (TextUtils.isEmpty(event)) return;
        Map<String, String> params = new HashMap<>();
        if (!TextUtils.isEmpty(key)) params.put(Param.ITEM_ID, key);
        if (!TextUtils.isEmpty(value)) params.put(Param.VALUE, key + "#" + value + "#" + DeviceInfoUtils.getCountry());
        trackEvent(event, params);
    }

    public static void trackEvent(String event, Map<String, String> params) {
        if (TextUtils.isEmpty(event) || params == null || params.isEmpty()) return;

        try {
            if (mUmengAnalytics != null) {
                mUmengAnalytics.logEvent(Browser.getInstance().getApplicationContext(), event, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
