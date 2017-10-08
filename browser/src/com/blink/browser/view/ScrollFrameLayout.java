
package com.blink.browser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.blink.browser.PhoneUi;
import com.blink.browser.R;
import com.blink.browser.Tab;
import com.blink.browser.ToolBar;
import com.blink.browser.UiController;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.widget.AnimationListener;

public class ScrollFrameLayout extends FrameLayout {
    public static final int TOUCH_SHAKE = 10; // 防抖动

    private ToolBar mToolBar;
    private UiController mUiController;
    int mDownScrollY, mScrollY, mLastScrollMoveY;
    int mDownTouchY, mLastTouchY, mTouchY;
    private ImageView mSlideLeft, mSlideRight;
    private int mSlideDistance;
    private boolean mSlideSuccess = false;
    private IScrollListener mScrollListener;
    private float mDragLastTouchY, mDragLastTouchX;
    private static final int DRAG_ANIMATOR_TIME = 200;
    private boolean mDraggingFlag = false;
    private boolean mDragDisable = false;
    private int mDragDistanceX, mDragDistanceY;
    private int mTouchSlop;

    public ScrollFrameLayout(Context context) {
        super(context);
    }

    public ScrollFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mToolBar = (ToolBar) findViewById(R.id.bottom_bar).findViewById(R.id.tool_bar);

