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

package com.blink.browser;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import com.blink.browser.UI.ComboViews;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.preferences.BrowserPreferencesPage;
import com.blink.browser.preferences.SettingsPreferencesFragment;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Handle all browser related intents
 */
public class IntentHandler {

    private static final String TAG = "IntentHandler";

    // "source" parameter for Google search suggested by the browser
    final static String GOOGLE_SEARCH_SOURCE_SUGGEST = "browser-suggest";
    // "source" parameter for Google search from unknown source
    final static String GOOGLE_SEARCH_SOURCE_UNKNOWN = "unknown";
    public static final String ACTION_OPEN_NEW_TAB = "blink.shortcut.action.OPEN_NEW_TAB";
    public static final String ACTION_OPEN_NEW_INCOGNITO_TAB = "blink.shortcut.action.OPEN_NEW_INCOGNITO_TAB";
    public static final String ACTION_OPEN_URL_SEARCH = "blink.action.OPEN_URL_SEARCH";
    public static final String ACTION_OPEN_ADVANCED_PREFERENCES = "blink.action.OPEN_ADVANCED_PREFERENCES";

    /* package */ static final UrlData EMPTY_URL_DATA = new UrlData(null);

    private static final String[] SCHEME_WHITELIST = {
            "http",
            "https",
            "about",
            "content",
    };

    private Activity mActivity;
    private Controller mController;
    private TabControl mTabControl;
    private BrowserSettings mSettings;

    public IntentHandler(Activity browser, Controller controller) {
        mActivity = browser;
        mController = controller;
        mTabControl = mController.getTabControl();
        mSettings = controller.getSettings();
    }

