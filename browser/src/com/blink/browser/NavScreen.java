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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.homepages.ImmersiveController;
import com.blink.browser.view.carousellayoutmanager.CarouselLayoutManager;
import com.blink.browser.view.carousellayoutmanager.CarouselRecyclerView;
import com.blink.browser.view.carousellayoutmanager.CarouselZoomPostLayoutListener;
import com.blink.browser.view.carousellayoutmanager.CenterScrollListener;

public class NavScreen extends RelativeLayout
        implements OnClickListener, OnMenuItemClickListener {

    UiController mUiController;
    PhoneUi mUi;
    Activity mActivity;
    ImageView mNewTab;
    ImageView mBack;
    ImageView mTabModeSwith;
    CarouselRecyclerView mTabsRecyclerView;
    RelativeLayout mTabBar;
    private CarouselTabAdapter mTabAdapter;
    private CarouselLayoutManager mTabLayoutManager;
    private ItemTouchHelper mItemTouchHelper;
    int mOrientation;
    boolean mNeedsMenu;
    private boolean mBlockEvents = false;
    private LinearLayout mZeroIncognito;
    boolean mShowNavAnimating = false;
    private int mCloseCount = 0;
    private boolean mCancelAnimation;
    FrameLayout mTabsContent;

    public NavScreen(Activity activity, UiController ctl, PhoneUi ui) {
        super(activity);
        mActivity = activity;
        mUiController = ctl;
        mUi = ui;
        mOrientation = activity.getResources().getConfiguration().orientation;
        init();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mBlockEvents || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mBlockEvents || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mBlockEvents || super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mBlockEvents || super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mBlockEvents || super.dispatchGenericMotionEvent(ev);
    }

    public void setBlockEvents(boolean block) {
        mBlockEvents = block;
    }

    @Override
    protected void onConfigurationChanged(Configuration newconfig) {
        if (newconfig.orientation != mOrientation) {
            removeAllViews();
            mOrientation = newconfig.orientation;
            init();
        }
    }

    public void refreshAdapter() {
        int position = mUiController.getTabControl().getCurrentPosition();
        mTabsRecyclerView.scrollToPosition(position + 1);
        mTabAdapter.notifyDataSetChanged();
    }

    public boolean isShowNavScreenAnimating() {
        return mShowNavAnimating;
    }

    public void setShowNavScreenAnimating(boolean showNavScreenAnimating) {
        mShowNavAnimating = showNavScreenAnimating;
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.nav_screen, this);
        setContentDescription(getContext().getResources().getString(
                R.string.accessibility_transition_navscreen));
        mNewTab = (ImageView) findViewById(R.id.newtab);
        mNewTab.setOnClickListener(this);
        mBack = (ImageView) findViewById(R.id.back_ui);
        mBack.setOnClickListener(this);
        mTabsContent = (FrameLayout) findViewById(R.id.tabs_content);
        mTabModeSwith = (ImageView) findViewById(R.id.tab_mode_switch);
        mTabModeSwith.setOnClickListener(this);
        mZeroIncognito = (LinearLayout) findViewById(R.id.zero_incognito);
        mTabBar = (RelativeLayout) findViewById(R.id.tabbar);
        mTabsRecyclerView = (CarouselRecyclerView) findViewById(R.id.tabs_recyclerview);
        mTabAdapter = new CarouselTabAdapter(getContext(), mUiController, mUi, this, mTabsRecyclerView);
        initRecyclerView(mTabsRecyclerView, new CarouselLayoutManager(mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? CarouselLayoutManager
                .VERTICAL : CarouselLayoutManager.HORIZONTAL, false), mTabAdapter);
        mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        if (mUiController.getTabControl().isIncognitoShowing()) {
            showIncognitoTabMode();
        } else {
            showNormalTabMode();
        }
    }

    private void initRecyclerView(final RecyclerView recyclerView, final CarouselLayoutManager layoutManager, final
    CarouselTabAdapter adapter) {
        // enable zoom effect. this line can be customized
        layoutManager.setPostLayoutListener(new CarouselZoomPostLayoutListener());

        recyclerView.setLayoutManager(layoutManager);
        mTabLayoutManager = layoutManager;
        // we expect only fixed sized item for now
        recyclerView.setHasFixedSize(true);
        // sample adapter with random data
        recyclerView.setAdapter(adapter);
        int position = mUiController.getTabControl().getCurrentPosition();
        recyclerView.scrollToPosition(position + 1);
        // enable center post scrolling ,which will cause the view rollback
        recyclerView.addOnScrollListener(new CenterScrollListener());
        // add the swipe up/down
        int swipeDirection  = layoutManager.getOrientation() == CarouselLayoutManager.VERTICAL ? ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT : ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, swipeDirection) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView
                    .ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == 0 || position == adapter.getItemCount() - 1) {
                    return;
                }
                mTabAdapter.onCloseTab(mUiController.getTabControl().getTab(viewHolder.getAdapterPosition() - 1));
                mTabAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                if (!mUiController.getTabControl().isIncognitoShowing() && !mUiController.getTabControl()
                        .hasAnyOpenNormalTabs()) {
                    mUi.createNewTabWithNavScreen(false);
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float
                    dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (layoutManager.getOrientation() == CarouselLayoutManager.VERTICAL) {
                    viewHolder.itemView.setAlpha(1 - Math.abs(dX) / viewHolder.itemView.getWidth() * 0.3f);
                } else {
                    viewHolder.itemView.setAlpha(1 - Math.abs(dY) / viewHolder.itemView.getHeight());
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                viewHolder.itemView.setAlpha(1);
                float originalElevation;
                originalElevation = ViewCompat.getElevation(viewHolder.itemView);
                super.clearView(recyclerView, viewHolder);
                ViewCompat.setElevation(viewHolder.itemView, originalElevation);
                viewHolder.itemView.setTag(android.support.v7.recyclerview.R.id.item_touch_helper_previous_elevation,
                        originalElevation);
            }
        };
        mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onClick(View v) {
        mCancelAnimation = true;
        if (mNewTab == v) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS, AnalyticsSettings
                    .ID_ADD);
            if (!mUiController.getTabControl().canCreateNewTab(mUiController.getTabControl().isIncognitoShowing())) {
                mUi.showMaxTabsWarning();
                return;
            }
            mUi.createNewTabWithNavScreen(mUiController.getTabControl().isIncognitoShowing());
        } else if (mTabModeSwith == v) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS, AnalyticsSettings
                    .ID_INCOGNITOCLICK);
            flip3D(this, true, true);
        } else if (mBack == v) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS, AnalyticsSettings
                    .ID_BACK);
            Tab currTab = mUiController.getTabControl().getCurrentTab();
            if (currTab == null) {
                switchNavScreenNormal();
            } else if (currTab != null && currTab.isNativePage()) {
                mUi.panelSwitchHome(mUiController.getTabControl().getTabPosition(currTab), true);
            } else if (currTab != null) {
                mUi.panelSwitch(UI.ComboHomeViews.VIEW_WEBVIEW, mUiController.getTabControl().getTabPosition(currTab)
                        , true);
            }
        }
    }

    public View getTabBar() {
        return mTabBar;
    }


    private void setAnimation(final View view, final int position, int order) {
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_out_top);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mCloseCount++;
                if (mTabsRecyclerView.getChildAdapterPosition(view) != -1) {
                    mTabAdapter.notifyItemRemoved(mTabsRecyclerView.getChildAdapterPosition(view));
                }
                if (mTabLayoutManager.getChildCount() == mCloseCount) {
                    setBlockEvents(false);
                    mUiController.setBlockEvents(false);
                    if (!mUiController.getTabControl().isIncognitoShowing()) {
                        mUi.createNewTabWithNavScreen(false);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setStartOffset(order * 50);
        view.startAnimation(animation);
    }

    public void toggleTabMode() {
        Tab t;
        if (mUiController.getTabControl().isIncognitoShowing()) {
            t = mUiController.getTabControl().getCurrentTabForMode(false);
            mUiController.getTabControl().setIncognitoShowing(false);
            if (t != null) {
                mUiController.getTabControl().setCurrentTab(t);
            } else {
                showNormalTabMode();
                mTabAdapter.notifyDataSetChanged();
                mUi.createNewTabWithNavScreen(false);
                return;
            }
            showNormalTabMode();
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.INCOGNITO_EVENTS, AnalyticsSettings
                    .ID_NORMAL);
        } else {
            t = mUiController.getTabControl().getCurrentTabForMode(true);
            mUiController.getTabControl().setIncognitoShowing(true);
            if (t != null) {
                mUiController.getTabControl().setCurrentTab(t);
            }
            showIncognitoTabMode();
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.INCOGNITO_EVENTS, AnalyticsSettings
                    .ID_INCOGNITO);
        }
        if (t != null) {
            int position = mUiController.getTabControl().getTabPosition(t);
            mTabsRecyclerView.scrollToPosition(position + 1);
        }
        mTabAdapter.notifyDataSetChanged();
    }

    public void showIncognitoTabMode() {
        ImmersiveController.getInstance().changeStatus();
        mTabModeSwith.setImageResource(R.drawable.ic_browser_label_switch);
        mZeroIncognito.setVisibility(GONE);
        if (mUiController.getTabControl().getTabCount() < 1) {
            mZeroIncognito.setVisibility(VISIBLE);
        }
    }

    public void showNormalTabMode() {
        ImmersiveController.getInstance().changeStatus();
        mTabModeSwith.setImageResource(R.drawable.ic_browser_incognito_switch);
        mZeroIncognito.setVisibility(GONE);
    }

    public void openNewTab(boolean incognito) {
        if (!mUiController.getTabControl().canCreateNewTab(incognito)) {
            mUi.showMaxTabsWarning();
            return;
        }

        mUiController.setBlockEvents(true);
        if (!incognito) {
            mUiController.openTabToHomePage();
        } else {
            mUiController.openIncognitoTab();
        }
        mUiController.setBlockEvents(false);
    }

    protected void close(int position) {
        close(position, true);
    }

    protected void close(int position, boolean animate) {
        mUi.hideNavScreen(position, animate);
    }

    public void switchNavScreenNormal() {
        flip3D(this, true, false);
    }


    public void onTabCountUpdate(int tabCount) {
        if (mUiController.getTabControl().isIncognitoShowing() && mUiController.getTabControl().getTabCount() < 1) {
            mZeroIncognito.setVisibility(VISIBLE);
        }
    }

    public View getTabView(Tab tab) {
        View v = null;
        if (mTabLayoutManager != null) {
            v = mTabLayoutManager.findViewByPosition(mUiController.getTabControl().getTabPosition(tab) + 1);
        }
        return v;
    }

    private void flip3D(final View v, final boolean scale, boolean reverse) {
        mCancelAnimation = false;
        int distance = 16000;
        float cameraDist = mActivity.getResources().getDisplayMetrics().density * distance;
        v.setCameraDistance(cameraDist);
        final int duration = 250;
        final int degree = reverse ? 90 : -90;
        final int degree2 = -degree;

        final ObjectAnimator a, b;
        if (!scale) {
            a = ObjectAnimator.ofFloat(v, "rotationY", 0, degree);
            b = ObjectAnimator.ofFloat(v, "rotationY", degree2, 0);
        } else {
            final float scaleX = 0.8f;
            final float scaleY = 0.8f;
            a = ObjectAnimator.ofPropertyValuesHolder(v,
                    PropertyValuesHolder.ofFloat("rotationY", 0, degree),
                    PropertyValuesHolder.ofFloat("scaleX", 1, scaleX),
                    PropertyValuesHolder.ofFloat("scaleY", 1, scaleY));
            b = ObjectAnimator.ofPropertyValuesHolder(v,
                    PropertyValuesHolder.ofFloat("rotationY", degree2, 0),
                    PropertyValuesHolder.ofFloat("scaleX", scaleX, 1),
                    PropertyValuesHolder.ofFloat("scaleY", scaleY, 1));
        }

        a.setInterpolator(new LinearInterpolator());
        b.setInterpolator(new LinearInterpolator());
        a.setDuration(duration);
        b.setDuration(duration);
        b.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        a.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCancelAnimation) {
                    toggleTabMode();
                }
                if (scale) {
                    v.setScaleX(1);
                    v.setScaleY(1);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        AnimatorSet set = new AnimatorSet();
        set.play(a).before(b);
        set.start();
    }

}
