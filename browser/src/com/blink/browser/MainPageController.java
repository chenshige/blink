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

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.homepages.HomePageHeadView;
import com.blink.browser.homepages.OnSearchUrl;
import com.blink.browser.homepages.ScreenRotateable;
import com.blink.browser.homepages.navigation.WebNavigationView;
import com.blink.browser.util.AnimationUtils;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.widget.AnimationListener;

import static android.view.animation.AnimationUtils.loadAnimation;

public class MainPageController implements View.OnClickListener, ScreenRotateable {

    public static final int STATUS_EMPTY = 0;
    public static final int STATUS_DONE = 1;

    private static class TransferAnimArgs {
        static final int LOGO_DISMISS_TIME = 136;
        static final int SEARCH_BAR_DISMISS_TIME = 308;
        static final int SEARCH_BAR_DISMISS_DELAY = 72;
        static final int BACKGROUND_APPEAR_TIME = 216;
        static final int BACKGROUND_DISMISS_TIME = 216;
        static final int HEAD_VIEW_APPEAR_DELAY = 400;
        static final int HEAD_VIEW_APPEAR_TIME = 50;
    }

    private Activity mContext;
    private FrameLayout mHomePage;
    private View mBlackBackground;
    private WebNavigationView mWebNavigationViewView;
    private Tab mTab;
    private BaseUi mUi;
    private UiController mController;
    public FrameLayout mSearchEngineLayout;
    private int mInitState = STATUS_EMPTY;
    private OnSearchUrl mListener;

    private HomePageHeadView mHeadTitleView;

    private boolean mDataInit = false;

    private boolean mIncognito;

    public MainPageController(UiController controller, Activity context) {
        mInitState = STATUS_EMPTY;
        mController = controller;
        this.mContext = context;
    }

    void setUi(UI ui) {
        mUi = (BaseUi) ui;
    }

    // 占位，避免编译过滤掉资源文件
    public static int mRecommendIcon_en[] = {
            R.drawable.ic_browser_recommend_amazon,
            R.drawable.ic_browser_recommend_facebook,
            R.drawable.ic_browser_recommend_google,
            R.drawable.ic_browser_recommend_youtube,
            R.drawable.ic_browser_recommend_baidu,
            R.drawable.ic_browser_recommend_qiyi,
            R.drawable.ic_browser_recommend_taobao,
            R.drawable.ic_browser_recommend_wangyi,
            R.drawable.ic_browser_recommend_blank,
            R.drawable.ic_browser_recommend_add,
            R.drawable.ic_browser_recommend_wiki
    };

    public static int mLogoSearchEngine[] = {
            R.drawable.ic_browser_engine_baidu_logo, R.drawable.ic_browser_engine_google_logo, R.drawable.ic_browser_engine_yahoo_logo,
            R.drawable.ic_browser_engine_bing_logo, R.drawable.ic_browser_engine_360_logo,
            R.drawable.ic_browser_engine_sougou_logo, R.drawable.ic_browser_engine_yandex_logo
    };

    public static int mIconSearch[] = {
            R.drawable.ic_browser_engine_baidu, R.drawable.ic_browser_engine_google, R.drawable.ic_browser_engine_yahoo,
            R.drawable.ic_browser_engine_bing, R.drawable.ic_browser_engine_360,
            R.drawable.ic_browser_engine_sougou, R.drawable.ic_browser_yandex_icon
    };

    @Override
    public void onScreenRotate(boolean isPortrait) {
        if (mInitState == STATUS_EMPTY) return;
        mHeadTitleView.onScreenRotate(isPortrait);
        mWebNavigationViewView.onScreenRotate(isPortrait);
        restoreHeadViewStatus();
    }

    private void restoreHeadViewStatus() {
        final ImageView browserLogo = mHeadTitleView.getBrowserLogo();
        final FrameLayout searchBar = mHeadTitleView.getSearchBar();
        if (mWebNavigationViewView.isEdit()) {
            browserLogo.setVisibility(View.INVISIBLE);
            searchBar.setVisibility(View.INVISIBLE);
            mBlackBackground.setBackgroundColor(Color.BLACK);
        } else {
            refreshBlackBgOffEdit(mIncognito);
        }
    }

    public void registerSearchListener(final OnSearchUrl listener) {
        this.mListener = listener;
    }

