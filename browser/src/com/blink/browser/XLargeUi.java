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

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.webkit.WebView;

import java.util.List;

/**
 * Ui for xlarge screen sizes
 */
public class XLargeUi extends BaseUi {

    private static final String LOGTAG = "XLargeUi";

    private PaintDrawable mFaviconBackground;

    private ActionBar mActionBar;
    private TabBar mTabBar;

    private Handler mHandler;

    /**
     * @param browser
     * @param controller
     */
    public XLargeUi(Activity browser, UiController controller) {
        super(browser, controller);
        mHandler = new Handler();
        mTabBar = new TabBar(mActivity, mUiController, this);
        mActionBar = mActivity.getActionBar();
        setupActionBar();
        setUseQuickControls(BrowserSettings.getInstance().useQuickControls());
    }

    private void setupActionBar() {
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        mActionBar.setCustomView(mTabBar);
    }

    public void showComboView(ComboViews startWith, Bundle extras) {
        super.showComboView(startWith, extras);
        if (mUseQuickControls) {
            mActionBar.show();
        }
    }

    @Override
    public void setUseQuickControls(boolean useQuickControls) {
        super.setUseQuickControls(useQuickControls);
        checkHideActionBar();
        if (!useQuickControls) {
            mActionBar.show();
        }
        mTabBar.setUseQuickControls(mUseQuickControls);
        // We need to update the tabs with this change
        for (Tab t : mTabControl.getTabs()) {
            t.updateShouldCaptureThumbnails();
        }
    }

    private void checkHideActionBar() {
        if (mUseQuickControls) {
            mHandler.post(new Runnable() {
                public void run() {
                    mActionBar.hide();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkHideActionBar();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onConfigurationChanged(Configuration config) {

    }

    void stopWebViewScrolling() {
        WebView web = mUiController.getCurrentWebView();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }


    // WebView callbacks

    @Override
    public void addTab(Tab tab) {
        mTabBar.onNewTab(tab);
    }

    protected void onAddTabCompleted(Tab tab) {
        checkHideActionBar();
    }

    @Override
    public void setActiveTab(final Tab tab) {
        super.setActiveTab(tab);
        BrowserWebView view = (BrowserWebView)tab.getWebView();
        // TabControl.setCurrentTab has been called before this,
        // so the tab is guaranteed to have a webview
        if (view == null) {
            Log.e(LOGTAG, "active tab with no webview detected");
            return;
        }
        mTabBar.onSetActiveTab(tab);
        updateLockIconToLatest(tab);
    }

    @Override
    public void updateTabs(List<Tab> tabs) {
        mTabBar.updateTabs(tabs);
        checkHideActionBar();
    }

    @Override
    public void removeTab(Tab tab) {
        super.removeTab(tab);
        mTabBar.onRemoveTab(tab);
    }

    protected void onRemoveTabCompleted(Tab tab) {
        checkHideActionBar();
    }

    int getContentWidth() {
        if (mContentView != null) {
            return mContentView.getWidth();
        }
        return 0;
    }

    // action mode callbacks

    @Override
    public void onActionModeStarted(ActionMode mode) {
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
        checkHideActionBar();
    }

    @Override
    protected void updateNavigationState(Tab tab) {
    }

    // Set the favicon in the title bar.
    @Override
    public void setFavicon(Tab tab) {
        super.setFavicon(tab);
        mTabBar.onFavicon(tab, tab.getFavicon());
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        checkHideActionBar();
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        return mContentView.dispatchKeyEvent(event);
    }

    private boolean isTypingKey(KeyEvent evt) {
        return evt.getUnicodeChar() > 0;
    }

    TabBar getTabBar() {
        return mTabBar;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return mUseQuickControls;
    }

    private Drawable getFaviconBackground() {
        if (mFaviconBackground == null) {
            mFaviconBackground = new PaintDrawable();
            Resources res = mActivity.getResources();
            mFaviconBackground.getPaint().setColor(
                    res.getColor(R.color.tabFaviconBackground));
            mFaviconBackground.setCornerRadius(
                    res.getDimension(R.dimen.tab_favicon_corner_radius));
        }
        return mFaviconBackground;
    }

    @Override
    public Drawable getFaviconDrawable(Bitmap icon) {
        Drawable[] array = new Drawable[2];
        array[0] = getFaviconBackground();
        if (icon == null) {
            array[1] = mGenericFavicon;
        } else {
            array[1] = new BitmapDrawable(mActivity.getResources(), icon);
        }
        LayerDrawable d = new LayerDrawable(array);
        d.setLayerInset(1, 2, 2, 2, 2);
        return d;
    }

    @Override
    public void onQrUrl(String url) {
//        onSelect(url);
    }

    @Override
    public void onSelectIncognito(String url) {

    }

    @Override
    public void showAndOpenUrl(String url ,boolean isNewTab){}

    public void openViewPage() {}
}
