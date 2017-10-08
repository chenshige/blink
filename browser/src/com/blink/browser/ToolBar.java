// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.ViewDragHelper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.TabControl.OnTabCountChangeListener;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.menu.CommonMenu;
import com.blink.browser.menu.DownloadFile;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.view.CircleImageView;
import com.blink.browser.view.ToolbarCenterView;

import java.util.List;

public class ToolBar extends FrameLayout implements View.OnClickListener, OnTabCountChangeListener,
        BrowserWebView.OnScrollChangedListener, View.OnLongClickListener {
    private static final int PROGRESS_MAX = 100;
    public static final int SCROLL_ANIMATOR_TIME = 200;
    private static final int STOP_BTN_ANIMATION_TIME = 10 * 60;

    public static final int STATE_DOWNING = 1;
    public static final int STATE_DOWN = 2; //Toolbar 展开状态
    public static final int STATE_UPPING = 3;
    public static final int STATE_UP = 4; //Toolbar 收起状态
    private int mState = STATE_DOWN;
    private boolean mIsTouching;

    private RelativeLayout mMenuButton;
    private RelativeLayout mTitleLoadingButton;
    private FrameLayout mTabSwitcherButton;
    private TextView mTabPageNumber;
    private UiController mUiController;
    private CommonMenu mCommonMenu;
    private View mDividerView;
    private boolean mIsShowToolBar;
    private Animation mShowOrHideAnimation;
    private ImageView mMenuButtonID, mTabSwitcherButtonID, mMoreButtonId, mSafeIcon;
    private TextView mTitle;

    private PageProgressView mProgress;
    private boolean mInLoad;
    private Animator mScrollAnimator;
    private boolean mIsDownScrollAnimator;
    private boolean mIsUpScrollAnimator;
    private int mScrollDistance;
    private BrowserWebView mWebView;
    private CircleImageView mBackHomeView;
    private float mDownY;
    private int mMaxScrollDistance;
    private int mTouchSlop;
    private View mRightMenuView;
    private View mContextView;
    private boolean mMenuShow;
    private float mDownX;
    private View mProgressLeft;
    private boolean mScrollToolBar;
    private boolean mSwipeSuccess;
    private static final int SCROLLING_NO_DIRECTION = 0;
    private static final int SCROLLING_VERTICAL = 1;
    private static final int SCROLLING_HORIZONTAL = -1;
    private int mScrollDirection = SCROLLING_NO_DIRECTION;
    private View mLeftMenuView;
    private int mScrollHorizontalDistance;
    //private ToolbarCenterView mToolbarCenterView;
    private ImageView mStopLoadBtn;
    private boolean mIsSearchResultPage = false;
    private boolean mIsPageLoading; //有些网址会重定向
    private boolean mPageFinishedAnimation; //网页加载结束动画, true正在做动画

    private RelativeLayout.LayoutParams mMenuLayoutParams;
    private ValueAnimator mMenuLayoutAnimator;
    private RelativeLayout.LayoutParams mTitleLayoutParams;
    private ValueAnimator mTitleLayoutAnimator;
    private RelativeLayout.LayoutParams mTabSwitcherLayoutParams;
    private ValueAnimator mTabSwitcherLayoutAnimator;
    private ViewDragHelper mViewDragHelper;
    private boolean mDragRelease;
    private boolean mIsDoneScrollAnimation;
    private DownloadFile mDownloadFile;

    public ToolBar(Context context) {
        super(context);
    }

    public ToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initLayout();
        setOnClickListener(this);
    }

    private void initLayout() {
        mMenuButton = (RelativeLayout) findViewById(R.id.menu_toolbar);
        mTabSwitcherButton = (FrameLayout) findViewById(R.id.tabswitcher_toolbar);
        mTitleLoadingButton = (RelativeLayout) findViewById(R.id.title_loading_view);
        mTabPageNumber = (TextView) findViewById(R.id.page_number_tab_id);
        mTabSwitcherButton.setOnClickListener(this);
        mTabSwitcherButton.setOnLongClickListener(this);
        mMenuButtonID = (ImageView) findViewById(R.id.menu_toolbar_id);
        mTabSwitcherButtonID = (ImageView) findViewById(R.id.tabswitcher_toolbar_id);
        mSafeIcon = (ImageView)findViewById(R.id.safe_icon);
        mTitle = (TextView)findViewById(R.id.web_view_title_view);
        // mToolbarCenterView = (ToolbarCenterView) findViewById(R.id.toolbar_center_view);
        mProgress = (PageProgressView) findViewById(R.id.progress_view);
        mRightMenuView = findViewById(R.id.swipe_right_menu);
        mLeftMenuView = findViewById(R.id.swipe_left_menu);
        mContextView = findViewById(R.id.swipe_context);
        mProgressLeft = findViewById(R.id.progress_left);
        mStopLoadBtn = (ImageView) findViewById(R.id.stop_refresh);
        mStopLoadBtn.setOnClickListener(this);
        mMenuButtonID.setOnClickListener(this);
        mTitleLoadingButton.setOnClickListener(this);
        // mToolbarCenterView.setOnItemClickListener(this);
        setOnClickListener(this);

        mMaxScrollDistance = getResources().getDimensionPixelSize(R.dimen.max_scroll_distance);
        mScrollHorizontalDistance = getResources().getDimensionPixelSize(R.dimen.scroll_toolbar_horizontal_distance);
        ViewConfiguration config = ViewConfiguration.get(getContext());
        mTouchSlop = config.getScaledTouchSlop();

        mMenuLayoutParams = (RelativeLayout.LayoutParams) mMenuButton.getLayoutParams();
        mTabSwitcherLayoutParams = (RelativeLayout.LayoutParams) mTabSwitcherButton.getLayoutParams();
        mTitleLayoutParams = (RelativeLayout.LayoutParams)mTitleLoadingButton.getLayoutParams();
        mViewDragHelper = ViewDragHelper.create((FrameLayout) findViewById(R.id.tool_bar), 1f,
                new DragLayoutCallBack());
    }

    private void menuViewHide() {
        if (mLeftMenuView != null) {
            mLeftMenuView.setVisibility(INVISIBLE);
        }
        if (mRightMenuView != null) {
            mRightMenuView.setVisibility(View.INVISIBLE);
        }
        if (mProgressLeft != null) {
            mProgressLeft.setVisibility(INVISIBLE);
        }
        mMenuShow = false;
    }

    public void setUicontroller(UiController uiController) {
        mUiController = uiController;
        mUiController.getTabControl().registerTabChangeListener(this);
//        if (mToolbarCenterView != null) {
//            mToolbarCenterView.setUiController(uiController);
//        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        menuViewHide();
    }

    protected void setUrlTitle(Tab tab) {
        if (!tab.isNativePage()) {
            String url = tab.getUrl();
            String title = tab.getTitle();
            if (TextUtils.isEmpty(title)) {
                title = url;
            }
            if (mTitle != null && !title.equals(mTitle.getText().toString())) {
                mTitle.setText(title);
            }
//            if (tab.inForeground()) {
//                mToolbarCenterView.setUrlTitle(title);
//            }
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (view.getVisibility() != VISIBLE) {
            return;
        }
        if ((id == R.id.tool_bar || id == R.id.title_loading_view) && mState == STATE_UP) {
            scrollAnimation(DIRECTION.DOWN);
            if (mUiController != null && mUiController.getUi() != null) {
                ((BaseUi)mUiController.getUi()).changeWebViewHeight();
            }
            return;
        }
        if ((id == R.id.tabswitcher_toolbar || id == R.id.menu_toolbar) && mState == STATE_UP) {
            return;
        }
        switch (id) {
            case R.id.menu_toolbar_id:

                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.TOOLS_EVENTS, AnalyticsSettings.ID_MENU);
                if (mCommonMenu == null) {
                    ViewStub commonMenuViewStub = (ViewStub) ((ViewGroup) getParent()).findViewById(R.id
                            .view_stub_common_menu);
                    mCommonMenu = (CommonMenu) commonMenuViewStub.inflate();
                }
                if (mCommonMenu != null) {
                    mCommonMenu.onConfigurationChanged(getContext().getResources().getConfiguration());
                }
                mUiController.showCommonMenu(mCommonMenu);
                break;
            case R.id.stop_refresh:
                if (mInLoad && mUiController != null) {
                    mUiController.stopLoading();
                }
                break;
            default:
                mUiController.onToolBarItemClick(view);
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == mTabSwitcherButton && mBackHomeView != null && mUiController.getCurrentTab() != null && !mUiController.getCurrentTab().isNativePage()) {
            mBackHomeView.setVisibility(VISIBLE);
            mBackHomeView.setFillColor(getResources().getColor(R.color.back_home_bg));
            AnimationSet animationSet = new AnimationSet(true);
            TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, -mMaxScrollDistance);
            translateAnimation.setInterpolator(new DecelerateInterpolator());
            translateAnimation.setDuration(getResources().getInteger(R.integer.tab_animation_duration));
            animationSet.addAnimation(translateAnimation);

            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setInterpolator(new AccelerateInterpolator());
            alphaAnimation.setDuration(getResources().getInteger(R.integer.hide_back_home_duration));
            alphaAnimation.setStartOffset(getResources().getInteger(R.integer.tab_animation_duration));
            animationSet.addAnimation(alphaAnimation);

            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mUiController.getUi() instanceof PhoneUi) {
                        ((PhoneUi) mUiController.getUi()).panelSwitchHome(mUiController.getTabControl()
                                .getCurrentPosition(), true);
                    }
                    hideBackHome();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            mBackHomeView.startAnimation(animationSet);
            return true;
        }
        return false;
    }

    @Override
    public void onTabCountUpdate(int tabCount) {
        DisplayUtil.resetTabSwitcherTextSize(mTabPageNumber, mUiController.getTabControl().getTabCount());
    }

    public void setToolbarStyle(boolean incognito, boolean isNativePage) {
        setBackgroundResource(0);
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.web_view_toolbar_background_color));
        if (incognito) {
            mTabSwitcherButtonID.setImageResource(R.drawable.ic_browser_incognito_label);
            mTitle.setTextColor(Color.WHITE);
//            mMenuButtonID.setImageResource(R.drawable.ic_browser_incognito_more);
//            mTabPageNumber.setTextColor(Color.WHITE);
//            if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.LOLLIPOP) {
//                mTabSwitcherButton.setBackgroundResource(R.drawable.browser_common_menu_item_bg_incognito);
//                mMenuButtonID.setBackgroundResource(R.drawable.browser_common_menu_item_bg_incognito);
//                mStopLoadBtn.setBackgroundResource(R.drawable.browser_common_menu_item_bg_incognito);
//            }
//            mStopLoadBtn.setImageResource(R.drawable.ic_browser_toolbar_close_incognito);
        } else {
            mTitle.setTextColor(Color.BLACK);
            mTabPageNumber.setTextColor(getContext().getResources().getColor(R.color.toolbar_page_number_color));
            mMenuButtonID.setImageResource(R.drawable.ic_browser_more);
            mTabSwitcherButtonID.setImageResource(R.drawable.ic_browser_label);
            if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.LOLLIPOP) {
                mTabSwitcherButton.setBackgroundResource(R.drawable.browser_common_menu_item_bg);
                mMenuButtonID.setBackgroundResource(R.drawable.browser_common_menu_item_bg);
                mStopLoadBtn.setBackgroundResource(R.drawable.browser_common_menu_item_bg);
            }
            mStopLoadBtn.setImageResource(R.drawable.ic_browser_toolbar_close);
        }
        if (incognito && false) {
            setBackgroundColor(ContextCompat.getColor(getContext(), R.color.toolbar_incognito_background_color));
        } else {
            if (isNativePage) {
                setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
            } else {
                setBackgroundColor(ContextCompat.getColor(getContext(), R.color.web_view_toolbar_background_color));
            }
        }

        updateLayout(isNativePage);
        // mToolbarCenterView.updateStyle();
        if (isNativePage) {
            mDividerView.setVisibility(GONE);
            mStopLoadBtn.setVisibility(GONE);
            mMenuButtonID.setVisibility(VISIBLE);
            mProgress.setVisibility(GONE);
            mSafeIcon.setVisibility(GONE);
            mTitle.setText("");
        } else if (!mIsPageLoading) {
            mDividerView.setVisibility(VISIBLE);
            mProgress.setVisibility(GONE);
            mStopLoadBtn.clearAnimation();
            mStopLoadBtn.setVisibility(GONE);
            mMenuButtonID.setVisibility(VISIBLE);
        } else {
            mDividerView.setVisibility(VISIBLE);
            mProgress.setVisibility(VISIBLE);
            mStopLoadBtn.setVisibility(VISIBLE);
            mMenuButtonID.setVisibility(GONE);
        }
    }

    public void setDividerView(View view) {
        mDividerView = view;
    }

    public void setBackHomeView(CircleImageView view) {
        mBackHomeView = view;
        mBackHomeView.setRadius(getResources().getDimensionPixelSize(R.dimen.sub_action_button_size) / 2);
    }

    /**
     * 控制toolbar的显示和隐藏。首页，web，多标签，无痕，暂时隐藏,
     */
    public boolean updateToolBarVisibility() {
        return updateToolBarVisibility(false, false);
    }

    /**
     * @param animate          是否做toolbar的动画
     * @param isShowCommonMenu //是否需要隐藏弹出菜单
     * @return
     */
    public boolean updateToolBarVisibility(boolean animate, boolean isShowCommonMenu) {
        if (!isShowCommonMenu && mCommonMenu != null && mCommonMenu.isShowing()) {
            mCommonMenu.setVisibility(View.GONE);
        }
        mIsShowToolBar = true;
        if (((PhoneUi) mUiController.getUi()).showingNavScreen() && ((PhoneUi) mUiController.getUi())
                .showingNavScreenForExit()) {
            //navscreen界面
            mIsShowToolBar = false;
        } else if (((PhoneUi) mUiController.getUi()).showingNavScreen() && !((PhoneUi) mUiController.getUi())
                .showingNavScreenForExit()) {
            mIsShowToolBar = true;
        }

        if (mUiController.getViewPageController().isNeedHintToolbar()) {
            mIsShowToolBar = false;
        }

        if (mShowOrHideAnimation != null) {
            mShowOrHideAnimation.cancel();
            clearAnimation();
            mShowOrHideAnimation.setAnimationListener(null);
            setAnimation(null);
        }
        if (mIsShowToolBar) {
            if (mUiController.getCurrentTab() != null) {
                setToolbarStyle(mUiController.getTabControl().isIncognitoShowing(),
                        mUiController.getCurrentTab().isNativePage());
            }
            if (animate) {
                mTabSwitcherButton.clearAnimation();
                mMenuButton.clearAnimation();
                mTitleLoadingButton.clearAnimation();
                mShowOrHideAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_bottom);
                mShowOrHideAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mShowOrHideAnimation = null;
                        if (mDividerView != null && mUiController != null
                                && mUiController.getCurrentTab() != null
                                && !mUiController.getCurrentTab().isNativePage()) {
                            mDividerView.setVisibility(VISIBLE);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                startAnimation(mShowOrHideAnimation);
            } else {
                if (mDividerView != null && mUiController != null
                        && mUiController.getCurrentTab() != null
                        && !mUiController.getCurrentTab().isNativePage()) {
                    mDividerView.setVisibility(VISIBLE);
                }
                setVisibility(View.VISIBLE);
            }

        } else {
            if (animate) {
                mTabSwitcherButton.clearAnimation();
                mMenuButton.clearAnimation();
                mTitleLoadingButton.clearAnimation();
                mShowOrHideAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out_bottom);
                mShowOrHideAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (mDividerView != null) {
                            mDividerView.setVisibility(GONE);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        setVisibility(View.GONE);
                        mShowOrHideAnimation = null;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                startAnimation(mShowOrHideAnimation);
            } else {
                setVisibility(View.GONE);
                if (mDividerView != null) {
                    mDividerView.setVisibility(GONE);
                }
            }
        }
        return mIsShowToolBar;
    }

    public boolean isShowToolBar() {
        return mIsShowToolBar;
    }

    public boolean handleTouchToolBar(MotionEvent ev) {
        if (mBackHomeView != null && (mCommonMenu == null || !mCommonMenu.isShowing())) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (ev.getPointerCount() == 1) {
                        mDownX = ev.getX();
                        mDownY = ev.getY();
                        mSwipeSuccess = false;
                        mScrollDirection = SCROLLING_NO_DIRECTION;
                        mScrollToolBar = true;
                        return false;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getX() - mDownX;
                    float dy = ev.getY() - mDownY;
                    if (mScrollToolBar) {
                        if (-dy > mTouchSlop && mScrollDirection == SCROLLING_NO_DIRECTION) {
                            mScrollDirection = SCROLLING_VERTICAL;
                        }
                    }
                    if (mScrollDirection == SCROLLING_VERTICAL) {
                        if (mUiController.getCurrentTab() != null && !mUiController.getCurrentTab().isNativePage()) {
                            if (mDownY - ev.getY() >= mTouchSlop) {
                                mBackHomeView.setVisibility(VISIBLE);
                            }
                            if (mDownY - ev.getY() >= mTouchSlop + mMaxScrollDistance) {
                                mBackHomeView.setY(getY() - mMaxScrollDistance);
                                mBackHomeView.setFillColor(getResources().getColor(R.color.back_home_bg));
                                mSwipeSuccess = true;
                            } else {
                                mBackHomeView.setY(getY() - (mDownY - ev.getY() - mTouchSlop));
                                mBackHomeView.setFillColor(com.blink.browser.util.AnimationUtils.getColor((mDownY
                                                - ev.getY() - mTouchSlop) / mMaxScrollDistance * 1.0f,
                                        getResources().getColor(R.color.mb_blue), getResources().getColor(R.color
                                                .back_home_bg)));
                                mSwipeSuccess = false;
                            }
                            mBackHomeView.invalidate();
                            return false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mScrollToolBar = false;
                    if (mScrollDirection == SCROLLING_VERTICAL) {
                        boolean ret = false;
                        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL ||
                                ev.getPointerCount() > 1) {
                            if (mBackHomeView.getVisibility() != GONE) {
                                ret = true;
                            }
                            mDownY = ev.getY();
                            stopBackHome();
                        }
                        if (mSwipeSuccess) {
                            ev.setAction(MotionEvent.ACTION_CANCEL);
                        }
                        return ret;
                    }
                    break;
                default:
                    break;
            }
            if (mScrollDirection == SCROLLING_VERTICAL && ev.getPointerCount() > 1) {
                mDownY = ev.getY();
                stopBackHome();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScrollDirection != SCROLLING_VERTICAL && mState == STATE_DOWN) {
            try {
                mViewDragHelper.processTouchEvent(event);
                return true;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (handleTouchToolBar(ev)) return true;
        if (mScrollDirection != SCROLLING_VERTICAL && mState == STATE_DOWN) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mViewDragHelper.cancel();
                    return false;
            }
            try {
                return mViewDragHelper.shouldInterceptTouchEvent(ev);
            } catch (Throwable t) {
                t.printStackTrace();
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(true)) {
            layoutContextView(mContextView.getLeft());
            ViewCompat.postInvalidateOnAnimation(this);
        } else if (mDragRelease && mScrollDirection == SCROLLING_HORIZONTAL) {
            if (mRightMenuView != null && mRightMenuView.getVisibility() == VISIBLE && mSwipeSuccess) {
                if (mUiController.canGoForward()) {
                    mUiController.goForward();
                }
            } else if (mLeftMenuView != null && mLeftMenuView.getVisibility() == VISIBLE && mSwipeSuccess) {
                if (mUiController.canGoBack()) {
                    mUiController.goBack();
                }
            }
            mScrollDirection = SCROLLING_NO_DIRECTION;
            menuViewHide();
        }
    }

    public void switchHome() {
        mIsSearchResultPage = false;
        clearAnimation();
        setVisibility(VISIBLE);
        updateLayout(true);
        doUpAnimator(DIRECTION.DOWN, true);
//        if (mToolbarCenterView != null && mUiController != null) {
//            mToolbarCenterView.setState(mUiController.getCurrentTab(), ToolbarCenterView.STATE_HOMEPAGE);
//        }
//        mToolbarCenterView.setIconSafe(false);
        setIconSafe(false);
    }

    private void layoutContextView(int dx) {
        if (mLeftMenuView != null && dx > 0) {
            if (mRightMenuView != null) mRightMenuView.setVisibility(INVISIBLE);
            mLeftMenuView.layout(0, getResources().getDimensionPixelSize(R.dimen.progress_bar_height), dx,
                    mContextView.getMeasuredHeight());
            if (dx < mScrollHorizontalDistance) {
                mLeftMenuView.setAlpha(0);
            } else if (mUiController.canGoBack()) {
                mLeftMenuView.setVisibility(VISIBLE);
                mLeftMenuView.setAlpha(1);
                mSwipeSuccess = true;
            }
        }
        if (mRightMenuView != null && dx < 0) {
            if (mLeftMenuView != null) mLeftMenuView.setVisibility(INVISIBLE);
            mRightMenuView.layout(mContextView.getMeasuredWidth() + dx,
                    getResources().getDimensionPixelSize(R.dimen.progress_bar_height), mContextView.getMeasuredWidth
                            (), mContextView.getMeasuredHeight());
            if (-dx < mScrollHorizontalDistance) {
                mRightMenuView.setAlpha(0);
            } else if (mUiController.canGoForward()) {
                mRightMenuView.setVisibility(VISIBLE);
                mRightMenuView.setAlpha(1);
                mSwipeSuccess = true;
            }
        }
        if (mProgressLeft != null) {
            if (dx < 0) {
                mProgressLeft.setBackground(getResources().getDrawable(R.drawable.ic_browser_preview_bg));
                mProgressLeft.layout(mContextView.getMeasuredWidth() + 4 * dx, 0, mContextView.getMeasuredWidth(),
                        mContextView.getMeasuredHeight());
            } else {
                mProgressLeft.setBackground(getResources().getDrawable(R.drawable.ic_browser_back_bg));
                mProgressLeft.layout(0, 0, 4 * dx, mContextView.getMeasuredHeight());
            }
        }
    }

    private void stopBackHome() {
        mScrollDirection = SCROLLING_NO_DIRECTION;
        if (mBackHomeView.getVisibility() == GONE && mSwipeSuccess) {
            hideBackHome();
        } else if (mSwipeSuccess) {
            AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mUiController.getUi() instanceof PhoneUi) {
                        ((PhoneUi) mUiController.getUi()).panelSwitchHome(mUiController.getTabControl()
                                .getCurrentPosition(), true);
                        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.TOOLS_EVENTS, AnalyticsSettings
                                .ID_BACKHOMEPAGE);
                    }
                    hideBackHome();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setInterpolator(new AccelerateInterpolator());
            alphaAnimation.setDuration(getResources().getInteger(R.integer.tab_animation_duration));
            mBackHomeView.startAnimation(alphaAnimation);
        } else if (mBackHomeView.getVisibility() != GONE) {
            float toY = 0;
            if (mState == STATE_DOWN && getY() > mBackHomeView.getY()) {
                toY = getY() - mBackHomeView.getY();
            } else if (mState == STATE_UP && getY() + mScrollDistance > mBackHomeView.getY()) {
                toY = getY() + mScrollDistance - mBackHomeView.getY();
            }
            TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, toY);
            translateAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    hideBackHome();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            translateAnimation.setInterpolator(new AccelerateInterpolator());
            translateAnimation.setDuration(getResources().getInteger(R.integer.tab_animation_duration));
            mBackHomeView.startAnimation(translateAnimation);
        } else {
            hideBackHome();
        }

    }

    public void hideBackHome() {
        mBackHomeView.setVisibility(GONE);
        mBackHomeView.setY(getTop());
    }

    public boolean isProcessing(float x, float y) {
        Rect outRect = new Rect();
        int[] location = new int[2];
        getDrawingRect(outRect);
        getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return mScrollDirection == SCROLLING_VERTICAL || outRect.contains((int) x, (int) y);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    public void setProgress(int newProgress) {
        if (mProgress == null
                || (mUiController != null
                && mUiController.getCurrentTab() != null
                && mUiController.getCurrentTab().getTitle().equals(getContext().getResources().getString(R.string
                .home_page)))) {
            return;
        }
        if (mUiController != null
                && mUiController.getCurrentTab() != null
                && mUiController.getCurrentTab().isNativePage()) {
            mProgress.setVisibility(GONE);
        } else if (newProgress >= PROGRESS_MAX) {
            mProgress.setVisibility(View.GONE);
            mProgress.setProgress(PageProgressView.MAX_PROGRESS);
            // check if needs to be hidden
        } else {
            if (!mInLoad) {
                mProgress.setVisibility(View.VISIBLE);
                mInLoad = true;
            }
            mProgress.setProgress(newProgress * PageProgressView.MAX_PROGRESS
                    / PROGRESS_MAX);
        }
    }

    public PageProgressView getProgressView() {
        return mProgress;
    }

    public void setWebView(BrowserWebView webView) {
        if (webView == null) {
            return;
        }
        mWebView = webView;
        mWebView.setOnScrollChangedListener(this);
    }

    /**
     * 标识网页滑动方向，UP：上滑，toolbar收起。
     */
    public enum DIRECTION {
        UP, DOWN
    }

    public void resetDirection() {
        mIsTouching = false;
    }

    public int getToolbarState() {
        return mState;
    }

    /**
     * 获取滚动放向
     *
     * @param downY
     * @param lastMoveY
     * @param moveY
     */
    public void doScrollAnimation(int downY, int lastMoveY, int moveY) {
        if ((/*mToolbarCenterView != null && */mIsSearchResultPage) || mIsPageLoading
                || mPageFinishedAnimation
                || (mUiController != null && mUiController.getUi() != null
                && ((PhoneUi) mUiController.getUi()).showingNavScreenForExit())) {
            return;
        }
        if (mScrollDistance == 0) {
            mScrollDistance = getContext().getResources().getDimensionPixelOffset(R.dimen
                    .bottom_toolbar_scroll_animator_distance);
        }
        DIRECTION direction = null;
        int distance = moveY - downY;
        if (mState == STATE_UP && moveY - lastMoveY < 0 && mIsTouching) {
            direction = DIRECTION.UP;
        } else if (mState == STATE_DOWN && moveY - lastMoveY > 0 && mIsTouching) {
            direction = DIRECTION.DOWN;
        } else if (mState == STATE_UP && moveY - lastMoveY > 0 && mIsTouching) {
            //move过程中上滑
            direction = DIRECTION.DOWN;
        } else if (mState == STATE_DOWN && moveY - lastMoveY < 0 && mIsTouching) {
            direction = DIRECTION.UP;
        } else if (distance > mScrollDistance && !mIsTouching) {
            direction = DIRECTION.DOWN;
        } else if (distance < -mScrollDistance && !mIsTouching) { //&& mScrollState != STATE_DOWN && mScrollState !=
            // STATE_DOWNING
            direction = DIRECTION.UP;
        }
        scrollAnimation(direction);
    }

    /**
     * 当网页长度不够时，根据手势判断
     *
     * @param downTouchY
     * @param lastTouchY
     * @param moveTouchY
     */
    public void doTouchScrollAnimation(int downTouchY, int lastTouchY, int moveTouchY) {
        if (mIsPageLoading || mPageFinishedAnimation
                || (mUiController != null && mUiController.getUi() != null
                && ((PhoneUi) mUiController.getUi()).showingNavScreenForExit())) {
            return;
        }
        if (mScrollDistance == 0) {
            mScrollDistance = getContext().getResources().getDimensionPixelOffset(R.dimen
                    .bottom_toolbar_scroll_animator_distance);
        }
        DIRECTION direction = null;
        int distance = moveTouchY - downTouchY;
        if (distance > mScrollDistance) {
            direction = DIRECTION.DOWN;
        } else if (distance < -mScrollDistance) {
            direction = DIRECTION.UP;
        }
        scrollAnimation(direction);
    }

    /**
     * 动画开始
     *
     * @param direction webview滚动方向
     */
    public void scrollAnimation(DIRECTION direction) {
        if (direction == null || (/*mToolbarCenterView != null && */mIsSearchResultPage && mState != STATE_UP)
                || (mUiController != null && mUiController.getUi() != null
                && ((PhoneUi) mUiController.getUi()).showingNavScreenForExit())) {
            return;
        }
        mIsDoneScrollAnimation = true;
        switch (direction) {
            case UP:
                doDownAnimation(direction);
                break;
            case DOWN:
                doUpAnimator(direction);
                break;
        }
    }

    /**
     * 收起动画
     *
     * @param direction 方向
     */
    private void doDownAnimation(DIRECTION direction) {
        if (mIsUpScrollAnimator || mState == STATE_UP || mState == STATE_UPPING
                || mIsDownScrollAnimator || mPageFinishedAnimation || mIsPageLoading ||
                BrowserSettings.getInstance().lockToolbar()) {
            return;
        }
        mScrollAnimator = ObjectAnimator.ofFloat(this,
                "translationY",
                0, mScrollDistance);
        setUpAnimator(DIRECTION.UP, false);
        startTitleAnimator(direction, false);
        mScrollAnimator.start();

        Animator dividerAnimator = ObjectAnimator.ofFloat(mDividerView,
                "translationY", 0,
                mScrollDistance);
        dividerAnimator.setDuration(SCROLL_ANIMATOR_TIME);
        dividerAnimator.start();
    }

    /**
     * 上滑动画
     *
     * @param direction 方向
     */
    private void doUpAnimator(DIRECTION direction) {
        doUpAnimator(direction, false);
    }

    private void doUpAnimator(DIRECTION direction, boolean isHomePage) {
        if ((mIsDownScrollAnimator ||
                mState == STATE_DOWN ||
                mState == STATE_DOWNING ||
                mIsUpScrollAnimator ||
                mPageFinishedAnimation ||
                mIsPageLoading ||
                BrowserSettings.getInstance().lockToolbar()) &&
                (mUiController.getCurrentTab() != null && !mUiController.getCurrentTab().isNativePage())) {
            return;
        }
        mScrollAnimator = ObjectAnimator.ofFloat(this,
                "translationY",
                mScrollDistance, 0);
        setUpAnimator(DIRECTION.DOWN, isHomePage);
        startTitleAnimator(direction, isHomePage);
        mScrollAnimator.start();

        Animator dividerAnimator1 = ObjectAnimator.ofFloat(mDividerView,
                "translationY",
                mScrollDistance, 0);
        dividerAnimator1.setDuration(SCROLL_ANIMATOR_TIME);
        dividerAnimator1.start();
    }


    /**
     * 设置动画属性
     *
     * @param direction
     */
    private void setUpAnimator(final DIRECTION direction, boolean isHomePage) {
        if (direction == null) {
            return;
        }
        if (isHomePage) {
            mScrollAnimator.setDuration(0); //不需要动画，但是需要view升上来
        } else {
            mScrollAnimator.setDuration(SCROLL_ANIMATOR_TIME);
        }
        mScrollAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (direction == DIRECTION.DOWN) {
                    mIsDownScrollAnimator = true;
                    mIsUpScrollAnimator = false;
                    mState = STATE_DOWNING;
                } else if (direction == DIRECTION.UP) {
                    mIsDownScrollAnimator = false;
                    mIsUpScrollAnimator = true;
                    mState = STATE_UPPING;
                }
                mIsTouching = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsDownScrollAnimator = false;
                mIsUpScrollAnimator = false;
                if (direction == DIRECTION.UP) {
                    mState = STATE_UP;
                } else if (direction == DIRECTION.DOWN) {
                    mState = STATE_DOWN;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    /**
     * toolbar 按钮
     *
     * @param direction
     */
    private void startTitleAnimator(DIRECTION direction, boolean isHomePage) {

        switch (direction) {
            case DOWN:
                if (mState == STATE_DOWN) {
                    return;
                }
                mTabSwitcherButton.setVisibility(VISIBLE);
                mMenuButton.setVisibility(VISIBLE);
                mTitleLoadingButton.setVisibility(VISIBLE);

                AnimationSet upSet = new AnimationSet(true);
                ScaleAnimation upScaleAnimation = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.0f);

                TranslateAnimation textTranslateUpAnimation = new TranslateAnimation(0, 0, -16, 0);
                upSet.addAnimation(upScaleAnimation);
                upSet.addAnimation(textTranslateUpAnimation);
                upSet.setDuration(ToolBar.SCROLL_ANIMATOR_TIME);
                upSet.setFillAfter(true);
                mTitleLoadingButton.startAnimation(upSet);

                TranslateAnimation btnTranslateUpAnimation = new TranslateAnimation(0, 0, mScrollDistance, 0);
                if (isHomePage) {
                    btnTranslateUpAnimation.setDuration(0);
                } else {
                    btnTranslateUpAnimation.setDuration(SCROLL_ANIMATOR_TIME);
                }
                btnTranslateUpAnimation.setFillAfter(true);
                mTabSwitcherButton.startAnimation(btnTranslateUpAnimation);
                mMenuButton.startAnimation(btnTranslateUpAnimation);

                break;
            case UP:
                AnimationSet downSet = new AnimationSet(true);
                ScaleAnimation downScaleAnimation = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.0f);

                TranslateAnimation translateDownAnimation = new TranslateAnimation(0, 0, 0, 16);

                downSet.addAnimation(translateDownAnimation);
                downSet.setDuration(ToolBar.SCROLL_ANIMATOR_TIME);
                downSet.setFillAfter(true);
                downSet.addAnimation(downScaleAnimation);
                mTitleLoadingButton.startAnimation(downSet);

                TranslateAnimation btnTranslateDownAnimation = new TranslateAnimation(0, 0,
                        0, mScrollDistance);
                btnTranslateDownAnimation.setDuration(SCROLL_ANIMATOR_TIME);
                btnTranslateDownAnimation.setFillAfter(true);
                mTabSwitcherButton.startAnimation(btnTranslateDownAnimation);
                mMenuButton.startAnimation(btnTranslateDownAnimation);

                btnTranslateDownAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //清除动画防点击
                        mTabSwitcherButton.setVisibility(INVISIBLE);
                        mMenuButton.setVisibility(INVISIBLE);
                        //mTitleLoadingButton.setVisibility(INVISIBLE);
                        mTabSwitcherButton.clearAnimation();
                        mMenuButton.clearAnimation();
                        mTitleLoadingButton.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                break;
        }
///        mToolbarCenterView.startScrollAnimation(direction, mScrollDistance);
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        try {
            if (mScrollDistance == 0) {
                mScrollDistance = getContext().getResources().getDimensionPixelOffset(R.dimen
                        .bottom_toolbar_scroll_animator_distance);
            }
            if (t == 0) {
                scrollAnimation(DIRECTION.DOWN);
                if (mUiController != null && mUiController.getUi() != null) {
                    ((BaseUi)mUiController.getUi()).changeWebViewHeight();
                }
            } else if (mWebView.getContentHeight() * mWebView.getScaleY() - (mWebView.getHeight() + t) == 0) {
                scrollAnimation(DIRECTION.UP);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 获取是否是搜索结果页
     *
     * @return
     */
    private boolean isSearchResultPage() {
        return false;
    }

    /**
     * 页面加载时初始化view
     */
    public void onPageLoadStarted(Tab tab) {
        if (tab != null && tab.isNativePage()) {
            return;
        }

        scrollAnimation(DIRECTION.DOWN);
        ((BaseUi) mUiController.getUi()).changeWebViewHeight();
        mIsPageLoading = true;
//        mToolbarCenterView.clearChildAnimation();
//        mToolbarCenterView.setToolbarState(mState);
//        if (tab != null && tab.inForeground()) {
//            mToolbarCenterView.setState(tab, ToolbarCenterView.STATE_LOADING);
//        }
//        if (!TextUtils.isEmpty(tab.getUrl()) && tab.getUrl().startsWith("https://")) {
//            mToolbarCenterView.setIconSafe(true);
//        } else {
//            mToolbarCenterView.setIconSafe(false);
//        }

        setIconSafe(!TextUtils.isEmpty(tab.getUrl()) && tab.getUrl().startsWith("https://"));
        mStopLoadBtn.setVisibility(VISIBLE);
        mMenuButtonID.setVisibility(GONE);
        mTitle.setVisibility(VISIBLE);
        mProgress.setVisibility(VISIBLE);
        mInLoad = true;

    }

    /**
     * 页面完成时动画
     */
    public void onPageLoadFinished(Tab tab) {
        if (!mIsPageLoading) {
            return;
        }
        mIsPageLoading = false;
        mInLoad = false;
        mPageFinishedAnimation = true;

        if (tab != null && !tab.isNativePage()) {
            // mToolbarCenterView.onPageLoadFinished(mIsSearchResultPage, tab);
            AnimationDrawable frameAnim = (AnimationDrawable) getResources().getDrawable(R.drawable
                    .toolbar_loading_animation);
            if (mUiController != null && mUiController.getCurrentTab() != null
                    && mUiController.getCurrentTab().isPrivateBrowsingEnabled()) {
                frameAnim = (AnimationDrawable) getResources().getDrawable(R.drawable.toolbar_loading_animation_incognito);
            }
            mStopLoadBtn.setImageDrawable(frameAnim);
            frameAnim.start();
            mStopLoadBtn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mStopLoadBtn.setVisibility(GONE);
                    mMenuButtonID.setVisibility(VISIBLE);
                    mPageFinishedAnimation = false;
                }
            }, STOP_BTN_ANIMATION_TIME);
        } else {
            mStopLoadBtn.setVisibility(GONE);
            mMenuButtonID.setVisibility(VISIBLE);
            mPageFinishedAnimation = false;
        }
        mProgress.setVisibility(GONE);
        setIconSafe(tab != null && !TextUtils.isEmpty(tab.getUrl()) && tab.getUrl().startsWith("https://"));
//        if (tab != null && !TextUtils.isEmpty(tab.getUrl()) && tab.getUrl().startsWith("https://")) {
//            mToolbarCenterView.setIconSafe(true);
//        } else {
//            mToolbarCenterView.setIconSafe(false);
//        }

    }

    public void onPageLoadStopped(Tab tab) {
        mIsPageLoading = false;
        updateToolbarStyle();
        mStopLoadBtn.setVisibility(GONE);
        mMenuButtonID.setVisibility(VISIBLE);
    }

    public boolean getIsSearchResultPage() {
        return mIsSearchResultPage;
    }

    public void updateToolbarStyle() {
//        if (mToolbarCenterView != null) {
//            mToolbarCenterView.updateStyle();
//        }
    }

    public void updateLayout(boolean isNativePage) {
        if (mUiController.getCurrentTab() != null) {
//            if (mToolbarCenterView != null) {
//                mToolbarCenterView.clearAnimation();
//            }
            if (isNativePage) {
                mMenuLayoutAnimator = ValueAnimator.ofInt(mMenuLayoutParams.rightMargin, getResources()
                        .getDimensionPixelSize(R.dimen.toolbar_native_margin_init));
                mTabSwitcherLayoutAnimator = ValueAnimator.ofInt(mTabSwitcherLayoutParams.leftMargin, getResources()
                        .getDimensionPixelSize(R.dimen.toolbar_native_margin_init));
            } else {

                mMenuLayoutAnimator = ValueAnimator.ofInt(mMenuLayoutParams.rightMargin, getResources()
                        .getDimensionPixelSize(R.dimen.toolbar_menu_margin_right));
                mTabSwitcherLayoutAnimator = ValueAnimator.ofInt(mTabSwitcherLayoutParams.leftMargin, getResources()
                        .getDimensionPixelSize(R.dimen.toolbar_tabcount_margin_left));
            }
            if (mMenuLayoutAnimator != null) {
                mMenuLayoutAnimator.removeAllUpdateListeners();
                mMenuLayoutAnimator.cancel();
                mMenuLayoutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mMenuLayoutParams.rightMargin = (int) valueAnimator.getAnimatedValue();
                        mMenuButton.requestLayout();
                        mMenuButtonID.postInvalidate();
                    }
                });
                mMenuLayoutAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                mMenuLayoutAnimator.setDuration(getResources().getInteger(R.integer.tab_animation_duration));
                mMenuLayoutAnimator.start();
            }
            if (mTitleLayoutAnimator != null) {
                mTitleLayoutAnimator.removeAllUpdateListeners();
                mTitleLayoutAnimator.cancel();
                mTitleLayoutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mTitleLayoutParams.rightMargin = (int) valueAnimator.getAnimatedValue();
                        mTitleLoadingButton.requestLayout();
                        mTitle.postInvalidate();
                    }
                });
                mTitleLayoutAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                mTitleLayoutAnimator.setDuration(getResources().getInteger(R.integer.tab_animation_duration));
                mTitleLayoutAnimator.start();
            }
            if (mTabSwitcherLayoutAnimator != null) {
                mTabSwitcherLayoutAnimator.removeAllUpdateListeners();
                mTabSwitcherLayoutAnimator.cancel();
                mTabSwitcherLayoutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mTabSwitcherLayoutParams.leftMargin = (int) valueAnimator.getAnimatedValue();
                        mTabSwitcherButton.requestLayout();
                        mTabSwitcherButtonID.postInvalidate();
                    }
                });
                mTabSwitcherLayoutAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                mTabSwitcherLayoutAnimator.setDuration(getResources().getInteger(R.integer.tab_animation_duration));
                mTabSwitcherLayoutAnimator.start();
            }
        }
    }

    /**
     * 更新tab切换时的toolbar按钮状态
     */
    public void updateToolbarBtnState() {
        mIsSearchResultPage = isSearchResultPage();
        // mToolbarCenterView.setIsSearchResultPage(mIsSearchResultPage);
        Tab tab = mUiController == null ? null : mUiController.getCurrentTab();
        if (tab == null) {
            return;
        }
        if (tab.inPageLoad() && !tab.isNativePage()) {
            mInLoad = true;
            mProgress.setVisibility(VISIBLE);
            mIsPageLoading = true;
            setProgress(tab.getLoadProgress());
            mStopLoadBtn.setVisibility(VISIBLE);
            mMenuButtonID.setVisibility(GONE);
            // mToolbarCenterView.setState(tab, ToolbarCenterView.STATE_LOADING);
        } else {
            mInLoad = false;
            mIsPageLoading = false;
            mProgress.setVisibility(GONE);
            mStopLoadBtn.setVisibility(GONE);
            mMenuButtonID.setVisibility(VISIBLE);
            // mToolbarCenterView.setState(tab, ToolbarCenterView.STATE_LOADED);
        }
    }

    public void onConfigurationChanged(Configuration config) {
        updateLayout(mUiController.getCurrentTab() != null && mUiController.getCurrentTab().isNativePage());
        if (mCommonMenu != null) {
            mCommonMenu.onConfigurationChanged(config);
        }
//        if (mToolbarCenterView != null) {
//            mToolbarCenterView.onConfigurationChanged(config);
//        }
    }
    public void setIsDoneScrollAnimation(boolean isDoneScrollAnimation) {
        mIsDoneScrollAnimation = isDoneScrollAnimation;
    }

    public boolean getIsDoneScrollAnimation() {
        return mIsDoneScrollAnimation;
    }

    private class DragLayoutCallBack extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mContextView;
        }


        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            mSwipeSuccess = false;
            if (left <= -mMaxScrollDistance) {
                left = -mMaxScrollDistance;
            } else if (left >= mMaxScrollDistance) {
                left = mMaxScrollDistance;
            }
            if (!mMenuShow) {
                if (mProgressLeft != null) {
                    mProgressLeft.setVisibility(VISIBLE);
                }
                mMenuShow = true;
            }
            mDragRelease = false;
            mScrollDirection = SCROLLING_HORIZONTAL;
            layoutContextView(left);
            return left;
        }

        @Override
        public int getOrderedChildIndex(int index) {
            int indexTop = indexOfChild(mProgressLeft);
            int indexBottom = indexOfChild(mContextView);
            if (index == indexTop) {
                return indexBottom;
            }
            return index;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getMeasuredWidth();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            mDragRelease = true;
            mViewDragHelper.settleCapturedViewAt(0, 0);
            invalidate();
        }
    }

    public DownloadFile showDownloadFile() {
        if (mDownloadFile == null) {
            ViewStub Stub = (ViewStub) ((ViewGroup) getParent()).findViewById(R.id.view_stub_file);
            mDownloadFile = (DownloadFile) Stub.inflate();
        }
        int end[] = new int[2];

        mMenuButtonID.getLocationInWindow(end);
        mDownloadFile.setTarget(new Point(end[0], end[1]));
        mDownloadFile.moveAnimation( );
        return mDownloadFile;
    }

    public int getToolBarCenterViewHeight() {
        switch (getToolbarState()) {
            case STATE_DOWN:
            case STATE_DOWNING:
                return getHeight();
            case STATE_UP:
            case STATE_UPPING:
                return getHeight() / 2;
        }

        return 0;
    }

    private void setIconSafe(boolean isShowSafeIcon) {
        if (mSafeIcon == null) {
            return;
        }
        if (isShowSafeIcon) {
            mSafeIcon.setVisibility(VISIBLE);
        } else {
            mSafeIcon.setVisibility(GONE);
        }
    }
}
