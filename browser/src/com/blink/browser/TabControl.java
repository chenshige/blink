/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.webkit.WebView;

import com.blink.browser.widget.IncognitoNotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TabControl {
    // Log Tag
    private static final String LOGTAG = "TabControl";

    // next Tab ID, starting at 1
    private static long sNextId = 1;

    private static final String POSITIONS = "positions";
    private static final String CURRENT = "current";

    public static interface OnThumbnailUpdatedListener {
        void onThumbnailUpdated(Tab t);
    }

    // Maximum number of tabs.
    private int mMaxTabs;
    // Private array of WebViews that are used as tabs.
    private ArrayList<Tab> mTabs;
    private ArrayList<Tab> mIncognitoTabs;
    // Queue of most recently viewed tabs.
    private ArrayList<Tab> mTabQueue;
    // Current position in mTabs.
    private int mCurrentTab = -1;
    private int mCurrentIncognitoTab = -1;
    private static boolean mCurrentTabIsIncognito = false;
    // the main browser controller
    private final Controller mController;

    private OnThumbnailUpdatedListener mOnThumbnailUpdatedListener;
    private List<OnTabCountChangeListener> mTabCountListenerList;

    private Bitmap mHomeCapture;
    private Bitmap mIncognitoHomeCapture;
    private static final String INCOGNITO_TABS_OPEN_TAG = "incognito_tabs_open";
    private static final int INCOGNITO_TABS_OPEN_ID = 100;

    /**
     * Construct a new TabControl object
     */
    TabControl(Controller controller) {
        mController = controller;
        mMaxTabs = mController.getMaxTabs();
        mTabs = new ArrayList<>(mMaxTabs);
        mIncognitoTabs = new ArrayList<>(mMaxTabs);
        mTabQueue = new ArrayList<>(mMaxTabs);
        mTabCountListenerList = new ArrayList<>();
    }

    synchronized static long getNextId() {
        return sNextId++;
    }

    public void setHomeCapture(boolean incognito, Bitmap bitmap, boolean forceCopy) {
        if (incognito) {
            if (mIncognitoHomeCapture == null || forceCopy) {
                if (mIncognitoHomeCapture != null) {
                    mIncognitoHomeCapture.recycle();
                    mIncognitoHomeCapture = null;
                }
                mIncognitoHomeCapture = bitmap.copy(bitmap.getConfig(), true);
            }
        } else {
            if (mHomeCapture == null || forceCopy) {
                if (mHomeCapture != null) {
                    mHomeCapture.recycle();
                    mHomeCapture = null;
                }
                mHomeCapture = bitmap.copy(bitmap.getConfig(), true);
            }
        }
    }

    public Bitmap getHomeCapture() {
        synchronized (TabControl.this) {
            if (isIncognitoShowing()) {
                return mIncognitoHomeCapture;
            } else {
                return mHomeCapture;
            }
        }
    }

    /**
     * Return the current tab's main WebView. This will always return the main
     * WebView for a given tab and not a subwindow.
     *
     * @return The current tab's WebView.
     */
    WebView getCurrentWebView() {
        Tab t = getTab(isIncognitoShowing() ? mCurrentIncognitoTab : mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getWebView();
    }

    /**
     * Return the current tab's top-level WebView. This can return a subwindow
     * if one exists.
     *
     * @return The top-level WebView of the current tab.
     */
    WebView getCurrentTopWebView() {
        Tab t = getTab(isIncognitoShowing() ? mCurrentIncognitoTab : mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getTopWindow();
    }

    /**
     * Return the current tab's subwindow if it exists.
     *
     * @return The subwindow of the current tab or null if it doesn't exist.
     */
    WebView getCurrentSubWindow() {
        Tab t = getTab(isIncognitoShowing() ? mCurrentIncognitoTab : mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getSubWebView();
    }

    /**
     * return the list of tabs
     */
    List<Tab> getTabs() {
        if (!isIncognitoShowing()) {
            return mTabs;
        } else {
            return mIncognitoTabs;
        }
    }

    /**
     * Return the tab at the specified position.
     *
     * @return The Tab for the specified position or null if the tab does not
     * exist.
     */
    Tab getTab(int position) {
        if (!isIncognitoShowing()) {
            if (position >= 0 && position < mTabs.size()) {
                return mTabs.get(position);
            }
        } else {
            if (position >= 0 && position < mIncognitoTabs.size()) {
                return mIncognitoTabs.get(position);
            }
        }
        return null;
    }

    /**
     * Return the current tab.
     *
     * @return The current tab.
     */
    public Tab getCurrentTab() {
        return getTab(isIncognitoShowing() ? mCurrentIncognitoTab : mCurrentTab);
    }

    public Tab getCurrentTabForMode(boolean incognito) {
        if (!incognito) {
            if (mCurrentTab >= 0 && mCurrentTab < mTabs.size()) {
                return mTabs.get(mCurrentTab);
            }
        } else {
            if (mCurrentIncognitoTab >= 0 && mCurrentIncognitoTab < mIncognitoTabs.size()) {
                return mIncognitoTabs.get(mCurrentIncognitoTab);
            }
        }
        return null;
    }

    public boolean isIncognitoShowing() {
        return mCurrentTabIsIncognito;
    }

    void setIncognitoShowing(boolean incognito) {
        mCurrentTabIsIncognito = incognito;
    }

    /**
     * Return the current tab position.
     *
     * @return The current tab position
     */
    int getCurrentPosition() {
        return isIncognitoShowing() ? mCurrentIncognitoTab : mCurrentTab;
    }

    /**
     * Given a Tab, find it's position
     *
     * @param tab to find
     * @return position of Tab or -1 if not found
     */
    int getTabPosition(Tab tab) {
        if (tab == null) {
            return -1;
        }
        if (!tab.isPrivateBrowsingEnabled()) {
            return mTabs.indexOf(tab);
        } else {
            return mIncognitoTabs.indexOf(tab);
        }
    }

    boolean canCreateNewTab(boolean incognito) {
        if (incognito) {
            return mMaxTabs > mIncognitoTabs.size();
        } else {
            return mMaxTabs > mTabs.size();
        }

    }

    boolean canCreateNewTab() {
        if (!isIncognitoShowing()) {
            return mMaxTabs > mTabs.size();
        } else {
            return mMaxTabs > mIncognitoTabs.size();
        }
    }

    /**
     * Returns true if there are any incognito tabs open.
     *
     * @return True when any incognito tabs are open, false otherwise.
     */
    boolean hasAnyOpenIncognitoTabs() {
        return mIncognitoTabs.size() > 0;
    }

    boolean hasAnyOpenNormalTabs() {
        return mTabs.size() > 0;
    }

    void addPreloadedTab(Tab tab) {
        for (Tab current : mTabs) {
            if (current != null && current.getId() == tab.getId()) {
                throw new IllegalStateException("Tab with id " + tab.getId() + " already exists: "
                        + current.toString());
            }
        }
        mTabs.add(tab);
        tab.setController(mController);
        mController.onSetWebView(tab, tab.getWebView());
        tab.putInBackground();
        notifyTabCountChanged();
    }

    /**
     * Create a new tab.
     *
     * @return The newly createTab or null if we have reached the maximum
     * number of open tabs.
     */
    Tab createNewTab(boolean privateBrowsing) {
        return createNewTab(null, privateBrowsing);
    }

    Tab createNewTab(Bundle state, boolean privateBrowsing) {
        // Return false if we have maxed out on tabs
        if (!canCreateNewTab(privateBrowsing)) {
            return null;
        }

        final WebView w = createNewWebView(privateBrowsing);

        // Create a new tab and add it to the tab list
        Tab t = new Tab(mController, w, state);
        if (privateBrowsing) {
            mIncognitoTabs.add(t);
            if (mIncognitoTabs.size() == 1) {
                showIncognitoNotification();
            }
        } else {
            mTabs.add(t);
        }
        // Initially put the tab in the background.
        t.putInBackground();
        notifyTabCountChanged();
        return t;
    }

    /**
     * Create a new tab with default values for closeOnExit(false),
     * appId(null), url(null), and privateBrowsing(false).
     */
    Tab createNewTab() {
        return createNewTab(false);
    }

    SnapshotTab createSnapshotTab(long snapshotId, boolean privateBrowsing) {
        SnapshotTab t = new SnapshotTab(mController, snapshotId, privateBrowsing);
        if (privateBrowsing) {
            mIncognitoTabs.add(t);
        } else {
            mTabs.add(t);
        }
        notifyTabCountChanged();
        return t;
    }

    /**
     * Remove the parent child relationships from all tabs.
     */
    void removeParentChildRelationShips() {
        for (Tab tab : mTabs) {
            tab.removeFromTree();
        }
        for (Tab tab : mIncognitoTabs) {
            tab.removeFromTree();
        }
    }

    boolean removeTab(Tab t) {
        return removeTab(t, true);
    }
    /**
     * Remove the tab from the list. If the tab is the current tab shown, the
     * last created tab will be shown.
     *
     * @param t The tab to be removed.
     * @param deleteThumbnail The tab thumbnail to be removed.
     */
    private boolean removeTab(Tab t, boolean deleteThumbnail) {
        if (t == null) {
            return false;
        }

        // Grab the current tab before modifying the list.
        Tab current = getCurrentTab();

        // Remove t from our list of tabs.
        if (!t.isPrivateBrowsingEnabled()) {
            mTabs.remove(t);
        } else {
            mIncognitoTabs.remove(t);
            if (mIncognitoTabs.size() <= 0) {
                dismissIncognitoNotification();
            }
        }

        // Put the tab in the background only if it is the current one.
        if (current == t) {
            t.putInBackground();
            if (isIncognitoShowing()) {
                mCurrentIncognitoTab = -1;
            } else {
                mCurrentTab = -1;
            }
        } else {
            // If a tab that is earlier in the list gets removed, the current
            // index no longer points to the correct tab.
            if (null != current) {
                mCurrentTabIsIncognito = current.isPrivateBrowsingEnabled();
                if (mCurrentTabIsIncognito) {
                    mCurrentIncognitoTab = getTabPosition(current);
                } else {
                    mCurrentTab = getTabPosition(current);
                }
            }
        }

        // destroy the tab
        t.destroy();
        // clear it's references to parent and children
        t.removeFromTree(deleteThumbnail);

        // Remove it from the queue of viewed tabs.
        mTabQueue.remove(t);
        notifyTabCountChanged();
        return true;
    }

    /**
     * Destroy all the tabs and subwindows
     */
    void destroy() {
        for (Tab t : mTabs) {
            t.destroy();
        }
        mTabs.clear();
        for (Tab t : mIncognitoTabs) {
            t.destroy();
        }
        mIncognitoTabs.clear();
        mTabQueue.clear();
        notifyTabCountChanged();
    }

    /**
     * Returns the number of tabs created.
     *
     * @return The number of tabs created.
     */
    int getTabCount() {
        if (!isIncognitoShowing()) {
            return mTabs.size();
        } else {
            return mIncognitoTabs.size();
        }
    }

    /**
     * save the tab state:
     * current position
     * position sorted array of tab ids
     * for each tab id, save the tab state
     *
     * @param outState out state Bundle
     */
    void saveState(Bundle outState) {
        final int numTabs = mTabs.size();
        if (numTabs == 0) {
            return;
        }
        long[] ids = new long[numTabs];
        int i = 0;
        for (Tab tab : mTabs) {
            Bundle tabState = tab.saveState();
            if (tabState != null) {
                ids[i++] = tab.getId();
                String key = Long.toString(tab.getId());
                if (outState.containsKey(key)) {
                    // Dump the tab state for debugging purposes
                    for (Tab dt : mTabs) {
                        Log.e(LOGTAG, dt.toString());
                    }
                    throw new IllegalStateException(
                            "Error saving state, duplicate tab ids!");
                }
                outState.putBundle(key, tabState);
            } else {
                ids[i++] = -1;
                // Since we won't be restoring the thumbnail, delete it
                tab.deleteThumbnail();
            }
        }
        if (!outState.isEmpty()) {
            outState.putLongArray(POSITIONS, ids);
            Tab current = getCurrentTab();
            long cid = -1;
            if (current != null) {
                cid = current.getId();
            }
            outState.putLong(CURRENT, cid);
        }
    }

    /**
     * Check if the state can be restored.  If the state can be restored, the
     * current tab id is returned.  This can be passed to restoreState below
     * in order to restore the correct tab.  Otherwise, -1 is returned and the
     * state cannot be restored.
     */
    long canRestoreState(Bundle inState, boolean restoreIncognitoTabs) {
        if (!BrowserSettings.getInstance().getRestoreTabsOnStartup() || BrowserSettings.getInstance().getHomePageChanged()) {
            return -1;
        }
        final long[] ids = (inState == null) ? null : inState.getLongArray(POSITIONS);
        if (ids == null) {
            return -1;
        }
        final long oldcurrent = inState.getLong(CURRENT);
        long current = -1;
        if (restoreIncognitoTabs || (hasState(oldcurrent, inState) && !isIncognito(oldcurrent, inState))) {
            current = oldcurrent;
        } else {
            // pick first non incognito tab
            for (long id : ids) {
                if (hasState(id, inState) && !isIncognito(id, inState)) {
                    current = id;
                    break;
                }
            }
        }
        return current;
    }

    private boolean hasState(long id, Bundle state) {
        if (id == -1) return false;
        Bundle tab = state.getBundle(Long.toString(id));
        return ((tab != null) && !tab.isEmpty());
    }

    private boolean isIncognito(long id, Bundle state) {
        Bundle tabstate = state.getBundle(Long.toString(id));
        if ((tabstate != null) && !tabstate.isEmpty()) {
            return tabstate.getBoolean(Tab.INCOGNITO);
        }
        return false;
    }

    /**
     * Restore the state of all the tabs.
     *
     * @param currentId            The tab id to restore.
     * @param inState              The saved state of all the tabs.
     * @param restoreIncognitoTabs Restoring private browsing tabs
     * @param restoreAll           All webviews get restored, not just the current tab
     *                             (this does not override handling of incognito tabs)
     */
    void restoreState(Bundle inState, long currentId,
                      boolean restoreIncognitoTabs, boolean restoreAll) {
        if (currentId == -1) {
            return;
        }
        //the restore tab's id maybe equal with current tab's id,
        // decide thumbnail whether to delete after restore finish
        removeTab(getCurrentTab(), false);
        long[] ids = inState.getLongArray(POSITIONS);
        long maxId = -Long.MAX_VALUE;
        Map<Long, Tab> tabMap = new ArrayMap<Long, Tab>();
        for (long id : ids) {
            if (id > maxId) {
                maxId = id;
            }
            final String idkey = Long.toString(id);
            Bundle state = inState.getBundle(idkey);
            if (state == null || state.isEmpty()) {
                // Skip tab
                continue;
            } else if (!restoreIncognitoTabs
                    && state.getBoolean(Tab.INCOGNITO)) {
                // ignore tab
            } else if (id == currentId || restoreAll) {
                Tab t = createNewTab(state, false);
                if (t == null) {
                    // We could "break" at this point, but we want
                    // sNextId to be set correctly.
                    continue;
                }
                tabMap.put(id, t);
                // Me must set the current tab before restoring the state
                // so that all the client classes are set.
                if (id == currentId) {
                    setCurrentTab(t);
                }
            } else {
                // Create a new tab and don't restore the state yet, add it
                // to the tab list
                Tab t = new Tab(mController, state);
                tabMap.put(id, t);
                mTabs.add(t);
                // added the tab to the front as they are not current
                mTabQueue.add(0, t);
                notifyTabCountChanged();
            }
        }

        // make sure that there is no id overlap between the restored
        // and new tabs
        sNextId = maxId + 1;

        if (mCurrentTab == -1) {
            if (getTabCount() > 0) {
                setCurrentTab(getTab(0));
            }
        }
        // restore parent/child relationships
        for (long id : ids) {
            final Tab tab = tabMap.get(id);
            final Bundle b = inState.getBundle(Long.toString(id));
            if ((b != null) && (tab != null)) {
                final long parentId = b.getLong(Tab.PARENTTAB, -1);
                if (parentId != -1) {
                    final Tab parent = tabMap.get(parentId);
                    if (parent != null) {
                        parent.addChildTab(tab);
                    }
                }
            }
        }
    }

    /**
     * Free the memory in this order, 1) free the background tabs; 2) free the
     * WebView cache;
     */
    void freeMemory() {
        if (getTabCount() == 0) return;

        // free the least frequently used background tabs
        Vector<Tab> tabs = getHalfLeastUsedTabs(getCurrentTab());
        if (tabs.size() > 0) {
            Log.w(LOGTAG, "Free " + tabs.size() + " tabs in the browser");
            for (Tab t : tabs) {
                // store the WebView's state.
                t.saveState();
                // destroy the tab
                t.destroy();
            }
            return;
        }

        // free the WebView's unused memory (this includes the cache)
        Log.w(LOGTAG, "Free WebView's unused memory and cache");
        WebView view = getCurrentWebView();
        if (view != null) {
        	view.freeMemory();
        }
    }

    private Vector<Tab> getHalfLeastUsedTabs(Tab current) {
        Vector<Tab> tabsToGo = new Vector<Tab>();

        // Don't do anything if we only have 1 tab or if the current tab is
        // null.
        if (getTabCount() == 1 || current == null) {
            return tabsToGo;
        }

        if (mTabQueue.size() == 0) {
            return tabsToGo;
        }

        // Rip through the queue starting at the beginning and tear down half of
        // available tabs which are not the current tab or the parent of the
        // current tab.
        int openTabCount = 0;
        for (Tab t : mTabQueue) {
            if (t != null && t.getWebView() != null) {
                openTabCount++;
                if (t != current && t != current.getParent()) {
                    tabsToGo.add(t);
                }
            }
        }

        openTabCount /= 2;
        if (tabsToGo.size() > openTabCount) {
            tabsToGo.setSize(openTabCount);
        }

        return tabsToGo;
    }

    Tab getLeastUsedTab(Tab current) {
        if (getTabCount() == 1 || current == null) {
            return null;
        }
        if (mTabQueue.size() == 0) {
            return null;
        }
        // find a tab which is not the current tab or the parent of the
        // current tab
        for (Tab t : mTabQueue) {
            if (t != null && t.getWebView() != null) {
                if (t != current && t != current.getParent()) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Show the tab that contains the given WebView.
     *
     * @param view The WebView used to find the tab.
     */
    Tab getTabFromView(WebView view) {
        if(view == null){
            return null;
        }
        for (Tab t : mTabs) {

            if (t.getSubWebView() == view || t.getWebView() == view) {
                return t;
            }
        }
        for (Tab t : mIncognitoTabs) {
            if (t.getSubWebView() == view || t.getWebView() == view) {
                return t;
            }
        }
        return null;
    }

    /**
     * Return the tab with the matching application id.
     *
     * @param id The application identifier.
     */
    Tab getTabFromAppId(String id) {
        if (id == null) {
            return null;
        }
        for (Tab t : mTabs) {
            if (id.equals(t.getAppId())) {
                return t;
            }
        }
        for (Tab t : mIncognitoTabs) {
            if (id.equals(t.getAppId())) {
                return t;
            }
        }
        return null;
    }

    /**
     * Stop loading in all opened WebView including subWindows.
     */
    void stopAllLoading() {
        for (Tab t : mTabs) {
            final WebView webView = t.getWebView();
            if (webView != null) {
                webView.stopLoading();
            }
            final WebView subview = t.getSubWebView();
            if (subview != null) {
            	subview.stopLoading();
            }
        }
        for (Tab t : mIncognitoTabs) {
            final WebView webview = t.getWebView();
            if (webview != null) {
            	webview.stopLoading();
            }
            final WebView subview = t.getSubWebView();
            if (subview != null) {
            	subview.stopLoading();
            }
        }
    }

    // This method checks if a tab matches the given url.
    private boolean tabMatchesUrl(Tab t, String url) {
        return url.equals(t.getUrl()) || url.equals(t.getOriginalUrl());
    }

    /**
     * Return the tab that matches the given url.
     *
     * @param url The url to search for.
     */
    Tab findTabWithUrl(String url) {
        if (url == null) {
            return null;
        }
        // Check the current tab first.
        Tab currentTab = getCurrentTab();
        if (currentTab != null && tabMatchesUrl(currentTab, url)) {
            return currentTab;
        }
        // Now check all the rest.
        for (Tab tab : mTabs) {
            if (tabMatchesUrl(tab, url)) {
                return tab;
            }
        }
        for (Tab tab : mIncognitoTabs) {
            if (tabMatchesUrl(tab, url)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Recreate the main WebView of the given tab.
     */
    void recreateWebView(Tab t) {
        final WebView w = t.getWebView();
        boolean currentIncognito = t.isPrivateBrowsingEnabled();
        if (w != null) {
            t.destroy();
        }
        // Create a new WebView. If this tab is the current tab, we need to put
        // back all the clients so force it to be the current tab.
        t.setWebView(createNewWebView(currentIncognito), false);
        if (getCurrentTab() == t) {
            setCurrentTab(t, true);
        }
    }

    /**
     * Creates a new WebView and registers it with the global settings.
     */
    public WebView createNewWebView() {
        return createNewWebView(false);
    }

    /**
     * Creates a new WebView and registers it with the global settings.
     *
     * @param privateBrowsing When true, enables private browsing in the new
     *                        WebView.
     */
    private WebView createNewWebView(boolean privateBrowsing) {
        return mController.getWebViewFactory().createWebView(privateBrowsing);
    }

    /**
     * Put the current tab in the background and set newTab as the current tab.
     *
     * @param newTab The new tab. If newTab is null, the current tab is not
     *               set.
     */
    boolean setCurrentTab(Tab newTab) {
        return setCurrentTab(newTab, false);
    }

    /**
     * If force is true, this method skips the check for newTab == current.
     */
    private boolean setCurrentTab(Tab newTab, boolean force) {
        Tab current = getTab(isIncognitoShowing() ? mCurrentIncognitoTab : mCurrentTab);
        if (current == newTab && !force) {
            return true;
        }
        if (current != null) {
            current.putInBackground();
            if (newTab != null) {
                if (isIncognitoShowing() && newTab.isPrivateBrowsingEnabled()) {
                    mCurrentIncognitoTab = -1;
                }
                if (!isIncognitoShowing() && !newTab.isPrivateBrowsingEnabled()) {
                    mCurrentTab = -1;
                }
            } else {
                if (isIncognitoShowing()) {
                    mCurrentIncognitoTab = -1;
                } else {
                    mCurrentTab = -1;
                }
            }
        }
        if (newTab == null) {
            return false;
        }

        // Move the newTab to the end of the queue
        int index = mTabQueue.indexOf(newTab);
        if (index != -1) {
            mTabQueue.remove(index);
        }
        mTabQueue.add(newTab);

        // Display the new current tab
        if (!newTab.isPrivateBrowsingEnabled()) {
            mCurrentTab = mTabs.indexOf(newTab);
            mCurrentTabIsIncognito = false;
        } else {
            mCurrentIncognitoTab = mIncognitoTabs.indexOf(newTab);
            mCurrentTabIsIncognito = true;
        }
        WebView mainView = newTab.getWebView();
        boolean needRestore = mainView == null;
        if (needRestore && !newTab.isNativePage()) {
            // Same work as in createNewTab() except don't do new Tab()
            mainView = createNewWebView();
            newTab.setWebView(mainView);
        }
        newTab.putInForeground();
        return true;
    }

    // Used by Tab.onJsAlert() and friends
    void setActiveTab(Tab tab) {
        // Calls TabControl.setCurrentTab()
        mController.setActiveTab(tab);
    }

    /**
     * NavScreen is Showing
     *
     * @return
     */
    public boolean isShowingNavScreen() {
        UI mUi = mController.getUi();
        if (mUi instanceof PhoneUi) {
            return ((PhoneUi) mUi).showingNavScreen();
        }
        return false;
    }

    public void setOnThumbnailUpdatedListener(final OnThumbnailUpdatedListener listener) {
        mOnThumbnailUpdatedListener = listener;
        for (final Tab t : mTabs) {
            WebView web = t.getWebView();
            if (web != null) {
            	web.setPictureListener(listener != null ? t : null);
            }
        }
        for (final Tab t : mIncognitoTabs) {
            WebView web = t.getWebView();
            if (web != null) {
            	web.setPictureListener(listener != null ? t : null);
            }
        }
    }

    public void notifyTabCountChanged() {
        if (mTabCountListenerList == null || mTabCountListenerList.isEmpty()) {
            return;
        }
        for (int i = 0; i < mTabCountListenerList.size(); i++) {
            mTabCountListenerList.get(i).onTabCountUpdate(getTabCount());
        }
    }

    public void registerTabChangeListener(OnTabCountChangeListener listerner) {
        if (listerner != null && mTabCountListenerList != null) {
            mTabCountListenerList.add(listerner);
        }
        notifyTabCountChanged();
    }

    public OnThumbnailUpdatedListener getOnThumbnailUpdatedListener() {
        return mOnThumbnailUpdatedListener;
    }

    public interface OnTabCountChangeListener {
        void onTabCountUpdate(int tabCount);
    }

    public void showIncognitoNotification() {
        Context context = mController.getActivity();
        String actionMessage = context.getResources().getString(R.string.close_all_incognito_notification);
        String title = context.getResources().getString(R.string.application_name);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentIntent(IncognitoNotificationService.getRemoveAllIncognitoTabsIntent(context))
                .setContentText(actionMessage)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setSmallIcon(R.drawable.ic_browser_logo)
                .setShowWhen(false)
                .setLocalOnly(true);
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(INCOGNITO_TABS_OPEN_TAG, INCOGNITO_TABS_OPEN_ID, builder.build());
    }

    /**
     * Dismisses the incognito notification.
     */
    public void dismissIncognitoNotification() {
        Context context = mController.getActivity();
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(INCOGNITO_TABS_OPEN_TAG, INCOGNITO_TABS_OPEN_ID);
    }
}
