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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.ChannelUtil;
import com.blink.browser.util.SharedPreferencesUtils;

/**
 * Web view factory class for creating {@link BrowserWebView}'s.
 */
public class BrowserWebViewFactory implements WebViewFactory {


    private final Context mContext;

    public BrowserWebViewFactory(Context context) {
        mContext = context;
    }

    protected WebView instantiateWebView(AttributeSet attrs, int defStyle) {
        return new BrowserWebView(mContext, attrs, defStyle);
    }

    @Override
    public WebView createSubWebView(boolean privateBrowsing) {
        return createWebView(privateBrowsing);
    }

    @Override
    public WebView createWebView(boolean privateBrowsing) {
        WebView w = instantiateWebView(null, android.R.attr.webViewStyle);
        initWebViewSettings(w);
        ((BrowserWebView)w).setPrivateBrowsing(privateBrowsing);
        return w;
    }

    protected void initWebViewSettings(WebView w) {
        w.setScrollbarFadingEnabled(true);
        w.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        w.setMapTrackballToArrowKeys(false); // use trackball directly
        // Enable the built-in zoom
        w.getSettings().setBuiltInZoomControls(true);
        final PackageManager pm = mContext.getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)
                        || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);
        w.getSettings().setDisplayZoomControls(!supportsMultiTouch);

        // Add this WebView to the settings observer list and update the
        // settings
        final BrowserSettings s = BrowserSettings.getInstance();
        s.startManagingSettings(w.getSettings());

        if (Build.VERSION.SDK_INT > BuildUtil.VERSION_CODES.KITKAT) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(w, cookieManager.acceptCookie());
        }

        if (android.os.Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            // Remote Web Debugging is always enabled, where available.
            boolean isOpen = BrowserSettings.getInstance().getOpenDebugStatus();
            if (isOpen || !ChannelUtil.isOpenPlatform()) {
                WebView.setWebContentsDebuggingEnabled(isOpen);
            }
        }
    }

}
