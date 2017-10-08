package com.blink.browser.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.Tab;
import com.blink.browser.ToolBar;
import com.blink.browser.UiController;

public class ToolbarCenterView extends RelativeLayout {
    public static final int CENTER_VIEW_SCROLL_TIME = 100;
    public static final int STATE_HOMEPAGE = 1;
    public static final int STATE_LOADING = 2;
    public static final int STATE_LOADED = 3;
    private int mState;
    private int mToolbarState;

    private ImageView mSafeIcon;
    private RelativeLayout mWebTitleParent;
    private UiController mUiController;
    private TranslateAnimation mTranslateAnimation;
    private OnClickListener mClickListener;

    protected TextView mUrlInput;
    private boolean mIsSearchResultPage;
    AnimationSet loadedViewDownSet;
    private boolean mIsDoLoadedFinishAnimation;

    public ToolbarCenterView(Context context) {
        super(context);
    }

    public ToolbarCenterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWebTitleParent = (RelativeLayout) findViewById(R.id.title_loading_view);
        mUrlInput = (TextView) findViewById(R.id.web_view_title_view);
        mSafeIcon = (ImageView) findViewById(R.id.safe_icon);
    }

    public void setOnItemClickListener(OnClickListener listener) {
        if (listener == null) {
            return;
        }
        mClickListener = listener;
        mWebTitleParent.setOnClickListener(mClickListener);
    };

    public void setState(Tab tab, int state) {
        if (tab == null) {
            return;
        }
        boolean isNative = tab.isNativePage();
        mState = state;
        switch (mState) {
            case STATE_HOMEPAGE:
                mWebTitleParent.setVisibility(GONE);
                break;
            case STATE_LOADING:
                mWebTitleParent.setVisibility(VISIBLE);
                if (mToolbarState != ToolBar.STATE_UP) {
                    mWebTitleParent.clearAnimation();
                }
                break;
            case STATE_LOADED:
                mWebTitleParent.setVisibility(VISIBLE);
                if (mToolbarState != ToolBar.STATE_UP || isNative) {
                    handleOrientation();
                }
                break;
            default:
                break;
        }
    }

    public void updateStyle() {
        if (mUiController == null || mUiController.getCurrentTab() == null) {
            return;
        }
        boolean incognito = mUiController.getCurrentTab().isPrivateBrowsingEnabled();
        boolean isNativePage = mUiController.getCurrentTab().isNativePage();
        if (isNativePage) {
            mUrlInput.setText("");
            mWebTitleParent.setVisibility(GONE);
        } else {
            mWebTitleParent.setVisibility(VISIBLE);
        }
        updateBookmarkImage();
        if (incognito && false) {
            mUrlInput.setTextColor(Color.WHITE);
//            mSearchBtn.setImageResource(R.drawable.ic_browser_toolbar_search_incognito);
//            mShareBtn.setImageResource(R.drawable.ic_browser_toolbar_share_incognito);
//            if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.LOLLIPOP) {
//                mSearchBtn.setBackgroundResource(R.drawable.browser_common_menu_item_bg_incognito);
//                mShareBtn.setBackgroundResource(R.drawable.browser_common_menu_item_bg_incognito);
//            }
        } else {
            mUrlInput.setTextColor(Color.BLACK);
//            mSearchBtn.setImageResource(R.drawable.ic_browser_toolbar_search);
//            mShareBtn.setImageResource(R.drawable.ic_browser_toolbar_share);
//            if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.LOLLIPOP) {
//                mSearchBtn.setBackgroundResource(R.drawable.browser_common_menu_item_bg);
//                mShareBtn.setBackgroundResource(R.drawable.browser_common_menu_item_bg);
//            }
        }

        mState = STATE_HOMEPAGE;
        if (isNativePage) {
            mState = STATE_HOMEPAGE;
        } else if (mUiController != null
                && mUiController.getCurrentTab() != null
                && mUiController.getCurrentTab().inPageLoad()) {
            mState = STATE_LOADING;
        } else if (mUiController != null
                && mUiController.getCurrentTab() != null
                && !mUiController.getCurrentTab().inPageLoad()) {
            mState = STATE_LOADED;
        }
        setState(mUiController.getCurrentTab(), mState);
    }

    public void setUiController(UiController controller) {
        mUiController = controller;
    }

    public void startScrollAnimation(ToolBar.DIRECTION direction, int scrollDistance) {
        if (mIsDoLoadedFinishAnimation) {
            return;
        }
        updateBookmarkImage();
        mWebTitleParent.setVisibility(VISIBLE);
        switch (direction) {
            case DOWN:
                //上滑
                AnimationSet upSet = new AnimationSet(true);
                ScaleAnimation upScaleAnimation = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.0f);

                TranslateAnimation textTranslateUpAnimation = new TranslateAnimation(0, 0,
                        getContext().getResources().getDimension(R.dimen.toolbar_title_scroll_up_start_distance),
                        getContext().getResources().getDimension(R.dimen.toolbar_title_scroll_up_end_distance));
                AlphaAnimation textAlphaUpAnimation = new AlphaAnimation(1.0f, 0.0f);
                upSet.addAnimation(upScaleAnimation);
                upSet.addAnimation(textTranslateUpAnimation);
                upSet.addAnimation(textAlphaUpAnimation);
                upSet.setDuration(ToolBar.SCROLL_ANIMATOR_TIME);
                upSet.setFillAfter(true);
                mWebTitleParent.startAnimation(upSet);

                loadedViewDownSet = new AnimationSet(true);
                mTranslateAnimation = new TranslateAnimation(0, 0, scrollDistance, 0);
                AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
                setUpAnimator(ToolBar.DIRECTION.DOWN);
                loadedViewDownSet.addAnimation(alphaAnimation);
                loadedViewDownSet.addAnimation(mTranslateAnimation);
                loadedViewDownSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mToolbarState = ToolBar.STATE_DOWN;
                        mWebTitleParent.setVisibility(VISIBLE);
                        mWebTitleParent.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                mWebTitleParent.startAnimation(loadedViewDownSet);
                break;
            case UP:
                //下移
                mWebTitleParent.setVisibility(VISIBLE);
                AnimationSet downSet = new AnimationSet(true);

                ScaleAnimation downScaleAnimation = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.0f);

                TranslateAnimation translateDownAnimation = new TranslateAnimation(0, 0,
                        -getContext().getResources().getDimension(R.dimen.toolbar_title_scroll_start_distance),
                        -getContext().getResources().getDimension(R.dimen.toolbar_title_scroll_distance));
                AlphaAnimation alphaDownAnimation = new AlphaAnimation(0.0f, 1.0f);

                downSet.addAnimation(translateDownAnimation);
                downSet.setDuration(ToolBar.SCROLL_ANIMATOR_TIME);
                downSet.setFillAfter(true);
                downSet.addAnimation(downScaleAnimation);
                downSet.addAnimation(alphaDownAnimation);
                mWebTitleParent.startAnimation(downSet);
                loadedViewDownSet = new AnimationSet(true);

                mTranslateAnimation = new TranslateAnimation(0, 0,
                        0, scrollDistance);
                AlphaAnimation alphaAnimationup = new AlphaAnimation(1.0f, 0.0f);
                setUpAnimator(ToolBar.DIRECTION.UP);