    public void showViewPager() {
        attachMainViewPager();
        if (mHomePage != null) {
            mHomePage.setVisibility(View.VISIBLE);
            mHomePage.bringToFront();
        }
    }

    public void hideViewPager() {
        if (mHomePage != null) {
            mHomePage.setVisibility(View.GONE);
        }
    }

    public boolean isVisible() {
        return mInitState != STATUS_EMPTY &&
                mHomePage != null &&
                mHomePage.getVisibility() == View.VISIBLE;
    }

    public void setSearchViewMoveStyle() {
        if (mUi != null)
            mUi.openSearchInputView("");
    }

    public void openSelectSearchEngineView(View view) {
        if (mUi != null) {
            mUi.openSelectSearchEngineView(view);
        }
    }

    /**
     * 完成首次界面UI的数据刷新
     */
    public void onResumeIfNeed() {
        if (!mDataInit) {
            onResume();
            mDataInit = true;
        }
    }

    /**
     * 当主页切换到前景时就会被调用
     */
    public void onResume() {
//        // 通知主页数据变更,这里的数据需要异步加载
        if (mHeadTitleView != null) {
            mHeadTitleView.onResume();
            mHeadTitleView.updateSearchEngineLogo(false);
        }
        if (mWebNavigationViewView != null) {
            mWebNavigationViewView.onResume();
        }
    }

    public void onPause() {
        if (mHeadTitleView != null) {
            mHeadTitleView.onPause();
        }
    }

