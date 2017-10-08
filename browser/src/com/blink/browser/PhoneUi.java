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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Message;
import android.support.annotation.Keep;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.handler.BrowserHandler;
import com.blink.browser.homepages.ImmersiveController;
import com.blink.browser.homepages.OnSearchUrl;
import com.blink.browser.homepages.WebViewStatusChange;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.OtherAPPUtils;
import com.blink.browser.util.SharedPreferencesUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Ui for regular phone screen sizes
 */
public class PhoneUi extends BaseUi implements View.OnClickListener, TabControl.OnTabCountChangeListener,
        OnSearchUrl, PopupMenu.OnMenuItemClickListener {

    private static final String LOGTAG = "PhoneUi";
    private static final int MSG_INIT_NAVSCREEN = 100;
    private static final long ONE_DAY_TIME = 24 * 60 * 60 * 1000l;//一天是的时间戳
    private static final long DEFAULT_DAYS = 7;

    private NavScreen mNavScreen;

    private int mActionBarHeight;
    boolean mShowNav = false;
    private ComboHomeViews mComboStatus = ComboHomeViews.VIEW_HIDE_NATIVE_PAGER;
    private AnimScreen mAnimScreen;

    /**
     * @param browser
     * @param controller
     */
    public PhoneUi(Activity browser, UiController controller) {
        super(browser, controller);
        setUseQuickControls(BrowserSettings.getInstance().useQuickControls());
        TypedValue heightValue = new TypedValue();
        browser.getTheme().resolveAttribute(
                android.R.attr.actionBarSize, heightValue, true);
        mActionBarHeight = TypedValue.complexToDimensionPixelSize(heightValue.data,
                browser.getResources().getDisplayMetrics());
        mUiController.getTabControl().registerTabChangeListener(this);
        ImmersiveController.init(this, mTabControl);
        mMainPageController.registerSearchListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 如没有任何的可显示的网页，则显示主页
        Tab current = mTabControl.getCurrentTab();
        if (current == null) {
            mUiController.openTabToHomePage();
        } else if (current.isNativePage()
                && (mComboStatus == ComboHomeViews.VIEW_HIDE_NATIVE_PAGER || mComboStatus == ComboHomeViews
                .VIEW_NATIVE_PAGER)) {
            if (mMainPageController.getInitStatus() == MainPageController.STATUS_EMPTY) {
                mMainPageController.initRootView();
            }
            mUiController.loadNativePage(current);
        }
        if (!BrowserSettings.getInstance().useTempExitFullscreen()) {
            mUiController.setFullscreen(BrowserSettings.getInstance().useFullscreen());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMainPageController.onPause();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean onBackKey() {

        if (showingNavScreen()) {
            Tab currTab = mUiController.getTabControl().getCurrentTab();
            if (currTab == null) {
                mNavScreen.switchNavScreenNormal();
                return true;
            } else if (currTab != null && currTab.isNativePage()) {
                panelSwitchHome(mUiController.getTabControl().getTabPosition(currTab), true);
                return true;
            } else if (currTab != null) {
                currTab.resume();
            }

            mComboStatus = ComboHomeViews.VIEW_WEBVIEW;
            ImmersiveController.getInstance().changeStatus();
            mNavScreen.close(mUiController.getTabControl().getTabPosition(currTab));
            return true;
        }

        if (mMainPageController.onBackKey()) {
            return true;
        }


        return super.onBackKey();
    }

    public boolean showingNavScreen() {
        return mNavScreen != null && mNavScreen.getVisibility() == View.VISIBLE;
    }

    public boolean showingNavScreenForExit() {
        return mNavScreen != null && mShowNav;
    }


    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        return super.dispatchKey(code, event);
    }

    @Override
    public void onProgressChanged(Tab tab) {
        super.onProgressChanged(tab);
    }

    @Override
    public void setActiveTab(final Tab tab) {
        super.setActiveTab(tab);
        //if at Nav screen show, detach tab like what showNavScreen() do.
        if (mShowNav) {
            detachTab(mActiveTab);
        }

        BrowserWebView view = (BrowserWebView)tab.getWebView();
        // TabControl.setCurrentTab has been called before this,
        // so the tab is guaranteed to have a webview
        if (view == null) {
            Log.e(LOGTAG, "active tab with no webview detected");
            return;
        }
        // Request focus on the top window.
        if (mUseQuickControls) {
            mPieControl.forceToTop(mContentView);
        }
        updateLockIconToLatest(tab);
    }

    // menu handling callbacks

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuState(mActiveTab, menu);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
        //hideTitleBar();
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean inLoad) {
        //if (inLoad) {
        //    showTitleBar();
        //}
    }

    // action mode callbacks

    @Override
    public void onActionModeStarted(ActionMode mode) {
        //this is system code show actionbar need saved, now is hide.
//        if (!isEditingUrl()) {
//            hideTitleBar();
//        } else {
//            mTitleBar.animate().translationY(mActionBarHeight);
//        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
    }

    @Override
    public boolean isWebShowing() {
        return super.isWebShowing() && mComboStatus == ComboHomeViews.VIEW_WEBVIEW;
    }

    @Override
    public void showWeb(boolean animate) {
        super.showWeb(animate);
        hideNavScreen(mUiController.getTabControl().getCurrentPosition(), animate);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    void showNavScreen() {
        mShowNav = true;
        mUiController.setBlockEvents(true);
        final Tab currTab = mTabControl.getCurrentTab();
        if (currTab != null) {
            if (currTab.isNativePage()) {
                wrapViewPagerScreen(currTab);
            }
            currTab.capture();
        }

        //  SystemTintBarUtils.setSystemBarColor(mActivity, R.color.browser_background_end_color);
        if (mNavScreen == null) {
            mNavScreen = new NavScreen(mActivity, mUiController, this);
            mCustomViewContainer.addView(mNavScreen, COVER_SCREEN_PARAMS);
        } else {
            mNavScreen.setVisibility(View.VISIBLE);
            mNavScreen.setAlpha(1f);
            if (mUiController.getTabControl().isIncognitoShowing()) {
                mNavScreen.showIncognitoTabMode();
            } else {
                mNavScreen.showNormalTabMode();
            }
            mNavScreen.setShowNavScreenAnimating(true);
            mNavScreen.refreshAdapter();
        }
        mNavScreen.setBlockEvents(false);
        if (mAnimScreen == null) {
            mAnimScreen = new AnimScreen(mActivity);
        }
        FrameLayout currContainer = null;
        if (currTab != null && currTab.isNativePage()) {
            currContainer = (FrameLayout) getHomeContainer();
        } else {
            currContainer = mContentView;
        }
        if (currTab != null) {
            Bitmap capture = currTab.getScreenshot();
            if (capture == null && !currTab.getCaptureSuccess()) {
                capture = mUiController.getTabControl().getHomeCapture();
            }
            mAnimScreen.set(capture);
        }
        if (mAnimScreen.mMain.getParent() == null) {
            mCustomViewContainer.addView(mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        mCustomViewContainer.setVisibility(View.VISIBLE);
        mCustomViewContainer.bringToFront();
        int fromLeft = 0;
        int fromTop = 0;
        int fromRight = getMainContent().getWidth();
        int fromBottom = getMainContent().getHeight();
        int width = mActivity.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_width);
        int height = mActivity.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_height);
        int ntth = mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_titleheight);
        int position = mTabControl.getCurrentPosition();
        if (position > 0) {
            width = (int) (width * 1.05);
            height = (int) (height * 1.05);
            ntth = (int) (ntth * 1.05);
        }
        int toLeft = (int) ((getMainContent().getWidth() - width) / 2);
        int toTop = (getMainContent().getHeight() - mActivity.getResources().getDimensionPixelSize(R.dimen.toolbar_height) + ntth - height) / 2;
        if (mActivity.getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            toTop += mActivity.getResources().getDimensionPixelSize(R.dimen.navscreen_tab_views_offset);
            if (position > 0) {
                toTop += mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_height) * 1.05/ 2;
            }
        } else {
            if (position > 0) {
                toLeft += mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_width) * 1.05/ 2;
            }
        }
        int toRight = toLeft + width;
        int toBottom = toTop + height;
        int captureWidth = DisplayUtil.getScreenWidth(mActivity) < DisplayUtil.getScreenHeight(mActivity) ? DisplayUtil.getScreenWidth(mActivity) : DisplayUtil.getScreenHeight(mActivity);
        captureWidth /= 2;
        float scaleFactor = width / (float) (captureWidth);
        float scaleStart = getMainContent().getWidth() / (float) (captureWidth);
        mAnimScreen.setScaleFactor(scaleStart);
        mAnimScreen.mMain.layout(0, 0, getMainContent().getWidth(),
                getMainContent().getHeight());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(fromRight - fromLeft, fromBottom - fromTop);
        layoutParams.setMargins(0, 0, 0, 0);
        mAnimScreen.mContent.setLayoutParams(layoutParams);
        layoutParams = new RelativeLayout.LayoutParams(fromRight - fromLeft, ntth);
        layoutParams.setMargins(0, 0 - ntth, 0, 0);
        mAnimScreen.mTitle.setLayoutParams(layoutParams);
        float scaleX = width / (float) (getMainContent().getWidth() < getMainContent().getHeight() ? getMainContent().getWidth() : getMainContent().getHeight());
        float scaleY = ntth / mActivity.getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        mToolbar.updateToolBarVisibility(true, false);
        if (currTab != null && currTab.isPrivateBrowsingEnabled()) {
            mAnimScreen.mTitle.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.nav_tab_title_incognito_bg));
            mAnimScreen.mTitle.setTextColor(ContextCompat.getColor(mActivity, R.color.white));
        } else {
            mAnimScreen.mTitle.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.nav_tab_title_normal_bg));
            mAnimScreen.mTitle.setTextColor(ContextCompat.getColor(mActivity, R.color.normal_text_color));
        }
        currContainer.setVisibility(View.GONE);
        detachTab(mActiveTab);
        AnimatorSet inanim = new AnimatorSet();
        ObjectAnimator tx = ObjectAnimator.ofInt(mAnimScreen.mContent, "left",
                fromLeft, toLeft);
        ObjectAnimator ty;
        ty = ObjectAnimator.ofInt(mAnimScreen.mContent, "top",
                fromTop, toTop);
        ObjectAnimator tr = ObjectAnimator.ofInt(mAnimScreen.mContent, "right",
                fromRight, toRight);
        ObjectAnimator tb = ObjectAnimator.ofInt(mAnimScreen.mContent, "bottom",
                fromBottom, toBottom);
        ObjectAnimator sf = ObjectAnimator.ofFloat(mAnimScreen, "scaleFactor",
                scaleStart, scaleFactor);
        ObjectAnimator ttx = ObjectAnimator.ofInt(mAnimScreen.mTitle, "left",
                fromLeft, toLeft);
        ObjectAnimator tty = ObjectAnimator.ofInt(mAnimScreen.mTitle, "top",
                fromTop - ntth, toTop - ntth);
        ObjectAnimator ttr = ObjectAnimator.ofInt(mAnimScreen.mTitle, "right",
                fromRight, toRight);
        ObjectAnimator ttb;
        ttb = ObjectAnimator.ofInt(mAnimScreen.mTitle, "bottom",
                fromTop, toTop);
        ObjectAnimator sx = ObjectAnimator.ofFloat(mAnimScreen.mTitle, "scaleX",
                1f, scaleX);
        ObjectAnimator sy = ObjectAnimator.ofFloat(mAnimScreen.mTitle, "scaleY",
                1f, scaleY);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mAnimScreen.mTitle, "alpha",
                0f, 1f);
        alpha.setDuration(1);
        ValueAnimator ntabbar = ValueAnimator.ofFloat(0f, 1f);
        ntabbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (currTab != null && currTab.isPrivateBrowsingEnabled()) {
                    mNavScreen.getTabBar().setBackgroundColor(com.blink.browser.util.AnimationUtils.getColor((float) valueAnimator.getAnimatedValue(),
                            mActivity.getResources().getColor(R.color.incognito_bg_color), mActivity.getResources().getColor(R.color.navscreen_backgroud_color)));
                } else {
                    mNavScreen.getTabBar().setBackgroundColor(com.blink.browser.util.AnimationUtils.getColor((float) valueAnimator.getAnimatedValue(),
                            mActivity.getResources().getColor(R.color.toolbar_background_color), mActivity.getResources().getColor(R.color.navscreen_backgroud_color)));
                }
            }
        });
        inanim.playTogether(tx, ty, tr, tb, sf, ttx, tty, ttr, ttb, ntabbar);
        inanim.setDuration(mActivity.getResources().getInteger(R.integer.tab_animation_duration));
        inanim.setInterpolator(new FastOutSlowInInterpolator());
        AnimatorSet combo = new AnimatorSet();
        if (currTab != null && currTab.isNativePage()) {
            combo.playSequentially(alpha, inanim);
        } else {
            combo = inanim;
        }
        combo.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator anim) {
                if (mAnimScreen != null && mAnimScreen.mMain.getParent() != null) {
                    ((ViewGroup) mAnimScreen.mMain.getParent()).removeView(mAnimScreen.mMain);
                }
                finishAnimationIn();
                mNavScreen.setShowNavScreenAnimating(false);
                mUiController.setBlockEvents(false);
            }
        });
        combo.start();
    }

    private void setAnimation(View viewToAnimate) {
        if (viewToAnimate != null) {
            viewToAnimate.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
        }
    }

    private void finishAnimationIn() {
        if (showingNavScreen()) {
            // notify accessibility manager about the screen change
            mNavScreen.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }

    void hideNavScreen(int position, boolean animate) {
        mShowNav = false;
        final Tab tab = mUiController.getTabControl().getTab(position);
        if (!showingNavScreen()) {
            mToolbar.updateToolBarVisibility();
            return;
        }
        // SystemTintBarUtils.setSystemBarColor(mActivity);
        if ((tab == null) || !animate) {
            if (tab != null && !tab.isNativePage()) {
                if (tab != null) {
                    setActiveTab(tab);
                } else if (mTabControl.getTabCount() > 0) {
                /*
                如果当前的TabController中tab都是HomeTab的情况会出现getCurrentTab()==null
                 */
                    if (mTabControl.getCurrentTab() == null ||
                            mTabControl.getCurrentTab().isNativePage()) {
                        return;
                    }
                    // use a fallback tab
                    setActiveTab(mTabControl.getCurrentTab());
                }
            }
            mContentView.setVisibility(View.VISIBLE);
            mUiController.setActiveTab(tab);
            finishAnimateOut();
            mToolbar.updateToolBarVisibility();
            return;
        }
        View tabview = mNavScreen.getTabView(tab);
        mNavScreen.setBlockEvents(true);
        mUiController.setBlockEvents(true);
        mUiController.setActiveTab(tab);
        final FrameLayout currContainer;
        if (tab.isNativePage()) {
            currContainer = (FrameLayout) getHomeContainer();
        } else {
            currContainer = mContentView;
        }
        if (mAnimScreen == null) {
            mAnimScreen = new AnimScreen(mActivity);
        } else {
            mAnimScreen.mMain.setAlpha(1f);
        }

        Bitmap capture = tab.getScreenshot();
        if (capture == null && !tab.getCaptureSuccess()) {
            capture = mUiController.getTabControl().getHomeCapture();
        }
        mAnimScreen.set(capture);
        if (mAnimScreen.mMain.getParent() == null) {
            mCustomViewContainer.addView(mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        mCustomViewContainer.bringToFront();
        mToolbar.updateToolBarVisibility(true, false);
        int toLeft = 0;
        int toTop = 0;
        int toRight = getMainContent().getWidth();
        int width = (tabview == null ? getMainContent().getWidth() : tabview.getWidth()) - mActivity.getResources().getDimensionPixelSize(R.dimen.tab_card_item_padding) * 2;
        int ntth = mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_titleheight);
        int height = (tabview == null ? getMainContent().getHeight() : tabview.getHeight()) - mActivity.getResources().getDimensionPixelSize(R.dimen.tab_card_item_padding) * 2 - ntth;
        int fromLeft = (tabview == null ? 0 : tabview.getLeft()) + mActivity.getResources().getDimensionPixelSize(R.dimen.tab_card_item_padding);
        int fromTop = (tabview == null ? getMainContent().getHeight() : tabview.getTop()) + ntth + mActivity.getResources().getDimensionPixelSize(R.dimen.tab_card_item_padding);
        int fromRight = fromLeft + width;
        int fromBottom = fromTop + height;
        int captureWidth = DisplayUtil.getScreenWidth(mActivity) < DisplayUtil.getScreenHeight(mActivity) ? DisplayUtil.getScreenWidth(mActivity) : DisplayUtil.getScreenHeight(mActivity);
        captureWidth /= 2;
        float scaleEnd = getMainContent().getWidth() / (float) (captureWidth);
        float scaleFactor = (float) width / (captureWidth);
        mAnimScreen.setScaleFactor(scaleFactor);
        int toBottom = toTop + getMainContent().getHeight();
        mAnimScreen.mMain.layout(toLeft, toTop, getMainContent().getWidth(),
                toBottom);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        layoutParams.setMargins(fromLeft, fromTop, fromRight, fromBottom);
        mAnimScreen.mContent.setLayoutParams(layoutParams);
        layoutParams = new RelativeLayout.LayoutParams(width, ntth);
        layoutParams.setMargins(fromLeft, fromTop - ntth, fromRight, fromTop);
        mAnimScreen.mTitle.setLayoutParams(layoutParams);
        if (tab.isPrivateBrowsingEnabled()) {
            mAnimScreen.mTitle.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.nav_tab_title_incognito_bg));
            mAnimScreen.mTitle.setTextColor(ContextCompat.getColor(mActivity, R.color.white));
        } else {
            mAnimScreen.mTitle.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.nav_tab_title_normal_bg));
            mAnimScreen.mTitle.setTextColor(ContextCompat.getColor(mActivity, R.color.normal_text_color));
        }
        AnimatorSet set1 = new AnimatorSet();
        ObjectAnimator l = ObjectAnimator.ofInt(mAnimScreen.mContent, "left",
                fromLeft, toLeft);
        ObjectAnimator t;
        t = ObjectAnimator.ofInt(mAnimScreen.mContent, "top",
                fromTop, toTop);
        ObjectAnimator r = ObjectAnimator.ofInt(mAnimScreen.mContent, "right",
                fromRight, toRight);
        ObjectAnimator b = ObjectAnimator.ofInt(mAnimScreen.mContent, "bottom",
                fromBottom, toBottom);
        ObjectAnimator scale = ObjectAnimator.ofFloat(mAnimScreen, "scaleFactor",
                scaleFactor, scaleEnd);
        ObjectAnimator ttx = ObjectAnimator.ofInt(mAnimScreen.mTitle, "left",
                fromLeft, toLeft);
        ObjectAnimator tty = ObjectAnimator.ofInt(mAnimScreen.mTitle, "top",
                fromTop - ntth, toTop);
        ObjectAnimator ttr = ObjectAnimator.ofInt(mAnimScreen.mTitle, "right",
                fromRight, toRight);
        ObjectAnimator ttb;
        ttb = ObjectAnimator.ofInt(mAnimScreen.mTitle, "bottom",
                fromTop, toTop);

        mAnimScreen.mTitle.setAlpha(1);
        if (tab.isNativePage()) {
            mAnimScreen.mTitle.setText(R.string.home_page);
        } else {
            mAnimScreen.mTitle.setText(tab.getUrl());
        }
        if (mNavScreen.getTabBar().getTop() < fromBottom) {
            ValueAnimator ntabbar = ValueAnimator.ofFloat(1f, 0f);
            ntabbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    if (tab.isPrivateBrowsingEnabled()) {
                        mNavScreen.getTabBar().setBackgroundColor(com.blink.browser.util.AnimationUtils.getColor((float) valueAnimator.getAnimatedValue(),
                                mActivity.getResources().getColor(R.color.incognito_bg_color), mActivity.getResources().getColor(R.color.navscreen_backgroud_color)));
                    } else {
                        mNavScreen.getTabBar().setBackgroundColor(com.blink.browser.util.AnimationUtils.getColor((float) valueAnimator.getAnimatedValue(),
                                mActivity.getResources().getColor(R.color.toolbar_background_color), mActivity.getResources().getColor(R.color.navscreen_backgroud_color)));
                    }
                }
            });
            ntabbar.setDuration(mActivity.getResources().getInteger(R.integer.navscreen_toolbar_hide));
            ntabbar.start();
        }
        set1.playTogether(l, t, r, b, scale, ttx, tty, ttr, ttb);
        set1.setDuration(mActivity.getResources().getInteger(R.integer.tab_animation_duration));
        set1.setInterpolator(new DecelerateInterpolator());
        set1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                currContainer.setVisibility(View.VISIBLE);
                if (mAnimScreen != null && mAnimScreen.mMain.getParent() != null) {
                    ((ViewGroup) mAnimScreen.mMain.getParent()).removeView(mAnimScreen.mMain);
                }
                mAnimScreen = null;
                finishAnimateOut();
                mUiController.setBlockEvents(false);
            }
        });
        set1.start();
    }

    public void createNewTabWithNavScreen(final boolean incognito) {
        mShowNav = false;
        if (!showingNavScreen()) {
            mToolbar.updateToolBarVisibility();
            return;
        }
        mNavScreen.setBlockEvents(true);
        mUiController.setBlockEvents(true);
        final FrameLayout currContainer = (FrameLayout) getHomeContainer();
        currContainer.setVisibility(View.VISIBLE);
        if (mAnimScreen == null) {
            mAnimScreen = new AnimScreen(mActivity);
        } else {
            mAnimScreen.mMain.setAlpha(1f);
        }
        mAnimScreen.set(mUiController.getTabControl().getHomeCapture());
        if (mAnimScreen.mMain.getParent() == null) {
            mCustomViewContainer.addView(mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        mCustomViewContainer.bringToFront();
        final Tab homeTab = openNewTab(incognito);
        mTabControl.setCurrentTab(homeTab);
        mToolbar.updateToolBarVisibility(true, false);
        mToolbar.setToolbarStyle(mTabControl.isIncognitoShowing(), true);
        int toLeft = 0;
        int toTop = 0;
        int toRight = getMainContent().getWidth();
        int width = mActivity.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_width) / 3;
        int ntth = mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_titleheight);
        int height = mActivity.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_height) / 3;
        int fromLeft = getMainContent().getWidth() - width;
        int fromRight = fromLeft + width;
        int fromBottom = getMainContent().getHeight();
        int fromTop = fromBottom - height;
        int captureWidth = DisplayUtil.getScreenWidth(mActivity) < DisplayUtil.getScreenHeight(mActivity) ? DisplayUtil.getScreenWidth(mActivity) : DisplayUtil.getScreenHeight(mActivity);
        captureWidth /= 2;
        float scaleEnd = getMainContent().getWidth() / (float) (captureWidth);
        float scaleFactor = (float) width / (captureWidth);
        mAnimScreen.setScaleFactor(scaleFactor);
        int toBottom = toTop + getMainContent().getHeight();
        mAnimScreen.mMain.layout(toLeft, toTop, getMainContent().getWidth(),
                toBottom);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        layoutParams.setMargins(fromLeft, fromTop, fromRight, fromBottom);
        mAnimScreen.mContent.setLayoutParams(layoutParams);
        mAnimScreen.mTitle.setVisibility(View.GONE);
        AnimatorSet set1 = new AnimatorSet();
        ObjectAnimator l = ObjectAnimator.ofInt(mAnimScreen.mContent, "left",
                fromLeft, toLeft);
        ObjectAnimator t;
        t = ObjectAnimator.ofInt(mAnimScreen.mContent, "top",
                fromTop, toTop);
        ObjectAnimator r = ObjectAnimator.ofInt(mAnimScreen.mContent, "right",
                fromRight, toRight);
        ObjectAnimator b = ObjectAnimator.ofInt(mAnimScreen.mContent, "bottom",
                fromBottom, toBottom);
        ObjectAnimator scale = ObjectAnimator.ofFloat(mAnimScreen, "scaleFactor",
                scaleFactor, scaleEnd);
