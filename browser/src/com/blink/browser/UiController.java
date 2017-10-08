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
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.blink.browser.UI.ComboViews;
import com.blink.browser.menu.CommonMenu;

import java.util.List;


/**
 * UI aspect of the controller
 */
public interface UiController {

    UI getUi();

    WebView getCurrentWebView();

    WebView getCurrentTopWebView();

    Tab getCurrentTab();

    TabControl getTabControl();

    List<Tab> getTabs();

    Tab openTabToHomePage();

    Tab openIncognitoTab();

    Tab openTab(String url, boolean incognito, boolean setActive,
                boolean useCurrent);

    /**
     * 激活Tab某个tab，并加载新的URl
     *
     * @param tab
     * @param url
     */
    void openTab(Tab tab, String url);

    /**
     * 创建并打开指定的url
     *
     * @param url
     */
    void createAndOpenTab(String url);

    void createAndOpenTabIncognito(String url);

    void setActiveTab(Tab tab);

    boolean switchToTab(Tab tab);

    void closeCurrentTab();

    void closeTab(Tab tab);

    void closeOtherTabs();

    void closeAllTabs(boolean incognito);

    void stopLoading();

    Intent createBookmarkCurrentPageIntent(boolean canBeAnEdit);

    void bookmarksOrHistoryPicker(ComboViews startView);

    void bookmarkCurrentPage();

    void editUrl();

    void handleNewIntent(Intent intent);

    boolean shouldShowErrorConsole();

    void hideCustomView();

    void attachSubWindow(Tab tab);

    void removeSubWindow(Tab tab);

    boolean isInCustomActionMode();

    void endActionMode();

    void shareCurrentPage();

    void updateMenuState(Tab tab, Menu menu);

    boolean onOptionsItemSelected(MenuItem item);

    void loadUrl(Tab tab, String url);

    void loadNativePage(Tab tab);

    void setBlockEvents(boolean block);

    Activity getActivity();

    void showPageInfo();

    void openPreferences();

    void findOnPage();

    void toggleUserAgent();

    BrowserSettings getSettings();

    boolean supportsVoice();

    void startVoiceRecognizer();

    /**
     * The WebView back
     */
    void goBack();

    void goForward();

    boolean canGoBack();

    boolean canGoForward();

    /**
     * Popup menu bar
     */
    void showCommonMenu(CommonMenu menu);

    /**
     * The menu bar item click event
     */
    void menuPopuOnItemClick(View view);

    /**
     * 关闭menu
     */
    void onCloseMenu(boolean isDoAnimation);

    void updateCommomMenuState(CommonMenu commomMenu);

    void onToolBarItemClick(View view);

    void updateToolBarItemState();

    /**
     * 查找dailog dimiss
     *
     * @param id
     */
    void dismissFindDialog(int id);

    void panelSwitchHome(Tab current);

    void registerFullscreenListener(FullscreenListener listener);

    void setFullscreen(boolean isEnabled);

    MainPageController getViewPageController();

    void addBookmark(String title);

    void showDownloadAnimation();

    int toolBarHeight();
}
