/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.blink.browser;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.RemoteViews;

import com.blink.browser.adblock.AdBlock;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.bean.InputUrlEntity;
import com.blink.browser.homepages.HomeProvider;
import com.blink.browser.provider.BrowserProvider;
import com.blink.browser.reflection.ActivityManager;
import com.blink.browser.search.SearchEngine;
import com.blink.browser.search.SearchEngines;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.ChannelUtil;
import com.blink.browser.util.DeviceInfoUtils;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.Logger;
import com.wcc.wink.Wink;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class for managing settings
 */
public class BrowserSettings implements OnSharedPreferenceChangeListener,
        PreferenceKeys {

    static class CookieManagerProxy {
        public CookieManagerProxy() {
            tasks = new CopyOnWriteArrayList<>();
            BackgroundHandler.execute(new Runnable() {
                @Override
                public void run() {
                    mInstance = CookieManager.getInstance();
                    for (Runnable task : tasks) {
                        BackgroundHandler.getMainHandler().post(task);
                    }
                    tasks.clear();
                    tasks = null;
                }
            });
        }

        private CookieManager mInstance;
        private CopyOnWriteArrayList<Runnable> tasks;

        public void setAcceptCookie(final boolean accept) {
            if (mInstance != null) {
                mInstance.setAcceptCookie(accept);
            } else {
                tasks.add(new Runnable() {
                    @Override
                    public void run() {
                        mInstance.setAcceptCookie(accept);
                    }
                });
            }
        }

        public void removeAllCookie() {
            if (mInstance != null) {
                mInstance.removeAllCookie();
            } else {
                tasks.add(new Runnable() {
                    @Override
                    public void run() {
                        mInstance.removeAllCookie();
                    }
                });
            }
        }


    }

    // TODO: Do something with this UserAgent stuff
    private static final String COMPUTER_USERAGENT = "Mozilla/5.0 (X11; " +
            "Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) ";

    private static final String IPHONE_USERAGENT = "Mozilla/5.0 (iPhone; U; " +
            "CPU iPhone OS 5_1_1 like Mac OS X; en-us) AppleWebKit/534.46 " +
            "(KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3";

    private static final String IPAD_USERAGENT = "Mozilla/5.0 (iPad; U; " +
            "CPU OS 6_0_1 like Mac OS X) AppleWebKit/536.26 " +
            "(KHTML, like Gecko) Version/6.0 Mobile/10A523 Safari/8536.25";

    private static final String ANDROID_USERAGENT = "Mozilla/5.0 (Linux; U; " +
            "Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 " +
            "(KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    public static final String USER_AGENTS[] = {
            ANDROID_USERAGENT,
            IPHONE_USERAGENT,
            IPAD_USERAGENT,
            COMPUTER_USERAGENT
    };

    private static final String BLINK_BROWSER_USERAGENT_SUFFIX = " BlinkBrowser/";
    private static final String PREVIOUS_VERSION = "4.0.4";
    private String CHROME_VERSION = "";
    private String BASE_USERAGENT = "Mozilla/5.0 (Linux; U; Android %s) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Version/4.0 ";

    // The minimum min font size
    // Aka, the lower bounds for the min font size range
    // which is 1:5..24
    private static final int MIN_FONT_SIZE_OFFSET = 5;
    // The initial value in the text zoom range
    // This is what represents 100% in the SeekBarPreference range
    private static final int TEXT_ZOOM_START_VAL = 10;
    // The size of a single step in the text zoom range, in percent
    private static final int TEXT_ZOOM_STEP = 5;
    // The initial value in the double tap zoom range
    // This is what represents 100% in the SeekBarPreference range
    private static final int DOUBLE_TAP_ZOOM_START_VAL = 5;
    // The size of a single step in the double tap zoom range, in percent
    private static final int DOUBLE_TAP_ZOOM_STEP = 5;

    private static BrowserSettings sInstance;

    private Context mContext;
    private SharedPreferences mPrefs;
    private LinkedList<WeakReference<WebSettings>> mManagedSettings;
    private Controller mController;
    private WebStorageSizeManager mWebStorageSizeManager;
    private WeakHashMap<WebSettings, String> mCustomUserAgents;
    private static boolean sInitialized = false;
    private boolean mNeedsSharedSync = true;
    private float mFontSizeMult = 1.0f;
    private CookieManagerProxy mCookieManagerProxy = new CookieManagerProxy();

    // Current state of network-dependent settings
    private boolean mLinkPrefetchAllowed = true;

    // Cached values
    private int mPageCacheCapacity = 1;
    private String mAppCachePath;

    // Cached settings
    private SearchEngine mSearchEngine;

    private static String sFactoryResetUrl;
    private String mHomePageUrl;
    private final static int NOTIFY_ID = 1000;

    public static void initialize(final Context context) {
        sInstance = new BrowserSettings(context);
    }

    public static BrowserSettings getInstance() {
        return sInstance;
    }

    private BrowserSettings(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mManagedSettings = new LinkedList<WeakReference<WebSettings>>();
        mCustomUserAgents = new WeakHashMap<WebSettings, String>();
        BackgroundHandler.execute(mSetup);
        setTempExitFullscreen(false);
    }

    public void setController(Controller controller) {
        mController = controller;
        if (sInitialized) {
            syncSharedSettings();
        }
    }

    public void startManagingSettings(WebSettings settings) {

        if (mNeedsSharedSync) {
            syncSharedSettings();
        }

        synchronized (mManagedSettings) {
            syncStaticSettings(settings);
            syncSetting(settings);
            mManagedSettings.add(new WeakReference<>(settings));
        }
    }

    public void stopManagingSettings(WebSettings settings) {
        Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
        while (iter.hasNext()) {
            WeakReference<WebSettings> ref = iter.next();
            if (ref.get() == settings) {
                iter.remove();
                return;
            }
        }
    }

    private Runnable mSetup = new Runnable() {

        @Override
        public void run() {
            if (getNotificationToolShow()) {
                showNotification();
            }
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            mFontSizeMult = metrics.scaledDensity / metrics.density;
            // the cost of one cached page is ~3M (measured using nytimes.com). For
            // low end devices, we only cache one page. For high end devices, we try
            // to cache more pages, currently choose 5.
            if (ActivityManager.staticGetMemoryClass() > 16) {
                mPageCacheCapacity = 5;
            }
            mWebStorageSizeManager = new WebStorageSizeManager(mContext,
                    new WebStorageSizeManager.StatFsDiskInfo(getAppCachePath()),
                    new WebStorageSizeManager.WebKitAppCacheInfo(getAppCachePath()));
            // Workaround b/5254577
            mPrefs.registerOnSharedPreferenceChangeListener(BrowserSettings.this);
            if (Build.VERSION.CODENAME.equals("REL")) {
                // This is a release build, always startup with debug disabled
                setDebugEnabled(false);
            }
            if (mPrefs.contains(PREF_TEXT_SIZE)) {
                /*
                 * Update from TextSize enum to zoom percent
                 * SMALLEST is 50%
                 * SMALLER is 75%
                 * NORMAL is 100%
                 * LARGER is 150%
                 * LARGEST is 200%
                 */
                switch (getTextSize()) {
                    case SMALLEST:
                        setTextZoom(50);
                        break;
                    case SMALLER:
                        setTextZoom(75);
                        break;
                    case LARGER:
                        setTextZoom(150);
                        break;
                    case LARGEST:
                        setTextZoom(200);
                        break;
                }
                mPrefs.edit().remove(PREF_TEXT_SIZE).apply();
            }

            sFactoryResetUrl = mContext.getResources().getString(R.string.homepage_base);
            if (sFactoryResetUrl.indexOf("{CID}") != -1) {
                sFactoryResetUrl = sFactoryResetUrl.replace("{CID}",
                        BrowserProvider.getClientId(mContext.getContentResolver()));
            }

            synchronized (BrowserSettings.class) {
                sInitialized = true;
                BrowserSettings.class.notifyAll();
            }
        }
    };

    private static void requireInitialization() {
        synchronized (BrowserSettings.class) {
            while (!sInitialized) {
                try {
                    BrowserSettings.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Syncs all the settings that have a Preference UI
     */
    private void syncSetting(WebSettings settings) {
        settings.setGeolocationEnabled(enableGeolocation());
        settings.setJavaScriptEnabled(enableJavascript());
        settings.setLightTouchEnabled(enableLightTouch());
        //settings.setNavDump(enableNavDump());
        settings.setDefaultTextEncodingName(getDefaultTextEncoding());
        settings.setDefaultZoom(ZoomDensity.MEDIUM);
        settings.setMinimumFontSize(getMinimumFontSize());
        settings.setMinimumLogicalFontSize(getMinimumFontSize());
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.JELLY_BEAN) {
            settings.setTextZoom(getTextZoom());
        }
        try {
            settings.setLayoutAlgorithm(getLayoutAlgorithm());
        } catch (Exception e) {
            // This shouldn't be necessary, but there are a number
            // of KitKat devices that crash trying to set this
            Logger.error("BrowserSettings", "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING");
        }
        settings.setJavaScriptCanOpenWindowsAutomatically(!blockPopupWindows());
        settings.setLoadsImagesAutomatically(loadImages());
        settings.setSavePassword(rememberPasswords());
        settings.setSaveFormData(saveFormdata());
        settings.setUseWideViewPort(isWideViewport());
        settings.setLoadWithOverviewMode(true);

        setChromeVersion(settings);
        String ua = mCustomUserAgents.get(settings);
        if (ua != null) {
            settings.setUserAgentString(containsBlinkBrowserUASuffix(ua) ? ua : ua +
                    getBlinkBrowserUseragentSuffix());
        } else if (getUserAgent() == 0) {
            String userAgent = getDefaultUserAgent();
            settings.setUserAgentString(containsBlinkBrowserUASuffix(userAgent)
                    ? userAgent : userAgent + CHROME_VERSION + getBlinkBrowserUseragentSuffix());
        } else if (getUserAgent() == 3) {
            settings.setUserAgentString(COMPUTER_USERAGENT + CHROME_VERSION + getBlinkBrowserUseragentSuffix());
        } else {
            settings.setUserAgentString(containsBlinkBrowserUASuffix(USER_AGENTS[getUserAgent()])
                    ? USER_AGENTS[getUserAgent()] : USER_AGENTS[getUserAgent()]
                    + getBlinkBrowserUseragentSuffix());
        }
    }

    // Get default UserAgent.
    private void setChromeVersion(WebSettings settings) {
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            if (settings != null && TextUtils.isEmpty(CHROME_VERSION)) {
                String defaultUA = settings.getDefaultUserAgent(mContext);
                if (!TextUtils.isEmpty(defaultUA)) {
                    int index = defaultUA.indexOf("Chrome/");
                    if (index < 0) {
                        index = defaultUA.indexOf("WebKit/");
                    }
                    if (index > 0) {
                        CHROME_VERSION = defaultUA.substring(index);
                    }
                }
            }
        }
    }

    private boolean containsBlinkBrowserUASuffix(String ua) {
        return (ua != null && ua.contains(BLINK_BROWSER_USERAGENT_SUFFIX));
    }

    private String getBlinkBrowserUseragentSuffix() {
        return BLINK_BROWSER_USERAGENT_SUFFIX + DeviceInfoUtils.getAppVersionName(mContext);
    }

    private String getDefaultUserAgent() {
        Locale locale = Locale.getDefault();
        StringBuffer buffer = new StringBuffer();
        // Add version
        final String version = Build.VERSION.RELEASE;
        if (version.length() > 0) {
            if (Character.isDigit(version.charAt(0))) {
                // Release is a version, eg "3.1"
                buffer.append(version);
            } else {
                // Release is a codename, eg "Honeycomb"
                // In this case, use the previous release's version
                buffer.append(PREVIOUS_VERSION);
            }
        } else {
            // default to "1.0"
            buffer.append("1.0");
        }
        buffer.append("; ");
        final String language = locale.getLanguage();
        if (language != null) {
            buffer.append(convertObsoleteLanguageCodeToNew(language));
            final String country = locale.getCountry();
            if (country != null) {
                buffer.append("-");
                buffer.append(country.toLowerCase());
            }
        } else {
            // default to "en"
            buffer.append("en");
        }
        buffer.append(";");
        // add the model for the release build
        if ("REL".equals(Build.VERSION.CODENAME)) {
            final String model = Build.MODEL;
            if (model.length() > 0) {
                buffer.append(" ");
                buffer.append(model);
            }
        }
        final String id = Build.ID;
        if (id.length() > 0) {
            buffer.append(" Build/");
            buffer.append(id);
        }
        return String.format(BASE_USERAGENT, buffer);
    }

    /**
     * Convert obsolete language codes, including Hebrew/Indonesian/Yiddish,
     * to new standard.
     */
    private static String convertObsoleteLanguageCodeToNew(String langCode) {
        if (langCode == null) {
            return null;
        }
        if ("iw".equals(langCode)) {
            // Hebrew
            return "he";
        } else if ("in".equals(langCode)) {
            // Indonesian
            return "id";
        } else if ("ji".equals(langCode)) {
            // Yiddish
            return "yi";
        }
        return langCode;
    }

    /**
     * Syncs all the settings that have no UI
     * These cannot change, so we only need to set them once per WebSettings
     */
    private void syncStaticSettings(WebSettings settings) {
                settings.setDefaultFontSize(16);
                settings.setDefaultFixedFontSize(13);

                // WebView inside Browser doesn't want initial focus to be set.
                settings.setNeedInitialFocus(false);
                // Browser doesn't supports multiple windows
                settings.setSupportMultipleWindows(true);
                if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.LOLLIPOP) {
                    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                }
                // enable smooth transition for better performance during panning or
                // zooming
                settings.setEnableSmoothTransition(true);
                // enable content url access
                settings.setAllowContentAccess(true);

                // HTML5 API flags
                settings.setAppCacheEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setDomStorageEnabled(true);

                // HTML5 configuration parametersettings.
                settings.setAppCacheMaxSize(getWebStorageSizeManager().getAppCacheMaxSize());
                settings.setAppCachePath(getAppCachePath());
                settings.setDatabasePath(mContext.getDir("databases", 0).getPath());
                settings.setGeolocationDatabasePath(mContext.getDir("geolocation", 0).getPath());
                // origin policy for file access
                if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.JELLY_BEAN) {
                    settings.setAllowUniversalAccessFromFileURLs(false);
                    settings.setAllowFileAccessFromFileURLs(false);
                }
    }

    private void syncSharedSettings() {
        mNeedsSharedSync = false;
        mCookieManagerProxy.setAcceptCookie(acceptCookies());
        if (mController != null) {
            for (Tab tab : mController.getTabs()) {
                tab.setAcceptThirdPartyCookies(acceptCookies());
            }
            mController.setShouldShowErrorConsole(enableJavascriptConsole());
        }
    }

    private void syncManagedSettings() {
        syncSharedSettings();
        synchronized (mManagedSettings) {
            Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
            while (iter.hasNext()) {
                WeakReference<WebSettings> ref = iter.next();
                WebSettings settings = ref.get();
                if (settings == null) {
                    iter.remove();
                    continue;
                }
                syncSetting(settings);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        prefTrackEvent(key, sharedPreferences);
        syncManagedSettings();
        if (PREF_SEARCH_ENGINE.equals(key)) {
            updateSearchEngine(false);
        } else if (PREF_FULLSCREEN.equals(key)) {
            if (mController != null && mController.getUi() != null) {
//                mController.getUi().onFullscreenChange(useFullscreen());
                setTempExitFullscreen(false);
                mController.setFullscreen(useFullscreen());
            }
        } else if (PREF_ENABLE_QUICK_CONTROLS.equals(key)) {
            if (mController != null && mController.getUi() != null) {
                mController.getUi().setUseQuickControls(sharedPreferences.getBoolean(key, false));
            }
        } else if (PREF_AD_BLOCK.equals(key)) {
            AdBlock.getInstance().setAdBlockEnabled(getAdBlockEnabled());
        } else if (PREF_SHOW_STATUS_BAR.equals(key)) {
            //状态栏显示状态改变
            if (mController != null && mController.getUi() != null) {
                ((BaseUi) mController.getUi()).changeWebViewHeight();
            }
        }
    }

    public static String getFactoryResetHomeUrl(Context context) {
        requireInitialization();
        return sFactoryResetUrl;
    }

    public LayoutAlgorithm getLayoutAlgorithm() {
        LayoutAlgorithm layoutAlgorithm = LayoutAlgorithm.NORMAL;
        LayoutAlgorithm autosize = LayoutAlgorithm.NARROW_COLUMNS;
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            autosize = LayoutAlgorithm.TEXT_AUTOSIZING;
        }

        if (autofitPages() || forceEnableUserScalable()) {
            layoutAlgorithm = autosize;
        }
        return layoutAlgorithm;
    }

    public int getPageCacheCapacity() {
        requireInitialization();
        return mPageCacheCapacity;
    }

    public WebStorageSizeManager getWebStorageSizeManager() {
        requireInitialization();
        return mWebStorageSizeManager;
    }

    private String getAppCachePath() {
        if (mAppCachePath == null) {
            mAppCachePath = mContext.getDir("appcache", 0).getPath();
        }
        return mAppCachePath;
    }

    private void updateSearchEngine(boolean force) {
        String searchEngineName = getSearchEngineName();
        if (force || mSearchEngine == null ||
                !mSearchEngine.getName().equals(searchEngineName)) {
            mSearchEngine = SearchEngines.getInstance(mContext).get(mContext, searchEngineName);
        }
    }

    public SearchEngine getSearchEngine() {
        if (mSearchEngine == null) {
            updateSearchEngine(false);
        }
        return mSearchEngine;
    }

    public boolean isDebugEnabled() {
        return false;
        //requireInitialization();
        //return mPrefs.getBoolean(PREF_DEBUG_MENU, false);
    }

    public void setDebugEnabled(boolean value) {
        Editor edit = mPrefs.edit();
        edit.putBoolean(PREF_DEBUG_MENU, value);
        if (!value) {
            // Reset to "safe" value
            edit.putBoolean(PREF_ENABLE_HARDWARE_ACCEL_SKIA, false);
        }
        edit.apply();
    }

    public void clearCache() {
        if (mController != null) {
            WebView current = mController.getCurrentWebView();
            if (current != null) {
                current.clearCache(true);
            }
        }
    }

    public void clearCookies() {
        mCookieManagerProxy.removeAllCookie();
    }

    public void clearHistory() {
        ContentResolver resolver = mContext.getContentResolver();
        BrowserHelper.clearHistory(resolver);
        BrowserHelper.clearSearches(resolver);
        DatabaseManager.getInstance().deleteAllData(InputUrlEntity.class);
    }

    public void clearFormData() {
        WebViewDatabase.getInstance(mContext).clearFormData();
        if (mController != null) {
            WebView currentTopView = mController.getCurrentTopWebView();
            if (currentTopView != null) {
                currentTopView.clearFormData();
            }
        }
    }

    public void clearPasswords() {
        WebViewDatabase db = WebViewDatabase.getInstance(mContext);
        db.clearUsernamePassword();
        db.clearHttpAuthUsernamePassword();
    }

    public void clearDatabases() {
        WebStorage.getInstance().deleteAllData();
    }

    public void clearLocationAccess() {
        GeolocationPermissions.getInstance().clearAll();
    }

    public void resetDefaultPreferences() {
        mPrefs.edit()
                .clear()
                .apply();
        resetCachedValues();
        syncManagedSettings();
    }

    private void resetCachedValues() {
        updateSearchEngine(false);
    }

    public void toggleDebugSettings() {
        setDebugEnabled(!isDebugEnabled());
    }

    public boolean hasDesktopUseragent(WebView view) {
        return view != null && mCustomUserAgents.get(view) != null;
    }

    public void toggleDesktopUseragent(final WebView view) {
        if (view == null) {
            return;
        }
        WebSettings settings = view.getSettings();
        setChromeVersion(settings);
        if (mCustomUserAgents.get(settings) != null) {
            mCustomUserAgents.remove(settings);
            if (getUserAgent() == 0) {
                String userAgent = settings.getUserAgentString();
                settings.setUserAgentString(containsBlinkBrowserUASuffix(userAgent)
                        ? userAgent : userAgent + CHROME_VERSION + getBlinkBrowserUseragentSuffix());
            } else if (getUserAgent() == 3) {
                settings.setUserAgentString(COMPUTER_USERAGENT + CHROME_VERSION +
                        getBlinkBrowserUseragentSuffix());
            } else {
                settings.setUserAgentString(containsBlinkBrowserUASuffix(USER_AGENTS[getUserAgent()])
                        ? USER_AGENTS[getUserAgent()] : USER_AGENTS[getUserAgent()] +
                        getBlinkBrowserUseragentSuffix());
            }
        } else {
            mCustomUserAgents.put(settings, COMPUTER_USERAGENT);
            settings.setUserAgentString(COMPUTER_USERAGENT + CHROME_VERSION + getBlinkBrowserUseragentSuffix());
        }
    }

    public static int getAdjustedMinimumFontSize(int rawValue) {
        rawValue++; // Preference starts at 0, min font at 1
        if (rawValue > 1) {
            rawValue += (MIN_FONT_SIZE_OFFSET - 2);
        }
        return rawValue;
    }

    public int getAdjustedTextZoom(int rawValue) {
        rawValue = (rawValue - TEXT_ZOOM_START_VAL) * TEXT_ZOOM_STEP;
        return (int) ((rawValue + 100) * mFontSizeMult);
    }

    static int getRawTextZoom(int percent) {
        return (percent - 100) / TEXT_ZOOM_STEP + TEXT_ZOOM_START_VAL;
    }

    public int getAdjustedDoubleTapZoom(int rawValue) {
        rawValue = (rawValue - DOUBLE_TAP_ZOOM_START_VAL) * DOUBLE_TAP_ZOOM_STEP;
        return (int) ((rawValue + 100) * mFontSizeMult);
    }

    static int getRawDoubleTapZoom(int percent) {
        return (percent - 100) / DOUBLE_TAP_ZOOM_STEP + DOUBLE_TAP_ZOOM_START_VAL;
    }

    public SharedPreferences getPreferences() {
        return mPrefs;
    }

    // update connectivity-dependent options
    public void updateConnectionType() {
        syncManagedSettings();
    }

    // -----------------------------
    // getter/setters for accessibility_preferences.xml
    // -----------------------------

    @Deprecated
    private TextSize getTextSize() {
        String textSize = mPrefs.getString(PREF_TEXT_SIZE, "NORMAL");
        return TextSize.valueOf(textSize);
    }

    public int getMinimumFontSize() {
        int minFont = mPrefs.getInt(PREF_MIN_FONT_SIZE, 0);
        return getAdjustedMinimumFontSize(minFont);
    }

    public boolean forceEnableUserScalable() {
        return mPrefs.getBoolean(PREF_FORCE_USERSCALABLE, false);
    }

    public int getTextZoom() {
        requireInitialization();
        int textZoom = mPrefs.getInt(PREF_TEXT_ZOOM, 10);
        // When force enable user scalable, text zoom must be large than 130.
        return forceEnableUserScalable() ? 130 : getAdjustedTextZoom(textZoom);
    }

    public void setTextZoom(int percent) {
        mPrefs.edit().putInt(PREF_TEXT_ZOOM, getRawTextZoom(percent)).apply();
    }

    public int getDoubleTapZoom() {
        requireInitialization();
        int doubleTapZoom = mPrefs.getInt(PREF_DOUBLE_TAP_ZOOM, 5);
        return getAdjustedDoubleTapZoom(doubleTapZoom);
    }

    public void setDoubleTapZoom(int percent) {
        mPrefs.edit().putInt(PREF_DOUBLE_TAP_ZOOM, getRawDoubleTapZoom(percent)).apply();
    }

    // -----------------------------
    // getter/setters for advanced_preferences.xml
    // -----------------------------

    public String getSearchEngineName() {
        String value;
        if (mPrefs.contains(PREF_SEARCH_ENGINE)) {
            value = mPrefs.getString(PREF_SEARCH_ENGINE, mContext.getResources().getString(R.string
                    .default_search_engine_value));
        } else {
            value = mContext.getResources().getString(R.string.default_search_engine_value);
            mPrefs.edit().putString(PREF_SEARCH_ENGINE, value).apply();
        }
        return value;
    }

    public void setSearchEngineName(String value) {
        mPrefs.edit().putString(PREF_SEARCH_ENGINE, value).apply();
        resetCachedValues();
    }

    public void setSearchEngineLoadTime(long value) {
        Editor editor = mPrefs.edit();
        editor.putLong(SEARCH_ENGINE_LOAD_TIME, value);
        editor.commit();
    }

    public long getSearchEngineLoadTime() {
        return mPrefs.getLong(SEARCH_ENGINE_LOAD_TIME, 0);
    }

    public boolean enableJavascript() {
        return mPrefs.getBoolean(PREF_ENABLE_JAVASCRIPT, true);
    }

    public boolean getEnableCookiesIncognito() {
        return mPrefs.getBoolean(PREF_ENABLE_COOKIES_INCOGNITO, true);
    }

    public boolean autofitPages() {
        return mPrefs.getBoolean(PREF_AUTOFIT_PAGES, true);
    }

    public boolean blockPopupWindows() {
        return mPrefs.getBoolean(PREF_BLOCK_POPUP_WINDOWS, true);
    }

    public boolean loadImages() {
        return mPrefs.getBoolean(PREF_LOAD_IMAGES, true);
    }

    public String getDefaultTextEncoding() {
        return mPrefs.getString(PREF_DEFAULT_TEXT_ENCODING, mContext.getResources().getString(R.string
                .pref_default_text_encoding_default));
    }

    public void setDefaultTextEncoding(String value) {
        mPrefs.edit().putString(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING, value).apply();
    }

    // -----------------------------
    // getter/setters for general_preferences.xml
    // -----------------------------

    public String getHomePage() {
        if (TextUtils.isEmpty(mHomePageUrl)) {
            mHomePageUrl = mPrefs.getString(PREF_HOMEPAGE, getFactoryResetHomeUrl(mContext));
        }
        return mHomePageUrl;
    }

    public boolean getHomePageChanged() {
        boolean changed = mPrefs.getBoolean(PREF_HOMEPAGE_CHANGED, false);
        mPrefs.edit().putBoolean(PREF_HOMEPAGE_CHANGED, false).apply();
        return changed;
    }

    public String getPrefHomePage() {
        return mPrefs.getString(PREF_HOMEPAGE, getFactoryResetHomeUrl(mContext));
    }

    public void setHomePage(String value) {
        if (value.equals(mHomePageUrl)) {
            mPrefs.edit().putBoolean(PREF_HOMEPAGE_CHANGED, false).apply();
        } else {
            mPrefs.edit().putBoolean(PREF_HOMEPAGE_CHANGED, true).apply();
        }
        mPrefs.edit().putString(PREF_HOMEPAGE, value).apply();
    }

    public boolean isAutofillEnabled() {
        return mPrefs.getBoolean(PREF_AUTOFILL_ENABLED, true);
    }

    public void setAutofillEnabled(boolean value) {
        mPrefs.edit().putBoolean(PREF_AUTOFILL_ENABLED, value).apply();
    }

    // -----------------------------
    // getter/setters for debug_preferences.xml
    // -----------------------------

    public boolean isHardwareAccelerated() {
        if (!isDebugEnabled()) {
            return true;
        }
        return mPrefs.getBoolean(PREF_ENABLE_HARDWARE_ACCEL, true);
    }

    public boolean isSkiaHardwareAccelerated() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_HARDWARE_ACCEL_SKIA, false);
    }

    public int getUserAgent() {
//        if (!isDebugEnabled()) {
//            return 0;
//        }

        int ua = 0;
        try {
            ua = Integer.parseInt(mPrefs.getString(PREF_USER_AGENT, "0"));
        } catch (NumberFormatException e) {
        }
        return ua;
    }

    public void setUserAgent(String value) {
        mPrefs.edit().putString(PREF_USER_AGENT, value).apply();
    }


    // -----------------------------
    // getter/setters for hidden_debug_preferences.xml
    // -----------------------------

    public boolean enableVisualIndicator() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_VISUAL_INDICATOR, false);
    }

    public boolean enableCpuUploadPath() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_CPU_UPLOAD_PATH, false);
    }

    public boolean enableJavascriptConsole() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_JAVASCRIPT_CONSOLE, true);
    }

    public boolean isWideViewport() {
        if (!isDebugEnabled()) {
            return true;
        }
        return mPrefs.getBoolean(PREF_WIDE_VIEWPORT, true);
    }

    public boolean isNormalLayout() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_NORMAL_LAYOUT, false);
    }

    public boolean isTracing() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_TRACING, false);
    }

    public boolean enableLightTouch() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_LIGHT_TOUCH, false);
    }

    public boolean enableNavDump() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_NAV_DUMP, false);
    }

    public String getJsEngineFlags() {
        if (!isDebugEnabled()) {
            return "";
        }
        return mPrefs.getString(PREF_JS_ENGINE_FLAGS, "");
    }

    // -----------------------------
    // getter/setters for lab_preferences.xml
    // -----------------------------

    public boolean useQuickControls() {
        return mPrefs.getBoolean(PREF_ENABLE_QUICK_CONTROLS, false);
    }

    public boolean useMostVisitedHomepage() {
        return HomeProvider.MOST_VISITED.equals(getHomePage());
    }

    public boolean getAdBlockUpdateEnabled() {
        return mPrefs.getBoolean(PREF_ADBLOCK_UPDATE, true);
    }

    public void setAdBlockUpdateEnabled(boolean enabled) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_ADBLOCK_UPDATE, enabled);
        editor.commit();
    }

    public boolean getEasyListUpdateEnabled() {
        return mPrefs.getBoolean(PREF_EASYLIST_UPDATE, true);
    }

    public void setEasyListUpdateEnabled(boolean enabled) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_EASYLIST_UPDATE, enabled);
        editor.commit();
    }

    public boolean lockToolbar() {
        return mPrefs.getBoolean(PREF_LOCK_TOOLBAR, true);
    }

    public void setLockToolbar(boolean lock) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_LOCK_TOOLBAR, lock);
        editor.commit();
    }

    /**
     * 获取状态栏显示状态
     */
    public boolean showStatusBar() {
        return mPrefs.getBoolean(PREF_SHOW_STATUS_BAR, false);
    }

    public boolean getShowStatusBar() {
        return showStatusBar();//mPrefs.getBoolean(PreferenceKeys.PREF_SHOW_STATUS_BAR, false);
    }

    public void setShowStatusBar(boolean show) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_SHOW_STATUS_BAR, show);
        editor.commit();
    }

    public boolean useFullscreen() {
        return mPrefs.getBoolean(PREF_FULLSCREEN, true);
    }

    /**
     * 判断是否暂时退出全屏状态
     *
     * @return true 为暂时退出全屏状态
     */
    public boolean useTempExitFullscreen() {
        return mPrefs.getBoolean(PREF_TEMP_FULLSCREEN, false);
    }

    public void setFullscreen(boolean isFullScreen) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_FULLSCREEN, isFullScreen);
        editor.commit();
    }

    public void setTempExitFullscreen(boolean isTempFullScreen) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_TEMP_FULLSCREEN, isTempFullScreen);
        editor.commit();
    }


    public boolean isFirstCreateEngines() {
        return mPrefs == null || mPrefs.getBoolean(IS_FIRST_CREATE_ENGINES, true);
    }

    public void setIsFirstCreateEngines(boolean isFirst) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(IS_FIRST_CREATE_ENGINES, isFirst);
        editor.commit();
    }
    // -----------------------------
    // getter/setters for privacy_security_preferences.xml
    // -----------------------------

    public boolean showSecurityWarnings() {
        return mPrefs.getBoolean(PREF_SHOW_SECURITY_WARNINGS, false);
    }

    public boolean acceptCookies() {
        return mPrefs.getBoolean(PREF_ACCEPT_COOKIES, true);
    }

    public boolean saveFormdata() {
        return mPrefs.getBoolean(PREF_SAVE_FORMDATA, true);
    }

    public boolean enableGeolocation() {
        return mPrefs.getBoolean(PREF_ENABLE_GEOLOCATION, true);
    }

    public boolean rememberPasswords() {
        return mPrefs.getBoolean(PREF_REMEMBER_PASSWORDS, true);
    }

    // -----------------------------
    // getter/setters for bandwidth_preferences.xml
    // -----------------------------

    public static String getPreloadOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_wifi_only);
    }

    public static String getPreloadAlwaysPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_always);
    }

    private static final String DEAULT_PRELOAD_SECURE_SETTING_KEY =
            "browser_default_preload_setting";

    public String getDefaultPreloadSetting() {
        String preload = Settings.Secure.getString(mContext.getContentResolver(),
                DEAULT_PRELOAD_SECURE_SETTING_KEY);
        if (preload == null) {
            preload = mContext.getResources().getString(R.string.pref_wifi_only);
        }
        return preload;
    }

    public static String getLinkPrefetchOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_wifi_only);
    }

    public static String getLinkPrefetchAlwaysPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_always);
    }

    private static final String DEFAULT_LINK_PREFETCH_SECURE_SETTING_KEY =
            "browser_default_link_prefetch_setting";

    public String getDefaultLinkPrefetchSetting() {
        String preload = Settings.Secure.getString(mContext.getContentResolver(),
                DEFAULT_LINK_PREFETCH_SECURE_SETTING_KEY);
        if (preload == null) {
            preload = mContext.getResources().getString(R.string.pref_wifi_only);
        }
        return preload;
    }

    /**
     * Get the download path
     *
     * @return the download path
     */
    public String getDownloadPath() {
        return mPrefs.getString(PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH, FileUtils.getLocalDir());
    }

    /**
     * Get the download path
     *
     * @param path
     */
    public void setDownloadPath(String path) {
        mPrefs.edit().putString(PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH, path).apply();
        Wink.get().getSetting().setSimpleResourceStoragePath(path);
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                .ID_CUSTOMDOWNLOADPATH);
    }

    /**
     * Get the search suggestions open state
     *
     * @return
     */
    public boolean getSearchSuggestions() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_SEARCH_SUGGESTIONS, true);
    }

    /**
     * Get the notification tool show state
     *
     * @return
     */
    public boolean getNotificationToolShow() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_NOTIFICATION_TOOL_SHOW, !ChannelUtil.isChinaChannel());
    }

    /**
     * Get the restore last page open state
     *
     * @return
     */
    public boolean getRestoreTabsOnStartup() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_RESTORE_TABS_ON_STARTUP, false);
    }

    public void setRestoreTabsOnStartup(boolean enabled) {
        mPrefs.edit().putBoolean(PREF_RESTORE_TABS_ON_STARTUP, enabled).apply();
    }

    /**
     * Get the clear history and cache when exiting open state
     *
     * @return
     */
    public boolean getClearHistoryAndCacheExiting() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_CLEAR_HISTORY_CACHE_EXITING, false);
    }

    /**
     * Set the clear history and cache when exiting open state
     *
     * @return
     */
    public void setClearHistoryAndCacheExiting(boolean isOpen) {
        mPrefs.edit().putBoolean(PreferenceKeys.PREF_CLEAR_HISTORY_CACHE_EXITING, isOpen).apply();
    }

    /**
     * get the ADs block open state
     *
     * @return
     */
    public boolean getAdBlockEnabled() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_AD_BLOCK, ChannelUtil.isChinaChannel());
    }

    public void setAdBlockEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(PREF_AD_BLOCK, enabled).apply();
    }

    public int getAdBlockCount() {
        return getImgAdBlockCount() + getPopupAdBlockCount() + getJsAdBlockCount();
    }

    public int getImgAdBlockCount() {
        return mPrefs.getInt(PreferenceKeys.PREF_IMG_AD_BLOCK_COUNT, 0);
    }

    public int getJsAdBlockCount() {
        return mPrefs.getInt(PreferenceKeys.PREF_JS_AD_BLOCK_COUNT, 0);
    }

    public int getPopupAdBlockCount() {
        return mPrefs.getInt(PreferenceKeys.PREF_POPUP_AD_BLOCK_COUNT, 0);
    }

    public void setImgAdBlockCount(int count) {
        mPrefs.edit().putInt(PREF_IMG_AD_BLOCK_COUNT, count).apply();
    }

    public void setJsAdBlockCount(int count) {
        mPrefs.edit().putInt(PREF_JS_AD_BLOCK_COUNT, count).apply();
    }

    public void setPopupAdBlockCount(int count) {
        mPrefs.edit().putInt(PREF_POPUP_AD_BLOCK_COUNT, count).apply();
    }

    public void clearAdBlockCount() {
        mPrefs.edit().remove(PREF_IMG_AD_BLOCK_COUNT).apply();
        mPrefs.edit().remove(PREF_JS_AD_BLOCK_COUNT).apply();
        mPrefs.edit().remove(PREF_POPUP_AD_BLOCK_COUNT).apply();
    }

    /**
     * Get the Confirm on exit open state
     *
     * @return
     */
    public boolean getConfirmOnExit() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_CONFIRM_ON_EXIT, true);
    }

    /**
     * Set the Confirm on exit open state
     *
     * @return
     */
    public boolean setConfirmOnExit(boolean isOpen) {
        return mPrefs.edit().putBoolean(PreferenceKeys.PREF_CONFIRM_ON_EXIT, isOpen).commit();
    }

    /**
     * Get the night mode open state
     *
     * @return
     */
    public boolean getNightMode() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_NIGHT_MODE, false);
    }

    /**
     * Set the night mode open state
     *
     * @return
     */
    public void setNightMode(boolean mode) {
        mPrefs.edit().putBoolean(PreferenceKeys.PREF_NIGHT_MODE, mode)
                .apply();
    }

    /**
     * Get the Brightness
     *
     * @return
     */
    public float getBrightness() {
        return mPrefs.getFloat(PreferenceKeys.BRIGHTNESS, DisplayUtil.DEFAULT_BRIGHTNESS);
    }

    /**
     * Set the Brightness
     *
     * @return
     */
    public void setBrightness(float mode) {
        mPrefs.edit().putFloat(PreferenceKeys.BRIGHTNESS, mode)
                .apply();
    }

    /**
     * Get selected default values of clear data list
     *
     * @return
     */
    private Set<String> getDefalutClearData() {
        String[] entryValues = mContext.getResources().getStringArray(R.array.pref_clear_data_values);
        Set<String> set = new HashSet<>();
        for (int i = 0; i < entryValues.length; i++) {
            switch (entryValues[i]) {
                case PreferenceKeys.PREF_PRIVACY_CLEAR_CACHE:
                case PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY:
                    set.add(i + "");
                    break;
            }
        }

        return set;
    }

    /**
     * Get selected values of clear data list
     *
     * @return
     */
    public Set<String> getClearData() {
        return mPrefs.getStringSet(PreferenceKeys.PREF_CLEAR_DATA, getDefalutClearData());
    }

    /**
     * Set selected values of clear data list
     *
     * @param set
     */
    public void setClearData(Set<String> set) {
        mPrefs.edit().putStringSet(PreferenceKeys.PREF_CLEAR_DATA, set).apply();
    }

    public void clearData(String clearOption) {
        switch (clearOption) {
            case PreferenceKeys.PREF_PRIVACY_CLEAR_CACHE:
                clearCache();
                clearDatabases();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                        .ID_CACHE);
                break;
            case PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY:
                clearHistory();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                        .ID_CLEARHISTORY);
                break;
            case PreferenceKeys.PREF_PRIVACY_CLEAR_COOKIES:
                clearCookies();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                        .ID_COOKIES);
                break;
            case PreferenceKeys.PREF_PRIVACY_CLEAR_FORM_DATA:
                clearFormData();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                        .ID_FORMS);
                break;
            case PreferenceKeys.PREF_PRIVACY_CLEAR_PASSWORDS:
                clearPasswords();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                        .ID_PASSWORD);
                break;
            case PreferenceKeys.PREF_PRIVACY_CLEAR_GEOLOCATION_ACCESS:
                clearLocationAccess();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                        .ID_LOCATION);
                break;
            default:
                //do nothing
        }
    }

    public void clearAllData() {
        clearCache();
        clearDatabases();
        clearHistory();
        clearCookies();
        clearFormData();
        clearPasswords();
        clearLocationAccess();
    }

    private void prefTrackEvent(String key, SharedPreferences sharedPreferences) {
        switch (key) {
            case PREF_ACCEPT_COOKIES:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PRIVACYSECURITY_EVENTS, AnalyticsSettings
                        .ID_ACCEPTCOOKIES, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_ENABLE_GEOLOCATION:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PRIVACYSECURITY_EVENTS, AnalyticsSettings
                        .ID_ENABLELOACATION, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_SAVE_FORMDATA:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PRIVACYSECURITY_EVENTS, AnalyticsSettings
                        .ID_REMEMBERDATA, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_REMEMBER_PASSWORDS:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PRIVACYSECURITY_EVENTS, AnalyticsSettings
                        .ID_REMEMBERPASSWORDS, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_SHOW_SECURITY_WARNINGS:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PRIVACYSECURITY_EVENTS, AnalyticsSettings
                        .ID_SECURITYWARNING, sharedPreferences.getBoolean(key, false) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_AUTOFIT_PAGES:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_AUTO_FITPAGES, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_BLOCK_POPUP_WINDOWS:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_BLOCKPOPUPS, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_ENABLE_JAVASCRIPT:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_ENABLEJS, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_ENABLE_COOKIES_INCOGNITO:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_COOKIESINCOGNITO, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_RESTORE_TABS_ON_STARTUP:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_RESTORETABS, sharedPreferences.getBoolean(key, true) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_FORCE_USERSCALABLE:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_FORCEZOOM, sharedPreferences.getBoolean(key, false) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_DOWNLOAD_ADM:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOAD_EVENTS, AnalyticsSettings
                        .ID_ADMDOWNLOAD, sharedPreferences.getBoolean(key, false) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
            case PREF_DOWNLOAD_DIALOG_SHOW:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOAD_EVENTS, AnalyticsSettings
                        .ID_DOWNLOADDIALOG, sharedPreferences.getBoolean(key, false) ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                break;
        }
    }


    public void showNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.notification_search);

        Intent notifyIntent = new Intent(mContext, BrowserActivity.class);
        notifyIntent.putExtra(IntentHandler.ACTION_OPEN_URL_SEARCH, true);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendIntent = PendingIntent.getActivity(mContext, requestCode,
                notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.notification_search_input, pendIntent);

        Intent settingsIntent = new Intent(mContext, BrowserActivity.class);
        settingsIntent.putExtra(IntentHandler.ACTION_OPEN_ADVANCED_PREFERENCES, true);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        requestCode = requestCode + 1;
        pendIntent = PendingIntent.getActivity(mContext, requestCode,
                settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.notification_search_setting_image, pendIntent);

        mBuilder.setSmallIcon(R.drawable.ic_browser_logo)
                .setContent(remoteView)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManager mNotifyManager = (NotificationManager) mContext.getSystemService(Context
                .NOTIFICATION_SERVICE);
        mNotifyManager.notify(NOTIFY_ID, mBuilder.build());
    }

    public void hideNotification() {
        NotificationManager cancelNotificationManager = (NotificationManager) mContext.getSystemService(Context
                .NOTIFICATION_SERVICE);
        cancelNotificationManager.cancel(NOTIFY_ID);
    }

    public boolean getOpenDebugStatus() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_OPEN_DEBUG, false);
    }

    public boolean getDownloadADM() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_DOWNLOAD_ADM, false);
    }

    public void setDownloadConfirm(Boolean confirm) {
        mPrefs.edit().putBoolean(PREF_DOWNLOAD_DIALOG_SHOW, confirm).apply();
    }

    public boolean getDownloadConfirm() {
        return mPrefs.getBoolean(PreferenceKeys.PREF_DOWNLOAD_DIALOG_SHOW, true);
    }
}