//                loadedViewDownSet.addAnimation(mTranslateAnimation);
//                loadedViewDownSet.addAnimation(alphaAnimationup);
//                loadedViewDownSet.setAnimationListener(new Animation.AnimationListener() {
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        mWebTitleParent.setVisibility(VISIBLE);
//                        mWebTitleParent.clearAnimation();
//                        mToolbarState = ToolBar.STATE_UP;
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//                    }
//                });
//                mWebTitleParent.startAnimation(loadedViewDownSet);

                break;
        }
    }

    private void setUpAnimator(final ToolBar.DIRECTION direction) {
        loadedViewDownSet.setDuration(ToolBar.SCROLL_ANIMATOR_TIME);
        loadedViewDownSet.setFillAfter(true);
        loadedViewDownSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (mUiController != null && mUiController.getCurrentTab() != null
                        && mUiController.getCurrentTab().isNativePage()) {
                    return;
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mUiController != null && mUiController.getCurrentTab() != null
                        && mUiController.getCurrentTab().isNativePage()) {
                    return;
                }
//                if (direction == ToolBar.DIRECTION.DOWN) {
//                    mWebTitleParent.setVisibility(GONE);
//                } else if (direction == ToolBar.DIRECTION.UP) {
//                    mLoadedBtnParent.setVisibility(GONE);
//                }
                mWebTitleParent.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

   public void setUrlTitle(String title) {
       if (title == null) {
           return;
       }
       if (mUrlInput != null && !title.equals(mUrlInput.getText().toString())) {
           mUrlInput.setText(title);
       }
   }

    public void setIsSearchResultPage(boolean isSearchResultPage) {
        this.mIsSearchResultPage = isSearchResultPage;
    }

    public void onPageLoadFinished(boolean isSearchResultPage, Tab tab) {
        if (mToolbarState == ToolBar.STATE_UP || (tab != null && tab.isNativePage())) {
            return;
        }
        mIsSearchResultPage = isSearchResultPage;
        AnimationSet titleViewOutSet = new AnimationSet(true);
        titleViewOutSet.setDuration(100);
        titleViewOutSet.setFillAfter(true);
        final TranslateAnimation translateOut = new TranslateAnimation(0, 0, 0, -40);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        titleViewOutSet.addAnimation(translateOut);
        titleViewOutSet.addAnimation(alphaAnimation);
        titleViewOutSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsDoLoadedFinishAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Close toobar
                mWebTitleParent.setVisibility(VISIBLE);
//                mWebTitleParent.setVisibility(GONE);
//                mLoadedBtnParent.setVisibility(VISIBLE);
                mWebTitleParent.clearAnimation();
                mIsDoLoadedFinishAnimation = false;
                updateBookmarkImage();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mWebTitleParent.startAnimation(titleViewOutSet);

        TranslateAnimation translateIn = new TranslateAnimation(0, 0, 20, 0);
        AlphaAnimation alphaIn = new AlphaAnimation(0.0f, 1.0f);

        AnimationSet centerViewInSet1 = new AnimationSet(true);
        centerViewInSet1.setDuration(CENTER_VIEW_SCROLL_TIME);
        centerViewInSet1.setFillAfter(true);
        centerViewInSet1.addAnimation(translateIn);
        centerViewInSet1.addAnimation(alphaIn);
        centerViewInSet1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        centerViewInSet1.setStartOffset(250);
//        mSearchBtn.startAnimation(centerViewInSet1);

        if (!mIsSearchResultPage || true) {
//            mAddBookmarkBtn.setVisibility(VISIBLE);
//            mShareBtn.setVisibility(VISIBLE);
//            mLandscapeView.setVisibility(VISIBLE);
//            TranslateAnimation translateIn2 = new TranslateAnimation(0, 0, 20, 0);
//            AlphaAnimation alphaIn2 = new AlphaAnimation(0.0f, 1.0f);
//            AnimationSet centerViewInSet2 = new AnimationSet(true);
//            centerViewInSet2.setDuration(CENTER_VIEW_SCROLL_TIME);
//            centerViewInSet2.setFillAfter(true);
//            centerViewInSet2.addAnimation(translateIn2);
//            centerViewInSet2.addAnimation(alphaIn2);
//            centerViewInSet2.setStartOffset(100);
//            mAddBookmarkBtn.startAnimation(centerViewInSet2);
//
//            TranslateAnimation translateIn3 = new TranslateAnimation(0, 0, 20, 0);
//            AlphaAnimation alphaIn3 = new AlphaAnimation(0.0f, 1.0f);
//            AnimationSet centerViewInSet3 = new AnimationSet(true);
//            centerViewInSet3.setDuration(CENTER_VIEW_SCROLL_TIME);
//            centerViewInSet3.setFillAfter(true);
//            centerViewInSet3.addAnimation(translateIn3);
//            centerViewInSet3.addAnimation(alphaIn3);
//            centerViewInSet3.setStartOffset(150);
//            mShareBtn.startAnimation(centerViewInSet3);
        } else {
//            mAddBookmarkBtn.clearAnimation();
//            mShareBtn.clearAnimation();
//            mAddBookmarkBtn.setVisibility(GONE);
//            mShareBtn.setVisibility(View.GONE);
            handleOrientation();
        }
    }

    public void clearChildAnimation() {
//        mSearchBtn.clearAnimation();
//        mAddBookmarkBtn.clearAnimation();
//        mShareBtn.clearAnimation();
    }

    public void setToolbarState(int toolbarState) {
        mToolbarState = toolbarState;
    }

    public String getTitle() {
        return mUrlInput.getText().toString();
    }

    private void updateBookmarkImage() {
        if (mUiController == null) {
            return;
        }
//        if (((Controller) mUiController).canAddBookmark()) {
//            mAddBookmarkBtn.setImageResource(R.drawable.ic_browser_toolbar_bookmark_added);
//        } else if (mUiController.getCurrentTab() != null && mUiController.getCurrentTab().isPrivateBrowsingEnabled() && false) {
//            mAddBookmarkBtn.setImageResource(R.drawable.ic_browser_toolbar_add_bookmark_incognito);
//        } else {
//            mAddBookmarkBtn.setImageResource(R.drawable.ic_browser_toolbar_add_bookmark);
//        }
    }

    public void onConfigurationChanged(Configuration config) {
        if (mIsSearchResultPage) {
            handleOrientation();
        }
    }

    public void handleOrientation() {
//        if (getContext().getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE) {
//            mLandscapeView.setVisibility(INVISIBLE);
//        } else {
//            mLandscapeView.setVisibility(GONE);
//        }
    }

    public void setIconSafe(boolean isShowSafeIcon) {
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