    void onNewIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null && isForbiddenUri(uri)) {
            Log.e(TAG, "Aborting intent with forbidden uri, \"" + uri + "\"");
            return;
        }
        if (intent.getBooleanExtra(IntentHandler.ACTION_OPEN_URL_SEARCH, false)) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.NOTIFICATIONSEARCH_EVENTS, AnalyticsSettings.ID_SEARCHBARCLICK);
            mController.getUi().openSearchInputView("");
            return;
        }
        if (intent.getBooleanExtra(IntentHandler.ACTION_OPEN_ADVANCED_PREFERENCES, false)) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.NOTIFICATIONSEARCH_EVENTS, AnalyticsSettings.ID_SETTINGSCLICK);
            BrowserPreferencesPage.startPreferenceFragmentForResult(mActivity, SettingsPreferencesFragment.class.getName(), 100);
            return;
        }

        final String action = intent.getAction();
        if (ACTION_OPEN_NEW_TAB.equals(action)) {
            mController.openTabToHomePage();
            return;
        } else if (ACTION_OPEN_NEW_INCOGNITO_TAB.equals(action)) {
            mController.openIncognitoTab();
            return;
        }
        Tab current = mTabControl.getCurrentTab();
        // When a tab is closed on exit, the current tab index is set to -1.
        // Reset before proceed as Browser requires the current tab to be set.
        if (current == null) {
            // Try to reset the tab in case the index was incorrect.
            current = mTabControl.getTab(0);
            if (current == null) {
                // No tabs at all so just ignore this intent.
                return;
            }
            mController.setActiveTab(current);
        }
        final int flags = intent.getFlags();
        if (Intent.ACTION_MAIN.equals(action) ||
                (flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // just resume the browser
            return;
        }
        if (BrowserActivity.ACTION_SHOW_BOOKMARKS.equals(action)) {
            mController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
            return;
        }

        // In case the SearchDialog is open.
        ((SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE))
                .stopSearch();
        if (Intent.ACTION_VIEW.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || Intent.ACTION_SEARCH.equals(action)
                || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)
                || Intent.ACTION_WEB_SEARCH.equals(action)) {
            // If this was a search request (e.g. search query directly typed into the address bar),
            // pass it on to the default web search provider.
            UrlData urlData = getUrlDataFromIntent(intent);
            if (urlData.isEmpty()) {
                String url = handleWebSearchIntent(mActivity, mController, intent);
                if (!TextUtils.isEmpty(url)) {
                    urlData = new UrlData(url);
                } else {
                    urlData = new UrlData(mSettings.getHomePage());
                }
            }

            if (intent.getBooleanExtra(BrowserHelper.EXTRA_CREATE_NEW_TAB, false)
                    || urlData.isPreloaded()
                    || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)
                    || Intent.ACTION_WEB_SEARCH.equals(action)) {
                Tab t = mController.openTab(urlData);
                return;
            }
            /*
             * If the URL is already opened, switch to that tab
             * phone: Reuse tab with same appId
             * tablet: Open new tab
             */
            final String appId = intent
                    .getStringExtra(BrowserHelper.EXTRA_APPLICATION_ID);
            if (Intent.ACTION_VIEW.equals(action)
                    && (appId != null)
                    && appId.startsWith(mActivity.getPackageName())) {
                Tab appTab = mTabControl.getTabFromAppId(appId);
                if ((appTab != null) && (appTab == mController.getCurrentTab())) {
                    mController.switchToTab(appTab);
                    mController.loadUrlDataIn(appTab, urlData);
                    return;
                }
            }
            if (Intent.ACTION_VIEW.equals(action)
                    ) {
                if (!BrowserActivity.isTablet(mActivity)) {
                    Tab appTab = mTabControl.getTabFromAppId(appId);
                    if (appTab != null) {
                        mController.reuseTab(appTab, urlData);
                        return;
                    }
                }
                // No matching application tab, try to find a regular tab
                // with a matching url.
                Tab appTab = mTabControl.findTabWithUrl(urlData.mUrl);
                if (appTab != null) {
                    // Transfer ownership
                    appTab.setAppId(appId);
                    mController.switchToTab(appTab);
                    if (mController.getUi() instanceof PhoneUi) {
                        ((PhoneUi) mController.getUi()).panelSwitch(UI.ComboHomeViews.VIEW_WEBVIEW,
                                mController.getTabControl().getCurrentPosition(), false);
                    }
                    // Otherwise, we are already viewing the correct tab.
                } else {
                    // if FLAG_ACTIVITY_BROUGHT_TO_FRONT flag is on, the url
                    // will be opened in a new tab unless we have reached
                    // MAX_TABS. Then the url will be opened in the current
                    // tab. If a new tab is created, it will have "true" for
                    // exit on close.
                    Tab tab = mController.openTab(urlData);
                    if (tab != null) {
                        tab.setAppId(appId);
                        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
                            tab.setCloseOnBack(true);
                        }
                    }
                }
            } else {
                // Get rid of the subwindow if it exists
                mController.dismissSubWindow(current);
                // If the current Tab is being used as an application tab,
                // remove the association, since the new Intent means that it is
                // no longer associated with that application.
                current.setAppId(null);
                mController.loadUrlDataIn(current, urlData);
            }
        }
    }

    protected static UrlData getUrlDataFromIntent(Intent intent) {
        String url = "";
        Map<String, String> headers = null;
        PreloadedTabControl preloaded = null;
        String preloadedSearchBoxQuery = null;
        if (intent != null
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            final String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action) ||
                    NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                url = UrlUtils.smartUrlFilter(intent.getData());
                if (url != null && url.startsWith("http")) {
                    final Bundle pairs = intent
                            .getBundleExtra(BrowserHelper.EXTRA_HEADERS);
                    if (pairs != null && !pairs.isEmpty()) {
                        Iterator<String> iter = pairs.keySet().iterator();
                        headers = new ArrayMap<>();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            headers.put(key, pairs.getString(key));
                        }
                    }
                }
            } else if (Intent.ACTION_SEARCH.equals(action)
                    || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)
                    || Intent.ACTION_WEB_SEARCH.equals(action)) {
                url = intent.getStringExtra(SearchManager.QUERY);
                if (url != null) {
                    // In general, we shouldn't modify URL from Intent.
                    // But currently, we get the user-typed URL from search box as well.
                    url = UrlUtils.fixUrl(url).trim();
                    url = UrlUtils.smartUrlFilter(url, false);
                }
            }
        }
        return new UrlData(url, headers, intent, preloaded, preloadedSearchBoxQuery);
    }

    /**
     * Launches the default web search activity with the query parameters if the given intent's data
     * are identified as plain search terms and not URLs/shortcuts.
     *
     * @return true if the intent was handled and web search activity was launched, false if not.
     */
    static String handleWebSearchIntent(Activity activity,
                                        Controller controller, Intent intent) {
        if (intent == null) return null;

        String url = null;
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) url = data.toString();
        } else if (Intent.ACTION_SEARCH.equals(action)
                || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)
                || Intent.ACTION_WEB_SEARCH.equals(action)) {
            url = intent.getStringExtra(SearchManager.QUERY);
        }
        return handleWebSearchRequest(activity, controller, url,
                intent.getBundleExtra(SearchManager.APP_DATA),
                intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
    }

    /**
     * Launches the default web search activity with the query parameters if the given url string
     * was identified as plain search terms and not URL/shortcut.
     *
     * @return true if the request was handled and web search activity was launched, false if not.
     */
    private static String handleWebSearchRequest(Activity activity,
                                                 Controller controller, String inUrl, Bundle appData,
                                                 String extraData) {
        if (inUrl == null) return null;

        // In general, we shouldn't modify URL from Intent.
        // But currently, we get the user-typed URL from search box as well.
        String url = inUrl;
        if (!canHandleByOtherApp(inUrl, activity)) {
            url = UrlUtils.fixUrl(inUrl).trim();
        }
        if (TextUtils.isEmpty(url)) return null;

        // URLs are handled by the regular flow of control, so
        // return early.
        if (Patterns.WEB_URL.matcher(url).matches()
                || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url).matches()) {
            return null;
        }

        final ContentResolver cr = activity.getContentResolver();
        final String newUrl = url;
        if (controller == null || controller.getTabControl() == null
                || controller.getTabControl().getCurrentWebView() == null
                || !controller.getTabControl().getCurrentWebView()
                .isPrivateBrowsingEnabled()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... unused) {
                    BrowserHelper.addSearchUrl(cr, newUrl);
                    return null;
                }
            }.execute();
        }

        if (canHandleByOtherApp(url, activity)) {
            return url;
        }
        //directly get the url not send the intent search request
        return UrlUtils.filterBySearchEngine(activity, url);