    public void onConfigurationChanged(Configuration config) {
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            onScreenRotate(true);
        } else if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onScreenRotate(false);
        }
    }

    public boolean isWebEditShowing() {
        return mWebNavigationViewView != null && mWebNavigationViewView.isWebEditShowing();
    }

    private boolean isBottomButtonShowing() {
        TextView bottomButton = mUi.getBottomButton();
        return bottomButton.getVisibility() == View.VISIBLE;
    }

    public boolean isNeedHintToolbar() {
        return isWebEditShowing() || isBottomButtonShowing();
    }


    public void wrapScreenshot(Tab tab) {
        if (tab != null) {
            tab.setHomePage(mHomePage);
        }
    }

    /**
     * 主页显示时伴随会有一个空Tab，并且该空的tab会进入TabController队列
     *
     * @param tab
     */
    public void switchTab(Tab tab) {
        this.mTab = tab;
    }

    public Tab getCurrentTab() {
        return mTab;
    }

    private boolean hideMultSelectView() {
        return false;
    }

    /**
     * 处理推荐item的点击事件
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.window_close:
                hideMultSelectView();
                break;
        }
    }

    public int getInitStatus() {
        return mInitState;
    }

    /**
     * 初始化主界面
     */
    public void initRootView() {
        mHomePage = (FrameLayout) mUi.getHomeContainer();
        mBlackBackground = new View(mContext);
        refreshBlackBgOffEdit(mIncognito);
        mHomePage.addView(mBlackBackground);
        mHeadTitleView = new HomePageHeadView(mContext, this, DisplayUtil.isScreenPortrait(mContext), mIncognito);
        mSearchEngineLayout = mHeadTitleView.getSearchBar();
        mHomePage.addView(mHeadTitleView, mHeadTitleView.createLayoutParams());
        initWebNavigationView();
        mHomePage.setClipChildren(false);
        mInitState = STATUS_DONE;
    }

    public void initWebNavigationView() {
        this.mWebNavigationViewView = new WebNavigationView((FragmentActivity) mContext, mUi.mToolbar, mUi.getBottomButton());
        mWebNavigationViewView.setWebNavigationListener(new WebNavigationView.WebNavigationListener() {
            @Override
            public void openUrl(String url) {
                mListener.onSelect(url, false);
            }

            @Override
            public void onTransferOnEditStatus() {
                final ImageView browserLogo = mHeadTitleView.getBrowserLogo();
                final FrameLayout searchBar = mHeadTitleView.getSearchBar();
                AlphaAnimation logoAnim = new AlphaAnimation(1f, 0f);
                final Animation searchBarAnim = loadAnimation(mContext, R.anim.search_bar_dismiss_anim);

                logoAnim.setDuration(TransferAnimArgs.LOGO_DISMISS_TIME);
                logoAnim.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        browserLogo.setVisibility(View.INVISIBLE);
                    }
                });
                searchBarAnim.setStartOffset(TransferAnimArgs.SEARCH_BAR_DISMISS_DELAY);
                searchBarAnim.setDuration(TransferAnimArgs.SEARCH_BAR_DISMISS_TIME);
                searchBarAnim.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        searchBar.setVisibility(View.INVISIBLE);
                    }
                });
                ValueAnimator backgroundAnim = ValueAnimator.ofFloat(0f, 1f);
                backgroundAnim.setDuration(TransferAnimArgs.BACKGROUND_APPEAR_TIME);
                backgroundAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float value = ((float) valueAnimator.getAnimatedValue());
                        @ColorInt int startColor = mIncognito ? ContextCompat.getColor(mContext, R.color.incognito_bg_color) : Color.TRANSPARENT;
                        @ColorInt int endColor = Color.BLACK;
                        mBlackBackground.setBackgroundColor(AnimationUtils.getColor(value, startColor, endColor));
                    }
                });
                backgroundAnim.start();
                browserLogo.startAnimation(logoAnim);
                searchBar.startAnimation(searchBarAnim);
            }

            @Override
            public void onTransferOffEditStatus() {
                final ImageView browserLogo = mHeadTitleView.getBrowserLogo();
                final FrameLayout searchBar = mHeadTitleView.getSearchBar();
                Animation headViewAnim = new ScaleAnimation(0.8f, 1f, 0.8f, 1f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                headViewAnim.setDuration(TransferAnimArgs.HEAD_VIEW_APPEAR_TIME);
                headViewAnim.setStartOffset(TransferAnimArgs.HEAD_VIEW_APPEAR_DELAY);
                headViewAnim.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        browserLogo.setVisibility(View.VISIBLE);
                        searchBar.setVisibility(View.VISIBLE);
                    }
                });
                headViewAnim.setInterpolator(new Interpolator() {
                    @Override
                    public float getInterpolation(float v) {
                        browserLogo.setAlpha(v);
                        searchBar.setAlpha(v);
                        return v;
                    }
                });
                browserLogo.startAnimation(headViewAnim);
                searchBar.startAnimation(headViewAnim);
                ValueAnimator bgAnim = ValueAnimator.ofFloat(0f, 1f);
                bgAnim.setDuration(TransferAnimArgs.BACKGROUND_DISMISS_TIME);
                bgAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float value = ((float) valueAnimator.getAnimatedValue());
                        @ColorInt int startColor = Color.BLACK;
                        @ColorInt int endColor = mIncognito && false ? ContextCompat.getColor(mContext, R.color.incognito_bg_color) : Color.TRANSPARENT;
                        mBlackBackground.setBackgroundColor(AnimationUtils.getColor(value, startColor, endColor));
                    }
                });
                bgAnim.start();
            }
        });
        mHomePage.addView(mWebNavigationViewView);
    }

    public void onIncognito(boolean incognito) {
        if (mIncognito == incognito) return;
        mIncognito = incognito;
        mHeadTitleView.onIncognito(incognito);
        mWebNavigationViewView.onIncognito(incognito);
        refreshBlackBgOffEdit(incognito);
    }

    private void refreshBlackBgOffEdit(boolean incognito) {
        if (incognito && false) {
            mBlackBackground.setBackgroundColor(ContextCompat.getColor(mContext, R.color.incognito_bg_color));
        } else {
            mBlackBackground.setBackgroundColor(Color.TRANSPARENT);
        }
    }


    public void attachMainViewPager() {
        if (getInitStatus() == MainPageController.STATUS_EMPTY) {
            initRootView();
        }
        if (mHomePage != null) {
            mWebNavigationViewView.setVisibility(View.VISIBLE);
            if (mHeadTitleView != null) {
                mHeadTitleView.setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean onBackKey() {
        return mWebNavigationViewView != null && mWebNavigationViewView.onBackKey();
    }

    public void updateDefaultSearchEngine() {
        if (mHeadTitleView != null) {
            mHeadTitleView.post(new Runnable() {
                @Override
                public void run() {
                    mHeadTitleView.updateSearchEngineLogo(true);
                    mHeadTitleView.onResume();
                }
            });
        }
    }

    public void startVoiceRecognizer() {
        mController.startVoiceRecognizer();
    }

}
