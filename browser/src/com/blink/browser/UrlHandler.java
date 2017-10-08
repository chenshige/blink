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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import com.blink.browser.util.BuildUtil;

import java.net.URL;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;

public class UrlHandler {

    // Use in overrideUrlLoading
    /* package */ final static String SCHEME_WTAI = "wtai://wp/";
    /* package */ final static String SCHEME_WTAI_MC = "wtai://wp/mc;";
    /* package */ final static String SCHEME_WTAI_SD = "wtai://wp/sd;";
    /* package */ final static String SCHEME_WTAI_AP = "wtai://wp/ap;";

    Controller mController;
    Activity mActivity;

    private Boolean mIsProviderPresent = null;
    private Uri mRlzUri = null;

    public UrlHandler(Controller controller) {
        mController = controller;
        mActivity = mController.getActivity();
    }

    boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        try {
            // When users click URL that is ends with wma or wmv, Browser just download them.
            URL tmpUrl = new URL(url);
            String path = tmpUrl.getPath();
            if (view != null && path != null
                    && (path.endsWith(".wma") || path.endsWith(".wmv")) || path.endsWith(".wav") || path.endsWith("" +
                    ".m4a") || path.endsWith(".aac")) {
                DownloadHandler.onDownloadStart(mController.getActivity(), url, view.getOriginalUrl(), null, null,
                        null, false, mController);
                return true;
            }
        } catch (Exception e) {
        }
        if (url.startsWith(SCHEME_WTAI)) {
            // wtai://wp/mc;number
            // number=string(phone-number)
            if (url.startsWith(SCHEME_WTAI_MC)) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(WebView.SCHEME_TEL +
                                url.substring(SCHEME_WTAI_MC.length())));
                mActivity.startActivity(intent);
                // before leaving BrowserActivity, close the empty child tab.
                // If a new tab is created through JavaScript open to load this
                // url, we would like to close it as we will load this url in a
                // different Activity.
                mController.closeEmptyTab();
                return true;
            }
            // wtai://wp/sd;dtmf
            // dtmf=string(dialstring)
            if (url.startsWith(SCHEME_WTAI_SD)) {
                // TODO: only send when there is active voice connection
                return false;
            }
            // wtai://wp/ap;number;name
            // number=string(phone-number)
            // name=string
            if (url.startsWith(SCHEME_WTAI_AP)) {
                // TODO
                return false;
            }
        }

        // The "about:" schemes are internal to the browser; don't want these to
        // be dispatched to other apps.
        if (url.startsWith("about:")) {
            return false;
        }

        if (startActivityForUrl(tab, url)) {
            return true;
        }

        if (handleMenuClick(tab, url)) {
            return true;
        }

        return false;
    }

    boolean startActivityForUrl(Tab tab, String url) {
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
            r = mActivity.getPackageManager().resolveActivity(intent, 0);
        } catch (Exception e) {
            return false;
        }
        if (r == null) {
            String packagename = intent.getPackage();
            if (packagename != null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("market://search?q=pname:" + packagename));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                try {
                    mActivity.startActivity(intent);
                    // before leaving BrowserActivity, close the empty child tab.
                    // If a new tab is created through JavaScript open to load this
                    // url, we would like to close it as we will load this url in a
                    // different Activity.
                    mController.closeEmptyTab();
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w("Browser", "No activity found to handle " + url);
                    return false;
                }
            } else {
                return true;
            }
        }
        // sanitize the Intent, ensuring web pages can not bypass browser
        // security (only access to BROWSABLE activities).
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setComponent(null);
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.JELLY_BEAN) {
            Intent selector = intent.getSelector();
            if (selector != null) {
                selector.addCategory(Intent.CATEGORY_BROWSABLE);
                selector.setComponent(null);
            }
        }
        // Re-use the existing tab if the intent comes back to us
        if (tab != null) {
            if (tab.getAppId() == null) {
                tab.setAppId(mActivity.getPackageName() + "-" + tab.getId());
            }
            intent.putExtra(BrowserHelper.EXTRA_APPLICATION_ID, tab.getAppId());
        }
        // Make sure webkit can handle it internally before checking for specialized
        // handlers. If webkit can't handle it internally, we need to call
        // startActivityIfNeeded
        Matcher m = UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url);
        if (m.matches() && !isSpecializedHandlerAvailable(intent)) {
            return false;
        }
        try {
            intent.putExtra(BrowserActivity.EXTRA_DISABLE_URL_OVERRIDE, true);
            if (mActivity.startActivityIfNeeded(intent, -1)) {
                // before leaving BrowserActivity, close the empty child tab.
                // If a new tab is created through JavaScript open to load this
                // url, we would like to close it as we will load this url in a
                // different Activity.
                mController.closeEmptyTab();
                return true;
            }
        } catch (ActivityNotFoundException ex) {
            // ignore the error. If no application can handle the URL,
            // eg about:blank, assume the browser can handle it.
        } catch (SecurityException ex) {

        }

        return false;
    }

    /**
     * Search for intent handlers that are specific to this URL
     * aka, specialized apps like google maps or youtube
     */
    private boolean isSpecializedHandlerAvailable(Intent intent) {
        PackageManager pm = mActivity.getPackageManager();
        List<ResolveInfo> handlers = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (handlers == null || handlers.size() == 0) {
            return false;
        }
        boolean ret = false;
        for (ResolveInfo resolveInfo : handlers) {
            IntentFilter filter = resolveInfo.filter;
            if (resolveInfo.activityInfo.packageName.equals(mActivity.getPackageName())) {
                return false;
            }
            if (filter == null) {
                // No intent filter matches this intent?
                // Error on the side of staying in the browser, ignore
                continue;
            }
            if (filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0) {
                // Generic handler, skip
                continue;
            }
            ret = true;
        }
        return ret;
    }

    // In case a physical keyboard is attached, handle clicks with the menu key
    // depressed by opening in a new tab
    boolean handleMenuClick(Tab tab, String url) {
        if (mController.isMenuDown()) {
            //第三个参数true （修改之前是!mSettings.openInBackground(),mSettings.openInBackground()的默认值是false）
            mController.openTab(url,
                    (tab != null) && tab.isPrivateBrowsingEnabled(),
                    true, true);
            mActivity.closeOptionsMenu();
            return true;
        }

        return false;
    }

}