//        SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
//        if (searchEngine == null) return null;
//        return searchEngine.startSearch(activity, url, appData, extraData);

    }

    private static boolean canHandleByOtherApp(String url, Activity activity) {
        Intent intent;
        // perform generic parsing of the URI to turn it into an Intent.
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException ex) {
            Log.w("Browser", "Bad URI " + url + ": " + ex.getMessage());
            return false;
        }

        // check whether the intent can be resolved. If not, we will see
        // whether we can download it from the Market.
        ResolveInfo r = null;
        try {
            r = activity.getPackageManager().resolveActivity(intent, 0);
        } catch (Exception e) {
            return false;
        }
        return r != null;
    }

    private static boolean isForbiddenUri(Uri uri) {
        String scheme = uri.getScheme();
        // Allow URIs with no scheme
        if (scheme == null) {
            return false;
        }

        scheme = scheme.toLowerCase(Locale.US);
        for (String allowed : SCHEME_WHITELIST) {
            if (allowed.equals(scheme)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A UrlData class to abstract how the content will be sent to WebView.
     * This base class uses loadUrl to show the content.
     */
    static class UrlData {
        final String mUrl;
        final Map<String, String> mHeaders;
        final PreloadedTabControl mPreloadedTab;
        final String mSearchBoxQueryToSubmit;
        final boolean mDisableUrlOverride;

        UrlData(String url) {
            this.mUrl = url;
            this.mHeaders = null;
            this.mPreloadedTab = null;
            this.mSearchBoxQueryToSubmit = null;
            this.mDisableUrlOverride = false;
        }

        UrlData(String url, Map<String, String> headers, Intent intent) {
            this(url, headers, intent, null, null);
        }

        UrlData(String url, Map<String, String> headers, Intent intent,
                PreloadedTabControl preloaded, String searchBoxQueryToSubmit) {
            this.mUrl = url;
            this.mHeaders = headers;
            this.mPreloadedTab = preloaded;
            this.mSearchBoxQueryToSubmit = searchBoxQueryToSubmit;
            if (intent != null) {
                mDisableUrlOverride = intent.getBooleanExtra(
                        BrowserActivity.EXTRA_DISABLE_URL_OVERRIDE, false);
            } else {
                mDisableUrlOverride = false;
            }
        }

        boolean isEmpty() {
            return (mUrl == null || mUrl.length() == 0);
        }

        boolean isPreloaded() {
            return mPreloadedTab != null;
        }

        PreloadedTabControl getPreloadedTab() {
            return mPreloadedTab;
        }

        String getSearchBoxQueryToSubmit() {
            return mSearchBoxQueryToSubmit;
        }
    }

}