        mSlideLeft = (ImageView) findViewById(R.id.slide_left);
        mSlideRight = (ImageView) findViewById(R.id.slide_right);
        mSlideDistance = getResources().getDimensionPixelSize(R.dimen.slide_back_or_forward_distance);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2;
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }

    public void setUiController(UiController controller) {
        mUiController = controller;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mToolBar == null || !mToolBar.isShown() || !mToolBar.isProcessing(ev.getX(), ev.getY())) {
            handleScrollEvent(ev);
        }
        handleSwipeEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void handleSwipeEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            mDragDisable = true;
            if (mDraggingFlag) {
                hideDragView();
            }
            return;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDragLastTouchX = ev.getX();
                mDragLastTouchY = ev.getY();
                mDraggingFlag = false;
                mDragDistanceX = 0;
                mDragDistanceY = 0;
                if (isInDragForwardBackRect(ev)) {
                    mDragDisable = false;
                } else {
                    mDragDisable = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float dx = ev.getX() - mDragLastTouchX;
                final float dy = ev.getY() - mDragLastTouchY;
                mDragDistanceX += dx;
                mDragDistanceY += dy;
                if (Math.abs(mDragDistanceY) > 1 || Math.abs(mDragDistanceX) > 1) {
                    if (Math.abs(mDragDistanceY) > mSlideDistance){
                        mDragDisable = true;
                        if (mDraggingFlag) {
                            hideDragView();
                        }
                        return;
                    } else if (!mDraggingFlag && Math.abs(dx) < Math.abs(dy)) {
                        mDragDisable = true;
                    }
                    if (!mDragDisable) {
                        mDraggingFlag = true;
                        if (mDragDistanceX > 0) {
                            if (mUiController.canGoBack()) {
                                mSlideRight.setVisibility(VISIBLE);
                                mSlideRight.setTranslationX(mDragDistanceX);
                                mSlideLeft.setVisibility(GONE);
                                if (mDragDistanceX > mSlideDistance) {
                                    mSlideSuccess = true;
                                } else {
                                    mSlideSuccess = false;
                                }
                            }
                        } else {
                            if (mUiController.canGoForward()) {
                                mSlideLeft.setVisibility(VISIBLE);
                                mSlideLeft.setTranslationX(mDragDistanceX);
                                mSlideRight.setVisibility(GONE);
                                if (-mDragDistanceX > mSlideDistance) {
                                    mSlideSuccess = true;
                                } else {
                                    mSlideSuccess = false;
                                }
                            }
                        }
                        invalidate();
                    }
                }
                mDragLastTouchX = ev.getX();
                mDragLastTouchY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDraggingFlag) {
                    hideDragView();
                }
                mDragLastTouchX = ev.getX();
                mDragLastTouchY = ev.getY();
                break;
        }
    }

    private void hideDragView() {
        if (!mDragDisable) {
            TranslateAnimation translateAnimation = null;
            if (mSlideRight.getVisibility() == VISIBLE) {
                if (mSlideRight.getTranslationX() > mSlideDistance) {
                    translateAnimation = new TranslateAnimation(0, getMeasuredWidth() + mSlideRight.getWidth(), 0, 0);
                } else {
                    translateAnimation = new TranslateAnimation(0, mSlideRight.getLeft(), 0, 0);
                }
                translateAnimation.setDuration(DRAG_ANIMATOR_TIME);
                translateAnimation.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSlideLeft.setVisibility(GONE);
                        mSlideRight.setVisibility(GONE);
                        if (mSlideSuccess) {
                            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PAGE_EVENTS, AnalyticsSettings.ID_BACKWARD);
                            mUiController.goBack();
                        }

                    }
                });
                mSlideRight.startAnimation(translateAnimation);
            } else if (mSlideLeft.getVisibility() == VISIBLE) {
                if (-mSlideLeft.getTranslationX() > mSlideDistance) {
                    translateAnimation = new TranslateAnimation(0, -getMeasuredWidth() - mSlideLeft.getWidth(), 0, 0);
                } else {
                    translateAnimation = new TranslateAnimation(0, mSlideLeft.getMeasuredWidth(), 0, 0);
                }
                translateAnimation.setDuration(DRAG_ANIMATOR_TIME);
                translateAnimation.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSlideLeft.setVisibility(GONE);
                        mSlideRight.setVisibility(GONE);
                        if (mSlideSuccess) {
                            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PAGE_EVENTS, AnalyticsSettings.ID_FORWARD);
                            mUiController.goForward();
                        }
                    }
                });
                mSlideLeft.startAnimation(translateAnimation);
            }
        } else {
            AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
            alphaAnimation.setDuration(DRAG_ANIMATOR_TIME);
            alphaAnimation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    mSlideLeft.setVisibility(GONE);
                    mSlideRight.setVisibility(GONE);
                }
            });
            if (mSlideRight.getVisibility() == VISIBLE) {
                mSlideRight.startAnimation(alphaAnimation);
            } else {
                mSlideLeft.startAnimation(alphaAnimation);
            }
        }
        mDragDisable = true;
        mDraggingFlag = false;
    }

    private boolean isInDragForwardBackRect(MotionEvent ev) {
        if (mUiController.getUi() instanceof PhoneUi && ((PhoneUi)  mUiController.getUi()).showingNavScreen()) {
            return false;
        }
        if (mUiController.getViewPageController().isNeedHintToolbar()) {
            return false;
        }
        if ((mToolBar != null && ev.getY() < mToolBar.getTop()) && (ev.getX() < mTouchSlop || ev.getX() + mTouchSlop > getMeasuredWidth())) {
            return true;
        }
        return false;
    }

    private void handleScrollEvent(MotionEvent ev) {
        if (mUiController == null || mToolBar == null) {
            return;
        }

        Tab current = mUiController.getCurrentTab();
        if (current != null && !current.isNativePage() && current.getWebView() != null) {
            WebView webView = current.getWebView();
            if (webView == null) {
                return;
            }
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDownScrollY = webView.getScrollY();
                    mLastScrollMoveY = mDownScrollY;
                    mScrollY = mDownScrollY;
                    mToolBar.resetDirection();
                    //网页长度不够的情况下
                    mDownTouchY = (int) ev.getRawY();
                    mLastTouchY = mDownTouchY;
                    mTouchY = mDownTouchY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(webView.getScrollY() - mLastScrollMoveY) > TOUCH_SHAKE) {
                        mLastScrollMoveY = mScrollY;
                        mScrollY = webView.getScrollY();
                    }
                    if (Math.abs(ev.getRawY() - mLastTouchY) > TOUCH_SHAKE) {
                        mLastTouchY = mTouchY;
                        mTouchY = (int) ev.getRawY();
                    }
                    if (Math.abs(mTouchY - mLastTouchY) > TOUCH_SHAKE && webView.getContentHeight() * webView.getScale() <= webView.getHeight()) {
                        //网页长度不够的情况
                        mToolBar.doTouchScrollAnimation(mDownTouchY, mLastTouchY, mTouchY);
                    } else if (Math.abs(webView.getScrollY() - mLastScrollMoveY) > TOUCH_SHAKE || webView.getScrollY() == 0
                            || (webView.getContentHeight() * webView.getScale() - (webView.getHeight() + webView.getScrollY()) == 0)) {
                        mToolBar.doScrollAnimation(mDownTouchY, mLastTouchY, mTouchY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mScrollListener != null && mToolBar.getIsDoneScrollAnimation()) {
                        mScrollListener.onToolbarStateChanged();
                        mToolBar.setIsDoneScrollAnimation(false);
                    }
            }
        }
    }

    public void registerScrollListener(IScrollListener scrollListener) {
        this.mScrollListener = scrollListener;
    }

    public void unRegisterScrollListener() {
        this.mScrollListener = null;
    }

    public interface IScrollListener {
        void onToolbarStateChanged();
    }
}
