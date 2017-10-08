package com.blink.browser.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.BaseUi;
import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;

import java.util.Map;

public class LeadPageView extends RelativeLayout implements View.OnClickListener {

    private static final int STEP_BACKHOME_ANIMA = 1;
    private static final int STEP_LEFTHOME_ANIMA = 2;
    private static final int STEP_RIGHTHOME_ANIMA = 3;

    private static final long BACKHOME_ANIMA_START = 300;
    private static final long BACKHOME_ANIMA_END = 200;

    private static final long LEFT_ANIMA_START = 400;
    private static final long LEFT_ANIMA_END = 200;

    private static final long RIGHT_ANIMA_START = 400;
    private static final long RIGHT_ANIMA_END = 200;

    private static final long THEMB_ANIMA_TIME = 200;

    private CircleImageView mBackHome;
    private FrameLayout mTabswitcherToolbar;
    private ImageView mStopRefresh;
    private ImageView mLeftImageView;
    private ImageView mRightImageView;
    private TextView mUrlView;
    private RelativeLayout mSummaryView;
    private TextView mSummary;
    private TextView mTabNumber;
    private RelativeLayout mLeadSwipeView;
    private View mLeftProgress;
    private View mRightProgress;
    private BaseUi mUi;
    private ImageView mThemb;

    private int mScrollHorizontalDistance;
    private int mMaxScrollDistance;
    private int mTouchSlop;
    private int mStep = 0;
    private boolean mBackHomeIsRuning;
    private boolean mLeftSetIsRuning;
    private boolean mRightSetIsRuning;
    private String mUrl;
    private Map<String, String> mHeaders;
    private int mThembSize;
    private int mActionBarH = 0;

    public LeadPageView(Context context, String url, Map<String, String> headers, BaseUi ui) {
        super(context);
        this.mUrl = url;
        mUi = ui;
        this.mHeaders = headers;
        initView();
        initData();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.lead_page_view, this);
        mLeadSwipeView = (RelativeLayout) findViewById(R.id.lead_swipe_view);
        mTabswitcherToolbar = (FrameLayout) findViewById(R.id.lead_tabswitcher_toolbar);
        mStopRefresh = (ImageView) findViewById(R.id.lead_stop_refresh);
        mUrlView = (TextView) findViewById(R.id.lead_center_view);
        mSummaryView = (RelativeLayout) findViewById(R.id.summary_lyt);
        mSummary = (TextView) findViewById(R.id.summary);
        mTabNumber = (TextView) findViewById(R.id.page_number_tab_id);
        mBackHome = (CircleImageView) findViewById(R.id.lead_back_home);
        mLeftImageView = (ImageView) findViewById(R.id.lead_swipe_left_menu);
        mRightImageView = (ImageView) findViewById(R.id.lead_swipe_right_menu);
        mLeftProgress = findViewById(R.id.left_progress);
        mRightProgress = findViewById(R.id.right_progress);
        mBackHome.setRadius(getResources().getDimensionPixelSize(R.dimen.sub_action_button_size) / 2);

