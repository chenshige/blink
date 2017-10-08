/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.app.Application;
import android.webkit.CookieSyncManager;

import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.BrowserWinkModule;
import com.blink.browser.util.Logger;
import com.wcc.wink.Wink;

import java.lang.ref.WeakReference;

public class Browser extends Application {

    private final static String LOGTAG = "browser";

    private static Browser sInstance;

    private WeakReference<ActivityController> mController;

    public Browser() {
        sInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // create CookieSyncManager with current Context
        CookieSyncManager.createInstance(this);
        // Create Browser Analytics.
        BrowserAnalytics.createAnalytics(this.getApplicationContext());
        BrowserSettings.initialize(getApplicationContext());
        Preloader.initialize(getApplicationContext());
        Wink.initInstance(this, new BrowserWinkModule());
    }


    public static Browser getInstance() {
        return sInstance;
    }

    public void setController(ActivityController controller) {
        mController = new WeakReference<ActivityController>(controller);
    }

    public WeakReference<ActivityController> getController() {
        return mController;
    }
}

