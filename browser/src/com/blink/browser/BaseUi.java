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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blink.browser.Tab.SecurityState;
import com.blink.browser.download.BrowserNetworkStateNotifier;
import com.blink.browser.handler.BrowserHandler;
import com.blink.browser.homepages.ImmersiveController;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.search.SearchEngines;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.LazyTaskHandler;
import com.blink.browser.util.SharedPreferencesUtils;
import com.blink.browser.util.SystemTintBarUtils;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.video.FullScreenToolLayer;
import com.blink.browser.video.JsInterfaceInject;
import com.blink.browser.video.VideoPlayerLayer;
import com.blink.browser.view.CircleImageView;
import com.blink.browser.view.LeadPageView;
import com.blink.browser.view.ScrollFrameLayout;
import com.blink.browser.view.switchsearchengine.SelectSearchEngine;

import java.util.List;
import java.util.Map;


/**
 * UI interface definitions
 */
public abstract class BaseUi implements UI, FullscreenListener, SearchEngines.IDefaultEngineIconUpdateListener,
        ScrollFrameLayout.IScrollListener {

    private static final String LOGTAG = "BaseUi";
    public static final String INPUT_SEARCH_URL = "url";
    public static final String INPUT_SEARCH_INCOGNITO = "incognito";

    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
            new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

    Activity mActivity;
    UiController mUiController;
    TabControl mTabControl;
    protected Tab mActiveTab;
    private InputMethodManager mInputManager;

    private Drawable mLockIconSecure;
    private Drawable mLockIconMixed;
    protected Drawable mGenericFavicon;

    protected FrameLayout mContentView;
    protected FrameLayout mCustomViewContainer;
    protected FrameLayout mFullscreenContainer;
    protected VideoPlayerLayer mToolLayer;
    private ScrollFrameLayout mMainContent;
    private FrameLayout mHomepageContainer;
    protected View mDivider;

    protected final MainPageController mMainPageController;

    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private LinearLayout mErrorConsoleContainer = null;
    private int mOriginalOrientation;

    private UrlBarAutoShowManager mUrlBarAutoShowManager;

    private Toast mStopToast;

    private Map<WebView, JsInterfaceInject> mJsObjectMap = null;

    // the default <video> poster
    private Bitmap mDefaultVideoPoster;
    // the video progress view
    private View mVideoProgressView;

    protected ToolBar mToolbar;
    private TextView mBottomButton;
    protected View mToolbarDivider;
    private boolean mActivityPaused;
    protected boolean mUseQuickControls;
    protected PieControl mPieControl;
    private boolean mBlockFocusAnimations;
    private final static int mFullScreenImmersiveSetting =
//                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | //隐藏底部NavigationBar
            View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private SelectSearchEngine mSelectSearchEngine;
    private boolean mVideoFullScreen = false;
    private int mLazyLayout = 0;
    private int mKeyboardHeight = 0;

    private LeadPageView mLeadPageView;

    public BaseUi(Activity browser, UiController controller) {
        mMainPageController = controller.getViewPageController();
        mActivity = browser;
        mUiController = controller;
        mUiController.registerFullscreenListener(this);
        mTabControl = controller.getTabControl();
        Resources res = mActivity.getResources();
        mInputManager = (InputMethodManager)
                browser.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mLockIconSecure = res.getDrawable(R.drawable.ic_secure_holo_dark);
        mLockIconMixed = res.getDrawable(R.drawable.ic_secure_partial_holo_dark);
        FrameLayout frameLayout = (FrameLayout) mActivity.getWindow()
                .getDecorView().findViewById(android.R.id.content);
        LayoutInflater.from(mActivity)
                .inflate(R.layout.custom_screen, frameLayout);
        mMainContent = (ScrollFrameLayout) frameLayout.findViewById(R.id.custom_screen);
        mMainContent.setUiController(mUiController);
        mHomepageContainer = (FrameLayout) frameLayout.findViewById(R.id.homepage_container);
        mContentView = (FrameLayout) frameLayout.findViewById(
                R.id.main_content);
        mCustomViewContainer = (FrameLayout) frameLayout.findViewById(
                R.id.fullscreen_custom_content);
        mErrorConsoleContainer = (LinearLayout) frameLayout
                .findViewById(R.id.error_console);

        mDivider = frameLayout.findViewById(R.id.divider);
        mGenericFavicon = res.getDrawable(R.drawable.app_web_browser_sm);
        mToolbar = (ToolBar) frameLayout.findViewById(R.id.bottom_bar).findViewById(R.id.tool_bar);
        mToolbar.setUicontroller(mUiController);
        mToolbarDivider = frameLayout.findViewById(R.id.divider);
        mToolbar.setDividerView(mToolbarDivider);
        mToolbar.setBackHomeView((CircleImageView) frameLayout.findViewById(R.id.back_home));
        mBottomButton = (TextView) frameLayout.findViewById(R.id.btn_bottom);
        mUrlBarAutoShowManager = new UrlBarAutoShowManager(this);
        mJsObjectMap = new ArrayMap<>();
        frameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ++mLazyLayout;
                // Execute Lazy Tasks after 3 layout.
                if (mLazyLayout == 2) {
                    LazyTaskHandler.executeLazyTask();
                }
            }
        });
        SearchEngines.getInstance(mActivity).registerIconUpdateListener(this);

        if (mMainContent != null) {
            mMainContent.registerScrollListener(this);
        }
    }

    public TextView getBottomButton() {
        return mBottomButton;
    }

    public View getMainContent() {
        return mMainContent;
    }

    public View getHomePageContainer() {
        return mHomepageContainer;
    }

    private void cancelStopToast() {
        if (mStopToast != null) {
            mStopToast.cancel();
            mStopToast = null;
        }
    }

    protected ViewGroup getHomeContainer() {
        return mHomepageContainer;
    }

    public void onPause() {
        if (mLeadPageView != null) {
            mLeadPageView.dismiss();
        }

        if (isCustomViewShowing()) {
            onHideCustomView();
        }
        cancelStopToast();
        mActivityPaused = true;
    }

    public void onResume() {
        mActivityPaused = false;
        // check if we exited without setting active tab
        // b: 5188145
        final Tab ct = mTabControl.getCurrentTab();
        if (ct != null && !ct.isNativePage()) {
            setActiveTab(ct);
        }
        mMainPageController.onResume();
        updateStatusBarState(!BrowserSettings.getInstance().getShowStatusBar());
        if (mToolbar != null) {
            mToolbar.updateToolbarStyle();
        }
    }

    public void onConfigurationChanged(Configuration config) {
        changeWebViewHeight();
        if (mToolbar != null) {
            mToolbar.onConfigurationChanged(config);
        }
    }

    protected boolean isActivityPaused() {
        return mActivityPaused;
    }

    public void changeWebViewHeight() {
        changeWebViewHeight(0);
    }

    public void changeWebViewHeight(int keyHeight) {
        if (mToolbar == null) {
            return;
        }
        int phoneHeight = DisplayUtil.getScreenHeight(mActivity);
        int phoneWidth = DisplayUtil.getScreenWidth(mActivity);
        int state = mToolbar.getToolbarState();
        int toolbarHeight = mActivity.getResources().getDimensionPixelOffset(R.dimen.bottom_toolbar_height);
        int toolbarFoldHeight = mActivity.getResources().getDimensionPixelOffset(R.dimen.bottom_toolbar_scroll_animator_distance);
        LayoutParams params = mContentView.getLayoutParams();

        int height = phoneHeight;

        if (BrowserSettings.getInstance().getShowStatusBar()) {
            height -= DisplayUtil.getStatusBarHeight(mActivity);
        }

        if (state == ToolBar.STATE_DOWNING || state == ToolBar.STATE_DOWN) {
            height -= toolbarHeight;
        } else if (state == ToolBar.STATE_UPPING || state == ToolBar.STATE_UP) {
            height -= toolbarFoldHeight;
        }
        height -= keyHeight;
        if (params.height == height) {
            return;
        }
        params.height = height;
        params.width = phoneWidth;
        mContentView.setLayoutParams(params);
    }

    public Activity getActivity() {
        return mActivity;
    }

    // key handling

    @Override
    public boolean onBackKey() {
        if (mLeadPageView != null) {
            return true;
        } else if (mSelectSearchEngine != null) {
            mSelectSearchEngine.onBackKey();
            return true;
        } else if (mCustomView != null) {
            mUiController.hideCustomView();
            return true;
        }
        return false;
    }

    @Override
    public void onNetworkToggle(boolean up) {
    }

    @Override
    public boolean onMenuKey() {
        return false;
    }

    @Override
    public void setUseQuickControls(boolean useQuickControls) {
        mUseQuickControls = useQuickControls;
        if (useQuickControls) {
            mPieControl = new PieControl(mActivity, mUiController, this);
            mPieControl.attachToContainer(mContentView);
        } else {
            if (mPieControl != null) {
                mPieControl.removeFromContainer(mContentView);
            }
        }
        updateUrlBarAutoShowManagerTarget();
    }

    // Tab callbacks
    @Override
    public void onTabDataChanged(Tab tab) {
        setFavicon(tab);
        updateLockIconToLatest(tab);
        updateNavigationState(tab);
        onProgressChanged(tab);
        mUiController.updateToolBarItemState();
        if (mToolbar != null) {
            mToolbar.setUrlTitle(tab);
        }
    }

    @Override
    public void onProgressChanged(Tab tab) {
        int progress = tab.getLoadProgress();
        if (tab.inForeground()) {
            if (mToolbar != null) {
                mToolbar.setProgress(progress);
            }
        }

    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        if (tab.inForeground()) {
            boolean isBookmark = tab.isBookmarkedSite();
        }
    }

    @Override
    public void onPageStopped(Tab tab) {
        cancelStopToast();
        if (tab.inForeground()) {
            if (mStopToast == null) {
                mStopToast = ToastUtil.toastShow(mActivity, R.string.stopping, Toast.LENGTH_SHORT);
            }
            mStopToast.show();
        }
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return true;
    }

    @Override

    public void addTab(Tab tab) {
    }

    @Override
    public void setActiveTab(final Tab tab) {
        if (tab == null) return;
        // block unnecessary focus change animations during tab switch
        mBlockFocusAnimations = true;
        if ((tab != mActiveTab) && (mActiveTab != null)) {
            removeTabFromContentView(mActiveTab);
            WebView web = mActiveTab.getWebView();
            if (web != null) {
                web.setOnTouchListener(null);
            }
        }
        mActiveTab = tab;
        BrowserWebView web = (BrowserWebView)mActiveTab.getWebView();
//        updateUrlBarAutoShowManagerTarget();
        attachTabToContentView(tab);
        if (!tab.isNativePage() && tab.getTopWindow() != null) {
            tab.getTopWindow().requestFocus();
        }
        setShouldShowErrorConsole(tab, mUiController.shouldShowErrorConsole());
        onTabDataChanged(tab);
        onProgressChanged(tab);
        mBlockFocusAnimations = false;
    }

    protected void updateUrlBarAutoShowManagerTarget() {
        WebView web = mActiveTab != null ? mActiveTab.getWebView() : null;
        if (!BrowserSettings.getInstance().useFullscreen()) {
            mUrlBarAutoShowManager.setTarget(null);
            return;
        }
        if (!mUseQuickControls && web instanceof BrowserWebView) {
            mUrlBarAutoShowManager.setTarget((BrowserWebView)web);
        } else {
            mUrlBarAutoShowManager.setTarget(null);
        }
    }

    Tab getActiveTab() {
        return mActiveTab;
    }

    @Override
    public void updateTabs(List<Tab> tabs) {
    }

    @Override
    public void removeTab(Tab tab) {
        if (mActiveTab == tab) {
            removeTabFromContentView(tab);
            mActiveTab = null;
        }
    }

    @Override
    public void detachTab(Tab tab) {
        removeTabFromContentView(tab);
    }

    @Override
    public void attachTab(Tab tab) {
        attachTabToContentView(tab);
    }

    protected void attachTabToContentView(Tab tab) {
        if ((tab == null) || (tab.getWebView() == null)) {
            return;
        }
        View container = tab.getViewContainer();
        WebView mainView = tab.getWebView();

        // Attach the WebView to the container and then attach the
        // container to the content view.
        FrameLayout wrapper =
                (FrameLayout) container.findViewById(R.id.webview_wrapper);
        ViewGroup parent = (ViewGroup) mainView.getParent();
        if (parent != wrapper) {
            if (parent != null) {
                parent.removeView(mainView);
            }
            wrapper.addView(mainView);
        }

        parent = (ViewGroup) container.getParent();
        if (parent != mContentView) {
            if (parent != null) {
                parent.removeView(container);
            }
            mContentView.addView(container, COVER_SCREEN_PARAMS);
        }
        mContentView.setVisibility(View.VISIBLE);
        mUiController.attachSubWindow(tab);
    }

    private void removeTabFromContentView(Tab tab) {
        if (tab == null) return; // 如果tab==null情况不需要要执行移除
        // Remove the container that contains the main WebView.
        WebView mainView = tab.getWebView();
        View container = tab.getViewContainer();
        if (container != null) {
            FrameLayout wrapper =
                    (FrameLayout) container.findViewById(R.id.webview_wrapper);
            if (wrapper != null && mainView != null) {
                wrapper.removeView(mainView);
            }
            if (mContentView != null) {
                mContentView.removeView(container);
            }
        }
        // Remove the container from the content and then remove the
        // WebView from the container. This will trigger a focus change
        // needed by WebView.
        mUiController.endActionMode();
        mUiController.removeSubWindow(tab);
        ErrorConsoleView errorConsole = tab.getErrorConsole(false);
        if (mErrorConsoleContainer != null && errorConsole != null) {
            mErrorConsoleContainer.removeView(errorConsole);
        }
    }

    @Override
    public void onSetWebView(Tab tab, WebView webView) {
        View container = tab.getViewContainer();
        if (container == null) {
            // The tab consists of a container view, which contains the main
            // WebView, as well as any other UI elements associated with the tab.
            container = mActivity.getLayoutInflater().inflate(R.layout.tab,
                    mContentView, false);
            tab.setViewContainer(container);
        }
        if (tab.getWebView() != webView) {
            // Just remove the old one.
            FrameLayout wrapper =
                    (FrameLayout) container.findViewById(R.id.webview_wrapper);
            wrapper.removeView(tab.getWebView());
        }
    }

    /**
     * create a sub window container and webview for the tab
     * Note: this methods operates through side-effects for now
     * it sets both the subView and subViewContainer for the given tab
     *
     * @param tab     tab to create the sub window for
     * @param subView webview to be set as a subwindow for the tab
     */
    @Override
    public void createSubWindow(Tab tab, WebView subView) {
        View subViewContainer = mActivity.getLayoutInflater().inflate(
                R.layout.browser_subwindow, null);
        ViewGroup inner = (ViewGroup) subViewContainer
                .findViewById(R.id.inner_container);
        inner.addView(subView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        final ImageButton cancel = (ImageButton) subViewContainer
                .findViewById(R.id.subwindow_close);
        final WebView cancelSubView = subView;
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	((BrowserWebView) cancelSubView).getWebChromeClient().onCloseWindow(cancelSubView);
            }
        });
        tab.setSubWebView(subView);
        tab.setSubViewContainer(subViewContainer);
    }

    /**
     * Remove the sub window from the content view.
     */
    @Override
    public void removeSubWindow(View subviewContainer) {
        mContentView.removeView(subviewContainer);
        mUiController.endActionMode();
    }

    /**
     * Attach the sub window to the content view.
     */
    @Override
    public void attachSubWindow(View container) {
        if (container.getParent() != null) {
            // already attached, remove first
            ((ViewGroup) container.getParent()).removeView(container);
        }
        mContentView.addView(container, COVER_SCREEN_PARAMS);
    }

    protected void refreshWebView() {
        WebView web = getWebView();
        if (web != null) {
            web.invalidate();
        }
    }

    public View getContentView() {
        return mContentView;
    }

    @Override
    public void showComboView(ComboViews startingView, Bundle extras) {
        Intent intent = new Intent(mActivity, ComboViewActivity.class);
        intent.putExtra(ComboViewActivity.EXTRA_INITIAL_VIEW, startingView.name());
        intent.putExtra(ComboViewActivity.EXTRA_COMBO_ARGS, extras);
        int menuId = R.id.bookmarks_history_button_id;
        if (startingView.name().equals(ComboViews.History)) {
            menuId = R.id.new_bookmark_button_id;
        }
        intent.putExtra(BrowserContract.MENU_ID, menuId);
        Tab t = getActiveTab();
        if (t != null) {
            intent.putExtra(ComboViewActivity.EXTRA_CURRENT_URL, t.getUrl());
        }
        mActivity.startActivityForResult(intent, Controller.COMBO_VIEW);
    }

    @Override
    public void showCustomView(View view, int requestedOrientation,
                               WebChromeClient.CustomViewCallback callback) {
        // if a view already exists then immediately terminate the new one
        mVideoFullScreen = true;
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }

        mOriginalOrientation = mActivity.getRequestedOrientation();
        compositeFullScreenLayer(view);

        mCustomView = view;
        updateStatusBarState(true);
        ((BrowserWebView)getWebView()).setVisibility(View.INVISIBLE);

        mCustomViewCallback = callback;
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onHideCustomView() {
        mVideoFullScreen = false;
        ((BrowserWebView)getWebView()).setVisibility(View.INVISIBLE);
        if (mCustomView == null) return;

        updateStatusBarState(!BrowserSettings.getInstance().getShowStatusBar()); //视频播放返回需重置全屏状态

        removeFullScreenLayer();
        mActivity.setRequestedOrientation(mOriginalOrientation);
    }

    @Override
    public boolean isCustomViewShowing() {
        return mCustomView != null;
    }

    protected void dismissIME() {
        if (mInputManager.isActive()) {
            mInputManager.hideSoftInputFromWindow(mContentView.getWindowToken(),
                    0);
        }
    }

    @Override
    public boolean isWebShowing() {
        return mCustomView == null;
    }

    // -------------------------------------------------------------------------

    protected void updateNavigationState(Tab tab) {
    }

    /**
     * Update the lock icon to correspond to our latest state.
     */
    protected void updateLockIconToLatest(Tab t) {
        if (t != null && t.inForeground()) {
            updateLockIconImage(t.getSecurityState());
        }
    }

    /**
     * Updates the lock-icon image in the title-bar.
     */
    private void updateLockIconImage(SecurityState securityState) {
        Drawable d = null;
        if (securityState == SecurityState.SECURITY_STATE_SECURE) {
            d = mLockIconSecure;
        } else if (securityState == SecurityState.SECURITY_STATE_MIXED
                || securityState == SecurityState.SECURITY_STATE_BAD_CERTIFICATE) {
            // TODO: It would be good to have different icons for insecure vs mixed content.
            // See http://b/5403800
            d = mLockIconMixed;
        }
    }

    // Set the favicon in the title bar.
    protected void setFavicon(Tab tab) {
        if (tab.inForeground()) {
            Bitmap icon = tab.getFavicon();
            // this version not need show icon
//            mNavigationBar.setFavicon(icon);
        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
    }

    // active tabs page

    public void showActiveTabsPage() {
    }

    /**
     * Remove the active tabs page.
     */
    public void removeActiveTabsPage() {
    }

    // menu handling callbacks

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
    }

    @Override
    public void onOptionsMenuOpened() {
    }

    @Override
    public void onExtendedMenuOpened() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onOptionsMenuClosed(boolean inLoad) {
    }

    @Override
    public void onExtendedMenuClosed(boolean inLoad) {
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean inLoad) {
    }

    // error console

    @Override
    public void setShouldShowErrorConsole(Tab tab, boolean flag) {
        if (tab == null) return;
        ErrorConsoleView errorConsole = tab.getErrorConsole(true);
        if (flag) {
            // Setting the show state of the console will cause it's the layout
            // to be inflated.
            if (errorConsole.numberOfErrors() > 0) {
                errorConsole.showConsole(ErrorConsoleView.SHOW_MINIMIZED);
            } else {
                errorConsole.showConsole(ErrorConsoleView.SHOW_NONE);
            }
            if (errorConsole.getParent() != null) {
                mErrorConsoleContainer.removeView(errorConsole);
            }
            // Now we can add it to the main view.
            mErrorConsoleContainer.addView(errorConsole,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            mErrorConsoleContainer.removeView(errorConsole);
        }
    }

    // -------------------------------------------------------------------------
    // Helper function for WebChromeClient
    // -------------------------------------------------------------------------

    @Override
    public Bitmap getDefaultVideoPoster() {
        if (mDefaultVideoPoster == null) {
            mDefaultVideoPoster = BitmapFactory.decodeResource(
                    mActivity.getResources(), R.drawable.default_video_poster);
        }
        return mDefaultVideoPoster;
    }

    @Override
    public View getVideoLoadingProgressView() {
        if (mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            mVideoProgressView = inflater.inflate(
                    R.layout.video_loading_progress, null);
        }
        return mVideoProgressView;
    }

    @Override
    public void showMaxTabsWarning() {
        ToastUtil.showShortToast(mActivity, R.string.max_tabs_warning);
    }

    public WebView getWebView() {
        if (mActiveTab != null) {
            return mActiveTab.getWebView();
        } else {
            return null;
        }
    }

    public BrowserWebView getBrowserWebView() {
        if (mActiveTab == null) {
            return null;
        }
        return (BrowserWebView)mActiveTab.getWebView();
    }

    protected Menu getMenu() {
        return null;
        // DEL:
        //MenuBuilder menu = new MenuBuilder(mActivity);
        //mActivity.getMenuInflater().inflate(R.menu.browser, menu);
        //return menu;
    }

    @Override
    public void onFullscreenChange(boolean enabled) {
//        updateStatusBarState(true);
    }

    public void updateStatusBarState(boolean enabled) {
        Window win = mActivity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;

        if (enabled) {
            winParams.flags |= bits;
            //全屏
            winParams.flags |= bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(mFullScreenImmersiveSetting);
            } else {
                mContentView.setSystemUiVisibility(mFullScreenImmersiveSetting);
            }
        } else {
            winParams.flags &= ~bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        win.setAttributes(winParams);
    }

    public Drawable getFaviconDrawable(Bitmap icon) {
        Drawable[] array = new Drawable[3];
        array[0] = new PaintDrawable(Color.BLACK);
        PaintDrawable p = new PaintDrawable(Color.WHITE);
        array[1] = p;
        if (icon == null) {
            array[2] = mGenericFavicon;
        } else {
            array[2] = new BitmapDrawable(icon);
        }
        LayerDrawable d = new LayerDrawable(array);
        d.setLayerInset(1, 1, 1, 1, 1);
        d.setLayerInset(2, 2, 2, 2, 2);
        return d;
    }

    public boolean isLoading() {
        return mActiveTab != null && mActiveTab.inPageLoad();
    }

    @Override
    public void showWeb(boolean animate) {
        mUiController.hideCustomView();
    }

    static class FullscreenHolder extends FrameLayout implements VideoPlayerLayer.Listener {
        private boolean mLockerState = false;

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (mLockerState && (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                    || event.getKeyCode() == KeyEvent.KEYCODE_MENU)) {
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        @Override
        public void setLockerState(boolean lockerstate) {
            mLockerState = lockerstate;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            // dispatch to the last child first, then one by one to the other child
            if (this.getChildCount() != 0) {
                return this.getChildAt(this.getChildCount() - 1).dispatchTouchEvent(ev);
            }
            return super.dispatchTouchEvent(ev);
        }
    }

    public void setContentViewMarginTop(int margin) {
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) mContentView.getLayoutParams();
        if (params.topMargin != margin) {
            params.topMargin = margin;
            mContentView.setLayoutParams(params);
        }
    }

    @Override
    public boolean blockFocusAnimations() {
        return mBlockFocusAnimations;
    }

    @Override
    public void onVoiceResult(String result) {
    }

    protected boolean showingViewPage() {
        return mMainPageController.isVisible();
    }

    protected void hideViewPager() {
        if (mMainPageController.getInitStatus() != MainPageController.STATUS_EMPTY) {
            mMainPageController.hideViewPager();
        }
    }

    /**
     * 在 {@link #showViewPage(Tab)} 的基础上添加了控制主页显示顶端的参数
     *
     * @param tab
     * @see {ViewPageController#onResume}
     */
    protected void showViewPage(Tab tab) {
        mMainPageController.switchTab(tab);
        mMainPageController.showViewPager();
        mMainPageController.onResumeIfNeed();
    }

    /**
     * 自动切换主界面与社区
     * 调用该方法前需要确认当前tab是home
     */
    public void togglePageSwitch() {
    }

    protected void wrapViewPagerScreen(Tab tab) {
        mMainPageController.wrapScreenshot(tab);
    }

    protected void compositeFullScreenLayer(View view) {
        SystemTintBarUtils.cancelSystemBarImmersive(mActivity); // reset window style
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        mFullscreenContainer = new FullscreenHolder(mActivity);
        // set focus on this view for handling the key event
        mFullscreenContainer.setFocusable(true);
        mFullscreenContainer.setFocusableInTouchMode(true);
        mFullscreenContainer.requestFocus();
        mToolLayer = new FullScreenToolLayer(this);
        mToolLayer.setPreView(view);
        mToolLayer.setListener((VideoPlayerLayer.Listener) mFullscreenContainer);

        BrowserNetworkStateNotifier.getInstance().addEventListener((BrowserNetworkStateNotifier.NetworkStateChangedListener) mToolLayer);

        WebView webview = getWebView();
        if (webview != null) {
            JsInterfaceInject jsobject = mJsObjectMap.get(getWebView());
            if (jsobject != null) {
                jsobject.setListener((VideoPlayerLayer.MediaInfoListener) mToolLayer);
                mToolLayer.beginFullScreen();
            }
        }
        mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        mFullscreenContainer.addView(mToolLayer.getLayer(), COVER_SCREEN_PARAMS);
        mFullscreenContainer.setKeepScreenOn(true);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
    }

    protected void removeFullScreenLayer() {
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        mToolLayer.endFullScreen();
        mCustomViewCallback.onCustomViewHidden(); // call this first, because remove view will cause it destroyed
        if (mToolLayer != null && mToolLayer.getLayer() != null) {
            mFullscreenContainer.removeView(mToolLayer.getLayer());
        }
        if (mCustomView != null) {
            mFullscreenContainer.removeView(mCustomView);
        }
        if (mFullscreenContainer != null) {
            mFullscreenContainer.setKeepScreenOn(false);
        }
        decor.removeView(mFullscreenContainer);
        BrowserNetworkStateNotifier.getInstance().removeEventListener((BrowserNetworkStateNotifier.NetworkStateChangedListener) mToolLayer);
        mToolLayer = null;
        mCustomView = null;
        mFullscreenContainer = null;
        // Restore the status bar immersive style
        ImmersiveController.getInstance().changeStatus();
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        return mToolLayer != null && mToolLayer.dispatchKey(code, event);
    }

    public void loadJsObject(JsInterfaceInject jsObject, boolean force) {
        WebView webview = getWebView();
        if (webview == null) {
            return;
        }

        if (mJsObjectMap != null) {
            if (!mJsObjectMap.containsKey(webview)) {
                mJsObjectMap.put(webview, jsObject);
            } else if (force) {
                mJsObjectMap.remove(webview);
                mJsObjectMap.put(webview, jsObject);
            }
        }
    }

    public void removeJsObjectRef(Tab tab) {
        if (tab == null || mJsObjectMap == null) {
            return;
        }

        WebView webview = tab.getWebView();
        if (webview == null) {
            return;
        }

        if (mJsObjectMap.containsKey(webview)) {
            mJsObjectMap.remove(webview);
        }
    }

    public void shareCurrentPage() {
        mUiController.shareCurrentPage();
    }

    public UiController getController() {
        return mUiController;
    }

    public boolean getVideoFullScreenState() {
        return mVideoFullScreen;
    }

    @Override
    public void openSearchInputView(String url) {
        Intent intent = new Intent(mActivity, UrlSearchActivity.class);
        intent.putExtra(INPUT_SEARCH_URL, url);
        boolean isPrivateBrowsing = false;
        if (mTabControl != null && mTabControl.getCurrentTab() != null) {
            isPrivateBrowsing = mTabControl.getCurrentTab().isPrivateBrowsingEnabled();
        }
        intent.putExtra(INPUT_SEARCH_INCOGNITO, isPrivateBrowsing);
        mActivity.startActivityForResult(intent, Controller.URL_SEARCH);
        mActivity.overridePendingTransition(R.anim.url_search_input_enter, R.anim.url_search_input_exit);
    }

    public void onPageLoadStarted(Tab tab) {
        if (mToolbar != null) {
            mToolbar.onPageLoadStarted(tab);
        }
    }

    public void onPageLoadFinished(Tab tab) {
        if (mToolbar != null) {
            mToolbar.onPageLoadFinished(tab);
        }
    }

    public void onPageLoadStopped(Tab tab) {
        if (mToolbar != null) {
            mToolbar.onPageLoadStopped(tab);
        }
    }

    public boolean getIsSearchResultPage() {
        return mToolbar != null && mToolbar.getIsSearchResultPage();
    }

    public void updateToolbarStyle() {
        if (mToolbar != null) {
            mToolbar.updateToolbarStyle();
        }
    }

    public void openSelectSearchEngineView(View view) {
        if (mMainContent == null) {
            return;
        }
        if (mSelectSearchEngine != null) {
            dimissSearchEngine();
        }
        mSelectSearchEngine = new SelectSearchEngine(getActivity(), this, view);
        mMainContent.addView(mSelectSearchEngine, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void changeSearchEngine() {
        if (mMainPageController != null) {
            mMainPageController.updateDefaultSearchEngine();
        }
    }

    public void dimissSearchEngine() {
        if (mMainContent != null && mSelectSearchEngine != null) {
            mMainContent.removeView(mSelectSearchEngine);
            mSelectSearchEngine = null;
        }
    }

    public void showLeadPage(String url, Map<String, String> headers) {
        if (mMainContent != null && mActivity != null) {
            mLeadPageView = new LeadPageView(mActivity, url, headers, this);
            mLeadPageView.setClickable(true);
            mMainContent.addView(mLeadPageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            SharedPreferencesUtils.put(SharedPreferencesUtils.SHOW_LEADPAGE, false);
            BrowserHandler.getInstance().handlerPostDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mLeadPageView != null) {
                        mLeadPageView.startBackHomeAnima();
                    }
                }
            }, 500);
        }
    }

    public void dimissLeadPage() {
        if (mMainContent != null && mLeadPageView != null) {
            mMainContent.removeView(mLeadPageView);
            mLeadPageView = null;
        }
    }


    @Override
    public void updateDefaultEngineIcon() {
        if (mMainPageController != null) {

            mMainPageController.updateDefaultSearchEngine();
        }
    }

    @Override
    public void onDestroy() {
        SearchEngines.getInstance(mActivity).unregisterIconUpdateListener(this);
    }

    @Override
    public void onToolbarStateChanged() {
        if (mKeyboardHeight == 0) {
            changeWebViewHeight();
        }
    }

    public ToolBar getToolbar() {
        return mToolbar;
    }

}