        view.setOnClickListener(this);
        mThemb = getThumbImage();
        mThembSize = getResources().getDimensionPixelSize(R.dimen.leadpage_themb_wh);
    }

    private ImageView getThumbImage() {
        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(R.drawable.ic_browser_indicator);
        return imageView;
    }

    private void initData() {
        if (!TextUtils.isEmpty(mUrl)) {
            mUrlView.setText(mUrl);
        }
        mScrollHorizontalDistance = getResources().getDimensionPixelSize(R.dimen.scroll_toolbar_horizontal_distance);
        mMaxScrollDistance = getResources().getDimensionPixelSize(R.dimen.max_scroll_distance);
        ViewConfiguration config = ViewConfiguration.get(getContext());
        mTouchSlop = config.getScaledTouchSlop();

        mLeftImageView.setVisibility(View.VISIBLE);
        mRightImageView.setVisibility(View.VISIBLE);

        mThembSize = getResources().getDimensionPixelSize(R.dimen.leadpage_themb_wh);
        if (BrowserSettings.getInstance().getShowStatusBar()) {
            mActionBarH = DisplayUtil.getStatusBarHeight(getContext());
        }
    }

    public void startBackHomeAnima() {
        mStep = STEP_BACKHOME_ANIMA;
        int toY = (int) (mTouchSlop + mMaxScrollDistance * 1.7);
        final ObjectAnimator translationStartY = ObjectAnimator.ofFloat(mBackHome, "translationY", 0, -toY);
        translationStartY.setDuration(BACKHOME_ANIMA_START);
        translationStartY.setInterpolator(new AccelerateInterpolator());
        translationStartY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                removeView(mThemb);
                mThemb = null;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                showSummaryAnima(R.string.leadpage_home);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        int[] location = new int[2];
        mLeadSwipeView.getLocationOnScreen(location);
        location = new int[]{location[0], location[1] - mActionBarH};
        if (mThemb == null) {
            mThemb = getThumbImage();
        }
        LayoutParams layoutParams = new RelativeLayout.LayoutParams(mThembSize, mThembSize);
        layoutParams.topMargin = location[1] + mLeadSwipeView.getHeight() / 2 - mThembSize / 2;
        layoutParams.leftMargin = DisplayUtil.getScreenWidth(getContext()) / 2 + mBackHome.getHeight() / 2 - mThembSize / 2;
        addView(mThemb, layoutParams);
        mThemb.setVisibility(View.GONE);


        ObjectAnimator leftAnimaX = ObjectAnimator.ofFloat(mTabswitcherToolbar, "translationX", 0,
                -getResources().getDimensionPixelSize(R.dimen.toolbar_native_margin_init) / 2);
        leftAnimaX.setDuration(200);
        leftAnimaX.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator rightAnimaX = ObjectAnimator.ofFloat(mStopRefresh, "translationX", 0,
                getResources().getDimensionPixelSize(R.dimen.toolbar_native_margin_init) / 2);
        rightAnimaX.setDuration(200);
        rightAnimaX.setInterpolator(new AccelerateInterpolator());

        //滑块滑动
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mThemb, "scaleX", 0.8f, 1f);
        scaleX.setDuration(100);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mThemb, "scaleY", 0.8f, 1f);
        scaleY.setDuration(100);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mStopRefresh.setImageResource(R.drawable.ic_browser_toolbar_close);
                mUrlView.setVisibility(View.VISIBLE);
                mBackHome.setFillColor(getResources().getColor(R.color.back_home_bg));
                mBackHome.invalidate();
                mThemb.setVisibility(View.VISIBLE);
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

        ObjectAnimator mThembTranslation = ObjectAnimator.ofFloat(mThemb, "translationY", 0, -toY);
        mThembTranslation.setDuration(200);
        mThembTranslation.setInterpolator(new AccelerateDecelerateInterpolator());
        mThembTranslation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                translationStartY.setStartDelay(THEMB_ANIMA_TIME);
                translationStartY.start();
                mBackHome.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        AnimatorSet mStartAnimaSet = new AnimatorSet();
        mStartAnimaSet.play(leftAnimaX).with(rightAnimaX);
        mStartAnimaSet.play(scaleX).with(scaleY);
        mStartAnimaSet.play(scaleX).after(rightAnimaX);
        mStartAnimaSet.play(mThembTranslation).after(scaleX);
        mStartAnimaSet.start();
        mBackHomeIsRuning = true;
    }


    public void startLeftAnima() {
        mStep = STEP_LEFTHOME_ANIMA;
        int toY = (int) (mTouchSlop + mMaxScrollDistance * 1.7);

        ViewWrapper wrapper = new ViewWrapper(mLeftProgress);
        ObjectAnimator leftAnimatorStart = ObjectAnimator.ofInt(wrapper, "width", 0, 4 * mScrollHorizontalDistance);
        leftAnimatorStart.setDuration(LEFT_ANIMA_START);
        leftAnimatorStart.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator leftImageViewAnimator = ObjectAnimator.ofFloat(mLeftImageView, "translationX", 0, mScrollHorizontalDistance / 2);
        leftImageViewAnimator.setDuration(LEFT_ANIMA_START);
        leftImageViewAnimator.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator swipeViewAnimator = ObjectAnimator.ofFloat(mLeadSwipeView, "translationX", 0, mScrollHorizontalDistance);
        swipeViewAnimator.setDuration(LEFT_ANIMA_START);
        swipeViewAnimator.setInterpolator(new AccelerateInterpolator());

        leftAnimatorStart.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mLeftProgress.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                showSummaryAnima(R.string.leadpage_back);
                if (mLeftSetIsRuning) {
                    mLeftSetIsRuning = false;
                }
                removeView(mThemb);
                mThemb = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        //滑块滑动
        int[] location = new int[2];
        mLeadSwipeView.getLocationOnScreen(location);
        location = new int[]{location[0], location[1] - mActionBarH};
        if (mThemb == null) {
            mThemb = getThumbImage();
        }
        LayoutParams layoutParams = new RelativeLayout.LayoutParams(mThembSize, mThembSize);
        layoutParams.topMargin = location[1] + mLeadSwipeView.getHeight() / 2 - mThembSize / 2;
        layoutParams.leftMargin = (int) mTabNumber.getX() - getResources().getDimensionPixelSize(R.dimen.toolbar_native_margin_init) / 2;
        addView(mThemb, layoutParams);
        mThemb.setVisibility(View.GONE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mThemb, "scaleX", 0.8f, 1f);
        scaleX.setDuration(100);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mThemb, "scaleY", 0.8f, 1f);
        scaleY.setDuration(100);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator mThembTranslation = ObjectAnimator.ofFloat(mThemb, "translationX", 0, mScrollHorizontalDistance * 2);
        mThembTranslation.setDuration(200);
        mThembTranslation.setInterpolator(new AccelerateDecelerateInterpolator());
        final AnimatorSet mEndAnimaSet = new AnimatorSet();
        mEndAnimaSet.play(scaleX).with(scaleY);
        mEndAnimaSet.play(mThembTranslation).after(scaleX);
        mEndAnimaSet.play(leftAnimatorStart).after(mThembTranslation);
        mEndAnimaSet.play(leftImageViewAnimator).with(leftAnimatorStart);
        mEndAnimaSet.play(swipeViewAnimator).with(leftAnimatorStart);


        ObjectAnimator translationEndY = ObjectAnimator.ofFloat(mBackHome, "translationY", -toY, 0);
        translationEndY.setDuration(BACKHOME_ANIMA_END);
        translationEndY.setInterpolator(new AccelerateInterpolator());
        translationEndY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mBackHome.setVisibility(View.GONE);
                mThemb.setVisibility(View.VISIBLE);
                mEndAnimaSet.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        translationEndY.start();
        mLeftSetIsRuning = true;
        dismissSummaryAnima();
    }

    public void startRightAnima() {
        mStep = STEP_RIGHTHOME_ANIMA;
        ViewWrapper wrapper = new ViewWrapper(mRightProgress);
        ObjectAnimator rightAnimatorStart = ObjectAnimator.ofInt(wrapper, "width", 0, 4 * mScrollHorizontalDistance);
        rightAnimatorStart.setDuration(RIGHT_ANIMA_START);
        rightAnimatorStart.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator rightImageViewAnimator = ObjectAnimator.ofFloat(mRightImageView, "translationX", 0, -mScrollHorizontalDistance / 2);
        rightImageViewAnimator.setDuration(RIGHT_ANIMA_START);
        rightImageViewAnimator.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator swipeViewAnimator = ObjectAnimator.ofFloat(mLeadSwipeView, "translationX", 0, -mScrollHorizontalDistance);
        rightImageViewAnimator.setDuration(RIGHT_ANIMA_START);
        rightImageViewAnimator.setInterpolator(new AccelerateInterpolator());


        rightAnimatorStart.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mRightProgress.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                showSummaryAnima(R.string.leadpage_forward);
                mRightImageView.setVisibility(VISIBLE);
                removeView(mThemb);
                mThemb = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        //滑块滑动
        int[] location = new int[2];
        mLeadSwipeView.getLocationOnScreen(location);
        location = new int[]{location[0], location[1] - mActionBarH};
        if (mThemb == null) {
            mThemb = getThumbImage();
        }
        LayoutParams layoutParams = new RelativeLayout.LayoutParams(mThembSize, mThembSize);
        layoutParams.topMargin = location[1] + mLeadSwipeView.getHeight() / 2 - mThembSize / 2;
        layoutParams.leftMargin = (int) mStopRefresh.getX() + getResources().getDimensionPixelSize(R.dimen.toolbar_native_margin_init) / 2;
        addView(mThemb, layoutParams);
        mThemb.setVisibility(View.GONE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mThemb, "scaleX", 0.8f, 1f);
        scaleX.setDuration(100);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mThemb, "scaleY", 0.8f, 1f);
        scaleY.setDuration(100);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator mThembTranslation = ObjectAnimator.ofFloat(mThemb, "translationX", 0, -mScrollHorizontalDistance * 2);
        mThembTranslation.setDuration(200);
        mThembTranslation.setInterpolator(new AccelerateDecelerateInterpolator());
        final AnimatorSet mEndAnimaSet = new AnimatorSet();
        mEndAnimaSet.play(scaleX).with(scaleY);
        mEndAnimaSet.play(mThembTranslation).after(scaleX);
        mEndAnimaSet.play(rightAnimatorStart).after(mThembTranslation);
        mEndAnimaSet.play(rightImageViewAnimator).with(rightAnimatorStart);
        mEndAnimaSet.play(swipeViewAnimator).with(rightAnimatorStart);


        ViewWrapper wrapperleft = new ViewWrapper(mLeftProgress);
        ObjectAnimator leftAnimatorEnd = ObjectAnimator.ofInt(wrapperleft, "width", 4 * mScrollHorizontalDistance, 0);
        leftAnimatorEnd.setDuration(LEFT_ANIMA_END);
        leftAnimatorEnd.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator leftImageViewAnimator = ObjectAnimator.ofFloat(mLeftImageView, "translationX", mScrollHorizontalDistance / 2, 0);
        leftImageViewAnimator.setDuration(LEFT_ANIMA_END);
        leftImageViewAnimator.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator swipeViewAnimatorleft = ObjectAnimator.ofFloat(mLeadSwipeView, "translationX", mScrollHorizontalDistance, 0);
        swipeViewAnimatorleft.setDuration(LEFT_ANIMA_END);
        swipeViewAnimatorleft.setInterpolator(new AccelerateInterpolator());

        leftAnimatorEnd.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mLeftProgress.setVisibility(View.GONE);
                mLeftImageView.setVisibility(View.GONE);
                mThemb.setVisibility(View.VISIBLE);
                mEndAnimaSet.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        AnimatorSet mStartAnimaSet = new AnimatorSet();
        mStartAnimaSet.play(leftAnimatorEnd).with(leftImageViewAnimator);
        mStartAnimaSet.play(leftAnimatorEnd).with(swipeViewAnimatorleft);
        mStartAnimaSet.start();
        mRightSetIsRuning = true;
        dismissSummaryAnima();

    }

    public void startEndAnima() {
        ViewWrapper wrapperRight = new ViewWrapper(mRightProgress);
        ObjectAnimator rightAnimatorEnd = ObjectAnimator.ofInt(wrapperRight, "width", 4 * mScrollHorizontalDistance, 0);
        rightAnimatorEnd.setDuration(RIGHT_ANIMA_END);
        rightAnimatorEnd.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator rightImageViewAnimator = ObjectAnimator.ofFloat(mRightImageView, "translationX", -mScrollHorizontalDistance / 2, 0);
        rightImageViewAnimator.setDuration(RIGHT_ANIMA_END);
        rightImageViewAnimator.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator swipeViewAnimatorleft = ObjectAnimator.ofFloat(mLeadSwipeView, "translationX", -mScrollHorizontalDistance, 0);
        swipeViewAnimatorleft.setDuration(RIGHT_ANIMA_END);
        swipeViewAnimatorleft.setInterpolator(new AccelerateInterpolator());

        rightAnimatorEnd.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mRightProgress.setVisibility(View.GONE);
                mUi.dimissLeadPage();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        AnimatorSet mStartAnimaSet = new AnimatorSet();
        mStartAnimaSet.play(rightAnimatorEnd).with(rightImageViewAnimator);
        mStartAnimaSet.play(rightAnimatorEnd).with(swipeViewAnimatorleft);
        mStartAnimaSet.start();
        dismissSummaryAnima();
    }

    private void showSummaryAnima(final int rid) {
        ObjectAnimator summaryViewAlpha = ObjectAnimator.ofFloat(mSummaryView, "alpha", 0f, 1f);
        summaryViewAlpha.setDuration(50);
        summaryViewAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator summaryAlpha = ObjectAnimator.ofFloat(mSummary, "alpha", 0f, 1f);
        summaryAlpha.setDuration(150);
        summaryAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        summaryAlpha.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mSummaryView.setVisibility(View.VISIBLE);
                mSummary.setVisibility(View.VISIBLE);
                mSummary.setText(rid);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                switch (mStep) {
                    case STEP_BACKHOME_ANIMA:
                        if (mBackHomeIsRuning) {
                            mBackHomeIsRuning = false;
                        }
                        break;
                    case STEP_LEFTHOME_ANIMA:
                        if (mLeftSetIsRuning) {
                            mLeftSetIsRuning = false;
                        }
                        break;
                    case STEP_RIGHTHOME_ANIMA:
                        if (mRightSetIsRuning) {
                            mRightSetIsRuning = false;
                        }
                        break;
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        AnimatorSet mStartAnimaSet = new AnimatorSet();
        mStartAnimaSet.play(summaryViewAlpha).with(summaryAlpha);
        mStartAnimaSet.start();
    }

    private void dismissSummaryAnima() {
        mSummaryView.setVisibility(View.INVISIBLE);
        mSummary.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (mStep) {
            case STEP_BACKHOME_ANIMA:
                if (!mBackHomeIsRuning) {
                    startLeftAnima();
                }
                break;
            case STEP_LEFTHOME_ANIMA:
                if (!mLeftSetIsRuning) {
                    startRightAnima();
                }
                break;
            case STEP_RIGHTHOME_ANIMA:
                if (!mRightSetIsRuning) {
                    startEndAnima();
                }
                break;
        }
    }

    public void dismiss() {
        mUi.dimissLeadPage();
    }

    private class ViewWrapper {
        private View mTarget;

        public ViewWrapper(View target) {
            mTarget = target;
        }

        public int getWidth() {
            return mTarget.getLayoutParams().width;
        }

        public void setWidth(int width) {
            mTarget.getLayoutParams().width = width;
            mTarget.requestLayout();
        }
    }
}