//        ObjectAnimator alphaA = ObjectAnimator.ofFloat(mAnimScreen.mMain, "alpha",
//                0f, 1f);
        set1.playTogether(l, t, r, b, scale);
        set1.setDuration(mActivity.getResources().getInteger(R.integer.tab_animation_duration));
        set1.setInterpolator(new FastOutSlowInInterpolator());

        set1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                if (mAnimScreen != null && mAnimScreen.mMain.getParent() != null) {
                    ((ViewGroup) mAnimScreen.mMain.getParent()).removeView(mAnimScreen.mMain);
                }
                mAnimScreen = null;
                finishAnimateOut();
                if (homeTab != null) {
                    panelSwitchHome(mUiController.getTabControl().getTabPosition(homeTab), true);
                }
                mUiController.setBlockEvents(false);
            }
        });
        set1.start();

    }

    private void finishAnimateOut() {
        mTabControl.setOnThumbnailUpdatedListener(null);
        mNavScreen.setVisibility(View.GONE);
        mCustomViewContainer.setAlpha(1f);
        mCustomViewContainer.setVisibility(View.GONE);
    }

    public Tab openNewTab(boolean incognito) {
        Tab tab;
        if (!mUiController.getTabControl().canCreateNewTab(incognito)) {
            showMaxTabsWarning();
            return null;
        }

        mUiController.setBlockEvents(true);
        if (!incognito) {
            tab = mUiController.openTab(BrowserSettings.getInstance().getHomePage(), false, false, true);
        } else {
            tab = mUiController.openTab(BrowserSettings.getInstance().getHomePage(), true, false, true);
        }
        mUiController.setBlockEvents(false);
        if (tab != null) {
            tab.setNativePage(true);
        }
        return tab;
    }


    @Override
    public boolean needsRestoreAllTabs() {
        return false;
    }

    public void toggleNavScreen() {
        if (!showingNavScreen()) {
            showNavScreen();
        } else {
            hideNavScreen(mUiController.getTabControl().getCurrentPosition(), false);
        }
    }

    /**
     * 切换不同的面板模式
     *
     * @param status BaseUi.VIEW_NATIVE_PAGER,BaseUi.VIEW_NAV_SCREEN,BaseUi.VIEW_WEBVIEW
     */
    public void panelSwitch(ComboHomeViews status, int position, boolean mAnimating) {
        Tab tab;
        boolean updateToolbarStyle = true;
        switch (status) {
            case VIEW_NAV_SCREEN:
                tab = mUiController.getCurrentTab();
                if (tab != null) {
                    tab.stop();
                }
                hideViewPager();
                if (!showingNavScreen()) {
                    showNavScreen();
                }
                updateToolbarStyle = false;
                mComboStatus = ComboHomeViews.VIEW_NAV_SCREEN;
                ImmersiveController.getInstance().changeStatus();
                break;
            case VIEW_WEBVIEW:
                hideViewPager();
                hideNavScreen(position, mAnimating);
                updateToolbarStyle = false;
                mToolbar.setToolbarStyle(mTabControl.isIncognitoShowing(), false);
                try {
                    mToolbar.setWebView((BrowserWebView)mTabControl.getCurrentTab().getWebView());
                    mToolbar.updateToolbarBtnState();
                } catch (Exception e) {
                }
                if (mComboStatus == ComboHomeViews.VIEW_NATIVE_PAGER) {
                    attachTab(mUiController.getCurrentTab());
                }
                tab = mUiController.getCurrentTab();
                if (tab != null) {
                    tab.setNativePage(false);
                    mComboStatus = ComboHomeViews.VIEW_WEBVIEW;
                    tab.resume();
                }
                mDivider.setVisibility(View.VISIBLE);
                break;
            default:
                /**
                 * VIEW_HIDE_NATIVE_PAGER　的作用:
                 * resume前处于主界面时，当从其他的Activity切换过来的时候，没有切换到网页
                 */
                hideViewPager();
                mComboStatus = ComboHomeViews.VIEW_HIDE_NATIVE_PAGER;
                ImmersiveController.getInstance().changeStatus();
                break;
        }
        if (updateToolbarStyle) {
            mToolbar.setToolbarStyle(mTabControl.isIncognitoShowing(), mTabControl.getCurrentTab().isNativePage());
            mToolbar.updateToolBarVisibility();
            mUiController.updateToolBarItemState();
        }
       /* try {
            updateStatusBarState(!BrowserSettings.getInstance().useTempExitFullscreen() && BrowserSettings
                    .getInstance().useFullscreen() && !mTabControl.getCurrentTab().isNativePage());
        } catch (Exception e) {
        }*/
    }

    /**
     * 打开主页的hometab(包装了主页的tab)
     *
     * @param position
     * @param mAnimating
     */
    public void panelSwitchHome(int position, boolean mAnimating) {
        final Tab homeTab = mUiController.getTabControl().getTab(position);
        if (homeTab != null) {
            if (homeTab.getWebView() != null) {
                homeTab.stop();
                homeTab.getWebView().stopLoading();
                homeTab.getWebView().clearFocus();
                detachTab(homeTab);
            }
            homeTab.setNativePage(true);
        }

        /**
         * 切换到ViewPage时，需要隐藏navbar,目的时使其失去焦点
         */
        showViewPage(homeTab);
        mTabControl.setCurrentTab(homeTab);
        boolean incognitoShowing = mTabControl.isIncognitoShowing();
        mToolbar.setToolbarStyle(mTabControl.isIncognitoShowing(), true);
        mMainPageController.onIncognito(incognitoShowing);
        mToolbar.switchHome();
        mUiController.updateToolBarItemState();
        //   mUiController.notifyPageChanged();

        mComboStatus = ComboHomeViews.VIEW_NATIVE_PAGER;

        ImmersiveController.getInstance().changeStatus();
//        updateStatusBarState(false);
        hideNavScreen(position, mAnimating);
        mDivider.setVisibility(View.GONE);

    }

    @Override
    public void showAndOpenUrl(String url, boolean isNewTab) {
        if (!isNewTab) {
            Tab tab = mTabControl.getCurrentTab();
            if (tab != null && tab.isNativePage()) {
                onSelect(url, false);
            }
        } else {
            hideViewPager();
            hideNavScreen(mTabControl.getCurrentPosition(), false);
        }

    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.voice_icon:
                mUiController.startVoiceRecognizer();
                break;
        }
    }

    @Override
    public void onVoiceResult(String result) {
        super.onVoiceResult(result);
        onSelect(result, false);
    }

    @Override
    public void onSelect(String url, boolean isInputUrl) {
        onSelect(url, isInputUrl, "");
    }

    public void onSelect(String url, boolean isInputUrl, String inputWord) {
        Tab t = mTabControl.getCurrentTab();
        if (t == null) return;
        if (t.isNativePage()) {
            mTabControl.recreateWebView(t);
        }
        if (isInputUrl) {
            t.setWebViewStatusChange(new WebViewStatusChange(inputWord));
        }
        t.setNativePage(false);
        mUiController.openTab(t, url);
    }

    @Override
    public void onSelectIncognito(String url) {
        Tab t = mUiController.openTab(url, false, false, false);
    }

    @Override
    public void onTabCountUpdate(int tabCount) {
        if (mNavScreen != null) {
            mNavScreen.onTabCountUpdate(tabCount);
        }
    }

    @Override
    public void onQrUrl(String url) {
        onSelect(url, false);
    }

    public void openViewPage() {
        if (!mUiController.getTabControl().canCreateNewTab()) {
            showMaxTabsWarning();
            return;
        }

        mUiController.setBlockEvents(true);
        mUiController.openTabToHomePage();
        mUiController.setBlockEvents(false);
    }

    public ComboHomeViews getComboStatus() {
        return this.mComboStatus;
    }

    static class AnimScreen {

        private View mMain;
        private ImageView mContent;
        private TextView mTitle;
        @Keep
        private float mScale;

        public AnimScreen(Context ctx) {
            mMain = LayoutInflater.from(ctx).inflate(R.layout.anim_screen, null);
            mTitle = (TextView) mMain.findViewById(R.id.title_anim);
            mContent = (ImageView) mMain.findViewById(R.id.content);
            mContent.setScaleType(ImageView.ScaleType.MATRIX);
            mContent.setImageMatrix(new Matrix());
            mScale = 1.0f;
            setScaleFactor(getScaleFactor());
        }

        public void set(Bitmap image) {
            mContent.setImageBitmap(image);
        }

        @Keep
        private void setScaleFactor(float sf) {
            mScale = sf;
            Matrix m = new Matrix();
            m.postScale(sf, sf);
            mContent.setImageMatrix(m);
        }

        @Keep
        private float getScaleFactor() {
            return mScale;
        }

    }

}
