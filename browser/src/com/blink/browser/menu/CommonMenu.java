package com.blink.browser.menu;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.Controller;
import com.blink.browser.R;
import com.blink.browser.Tab;
import com.blink.browser.UiController;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

public class CommonMenu extends RelativeLayout implements View.OnClickListener {
    public static final int SHOW_SHADOW_ANIM_DURATION = 150;
    public static final int HIDE_SHADOW_ANIM_DURATION = 100;
    public static final int SHOW_VIEW_ANIM_FIRST_TIME = 100;
    public static final int SHOW_VIEW_ANIM_SECOND_TIME = 50;
    public static final int DISMISS_VIEW_ANIM_SECOND_TIME = 100;
    public static final int TOOLBOX_OPEN_ANIMATION_TIME = 200;

    public static final String SPLIT = " : ";

    private MenuToolBox mMenuToolBoxView;
    private LinearLayout mAnimatorMenuView;  //动画部分
    private View mShadowView; //阴影部分
    private View mDivider;
    private TextView mToastView;

    private UiController mUiController;
    private LinearLayout mSettingLayout, mShareLayout, mDownloadLayout, mNewBookmarkLayout,
            mToolsBoxLayout, mBookmarkHistoryLayout, mQuitLayout, mRefreshLayout, mLastLineParent; // ,

    private ImageView mSettingImage, mShareImage, mNewBookmarkImage, mDownloadImage,
            mToolsBoxImage, mBookmarkHistoryImage, mQuitImage, mRefreshImage; //,
    private TextView mNewBookmarkText, mDownloadText, mBookmarkHistoryText, mRefreshText, mShareText;
    //, mQuitText, mToolsBoxText,mSettingText,

    private CommonMenuCircleImageView mIncognitoImage, mNoImageView, mShowStatusBarImage, mEyeProtectedImage;
    private RelativeLayout mFirstMenuLayout;

    private boolean mIsDoDismissAnimation;
    private boolean mIsDoBoxInAnimation, mIsDoBoxOutAnimation;

    public CommonMenu(Context context) {
        super(context);
    }

    public CommonMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViewInWin();
    }

    private void initViewInWin() {
        mAnimatorMenuView = (LinearLayout) findViewById(R.id.common_menu_parent);
        mShadowView = findViewById(R.id.common_menu_shadow);
        mDivider = findViewById(R.id.divider);

        mFirstMenuLayout = (RelativeLayout) findViewById(R.id.first_menu_parent);
        mSettingLayout = (LinearLayout) findViewById(R.id.setting_button_id);
        mShareLayout = (LinearLayout) findViewById(R.id.share_button_id);
        mNewBookmarkLayout = (LinearLayout) findViewById(R.id.new_bookmark_button_id);
        mDownloadLayout = (LinearLayout) findViewById(R.id.download_button_id);
        mToolsBoxLayout = (LinearLayout) findViewById(R.id.tools_box_button_id);
        mBookmarkHistoryLayout = (LinearLayout) findViewById(R.id.bookmarks_history_button_id);
        mQuitLayout = (LinearLayout) findViewById(R.id.exit_button_id);
        mRefreshLayout = (LinearLayout) findViewById(R.id.refresh_button_id);
        mRefreshText = (TextView) findViewById(R.id.refresh_common_menu);
        mSettingImage = (ImageView) findViewById(R.id.setting_common_menu_id);
        mShareImage = (ImageView) findViewById(R.id.share_common_menu_id);
        mShareText = (TextView)findViewById(R.id.share_common_menu);
        mBookmarkHistoryImage = (ImageView) findViewById(R.id.bookmarks_history_common_menu_id);
        mBookmarkHistoryText = (TextView) findViewById(R.id.bookmarks_history_common_menu);
        mBookmarkHistoryText.setText(getResources().getString(R.string.menu_browser_bookmarks) +
        getResources().getString(R.string.menu_browser_history));
        mIncognitoImage = (CommonMenuCircleImageView) findViewById(R.id.incognito_common_menu_id);
        mNoImageView = (CommonMenuCircleImageView) findViewById(R.id.noImage_common_menu_id);
        mNewBookmarkImage = (ImageView) findViewById(R.id.new_bookmark_common_menu_id);
        mNewBookmarkText = (TextView) findViewById(R.id.new_bookmark_common_menu);
        mDownloadImage = (ImageView) findViewById(R.id.download_common_menu_id);
        mShowStatusBarImage = (CommonMenuCircleImageView) findViewById(R.id.status_bar_toolbar_id);
        mEyeProtectedImage = (CommonMenuCircleImageView) findViewById(R.id.eye_protect_toolbar_id);
        mToolsBoxImage = (ImageView) findViewById(R.id.toolbox_common_menu_id);
        mRefreshImage = (ImageView) findViewById(R.id.refresh_toolbar_id);
        mDownloadText = (TextView) findViewById(R.id.download_common_menu);
        mToastView = (TextView) findViewById(R.id.toast_text_view);
        mQuitImage = (ImageView) findViewById(R.id.exit_common_menu_id);
        mLastLineParent = (LinearLayout) findViewById(R.id.tools_menu_column3);
        mIncognitoImage.setOnClickListener(this);
        mSettingLayout.setOnClickListener(this);
        mShareLayout.setOnClickListener(this);
        mNewBookmarkLayout.setOnClickListener(this);
        mDownloadLayout.setOnClickListener(this);
        mNoImageView.setOnClickListener(this);
        mRefreshLayout.setOnClickListener(this);
        mEyeProtectedImage.setOnClickListener(this);
        mShadowView.setOnClickListener(this);
        mToolsBoxLayout.setOnClickListener(this);
        mBookmarkHistoryLayout.setOnClickListener(this);
        mAnimatorMenuView.setOnClickListener(this);
        mFirstMenuLayout.setOnClickListener(this);
        mShowStatusBarImage.setOnClickListener(this);
        mQuitLayout.setOnClickListener(this);
        mLastLineParent.setOnClickListener(null);
    }

    public void setUiController(UiController controller) {
        if (controller == null) {
            return;
        }
        this.mUiController = controller;
    }

    public void updateMenuItemState(int id, boolean show) {
        switch (id) {
            case R.id.incognito_common_menu_id:
                if (show) {
                    mIncognitoImage.setImageResource(R.drawable.ic_browser_menu_incognito_on);
                }
                mIncognitoImage.setIsSelected(show);
                break;
            case R.id.noImage_common_menu_id:
                mNoImageView.setIsSelected(show);
                break;
            case R.id.eye_protect_toolbar_id:
                mEyeProtectedImage.setIsSelected(show);
                break;
            case R.id.status_bar_toolbar_id:
                mShowStatusBarImage.setIsSelected(show);
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tools_box_menu_parent || view.getId() == R.id.first_menu_parent) {
            return;
        }
        if (view.getId() == R.id.tools_box_button_id) {
            if (mMenuToolBoxView == null) {
                mMenuToolBoxView = (MenuToolBox) findViewById(R.id.menu_toolbox); //.findViewById(R.id.tools_box_menu_parent)
            }

            if (mFirstMenuLayout.getVisibility() == VISIBLE) {
                showMenuToolboxAnimation();
                mToolsBoxImage.setImageResource(R.drawable.ic_browser_menu_tools_open);
                try {
                    mMenuToolBoxView.updateToolBoxMenuState(mUiController.getCurrentTab(), ((Controller) mUiController)
                            .canSavedOffLine(), ((Controller) mUiController).canAddBookmark());
                } catch (Exception e) {
                }

            } else {
                hideMenuToolboxAnimation();
                mToolsBoxImage.setImageResource(R.drawable.ic_browser_menu_tools);
            }
            mMenuToolBoxView.setOnItemClickListener(this);
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.MENU_EVENTS, AnalyticsSettings
                    .ID_TOOLS);
            return;
        }
        if (mUiController != null) {
            mUiController.menuPopuOnItemClick(view);
        }
    }

    public void dismiss() {
        if (mAnimatorMenuView.getVisibility() == VISIBLE) {
            dismiss(mAnimatorMenuView, true);
        } else if (mMenuToolBoxView != null && mMenuToolBoxView.getVisibility() == VISIBLE) {
            dismiss(mMenuToolBoxView, true);
        }
    }

    private void dismiss(final View animationView, boolean isShadowViewDoAnimation) {
        if (mIsDoDismissAnimation) {
            return;
        }
        AnimationSet dismissAnimationSet = new AnimationSet(true);

        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 0.6f, 1.0f, 0.6f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        scaleAnimation.setDuration(DISMISS_VIEW_ANIM_SECOND_TIME);

        dismissAnimationSet.addAnimation(scaleAnimation);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setDuration(DISMISS_VIEW_ANIM_SECOND_TIME);
        dismissAnimationSet.addAnimation(alphaAnimation);
        dismissAnimationSet.setFillAfter(true);

        dismissAnimationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsDoDismissAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mAnimatorMenuView.setVisibility(View.GONE);
                animationView.setVisibility(GONE);
                animationView.clearAnimation();
                mIsDoDismissAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animationView.startAnimation(dismissAnimationSet);

        if (isShadowViewDoAnimation) {
            hideShadowViewAnimation(HIDE_SHADOW_ANIM_DURATION);
        }
    }

    private void hideShadowViewAnimation(int animationTime) {
        AlphaAnimation shadowAnimation = new AlphaAnimation(1.0f, 0.0f);
        shadowAnimation.setDuration(animationTime);
        shadowAnimation.setFillAfter(true);
        mShadowView.setAnimation(shadowAnimation);
        mShadowView.startAnimation(shadowAnimation);
        shadowAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
                mShadowView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public void show() {
        show(mAnimatorMenuView, true);
    }

    private void show(View animationView, boolean isShadowViewDoAnimation) {
        mIsDoDismissAnimation = false;
        setVisibility(View.VISIBLE);
        if (mFirstMenuLayout != null) {
            mFirstMenuLayout.setVisibility(VISIBLE);
            mFirstMenuLayout.clearAnimation();
        }
        if (mMenuToolBoxView != null) {
            mMenuToolBoxView.setVisibility(GONE);
            mMenuToolBoxView.clearAnimation();
        }
        animationView.setVisibility(View.VISIBLE);
        mShadowView.setVisibility(VISIBLE);
        if (mMenuToolBoxView != null && animationView != mMenuToolBoxView) {
            mMenuToolBoxView.clearAnimation();
            mMenuToolBoxView.setVisibility(GONE);
        }
        AnimationSet showAnimationSet = new AnimationSet(true);
        ScaleAnimation scaleAnimation_first = new ScaleAnimation(0.6f, 1.01f, 0.6f, 1.01f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        scaleAnimation_first.setDuration(SHOW_VIEW_ANIM_FIRST_TIME);
        showAnimationSet.addAnimation(scaleAnimation_first);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(SHOW_VIEW_ANIM_FIRST_TIME);
        showAnimationSet.addAnimation(alphaAnimation);

        ScaleAnimation scaleAnimation_second = new ScaleAnimation(1f, 0.99f, 1f, 0.99f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        scaleAnimation_second.setDuration(SHOW_VIEW_ANIM_SECOND_TIME);
        scaleAnimation_second.setStartOffset(SHOW_VIEW_ANIM_FIRST_TIME);
        showAnimationSet.addAnimation(scaleAnimation_second);
        showAnimationSet.setFillAfter(true);
        animationView.startAnimation(showAnimationSet);

        if (isShadowViewDoAnimation) {
            showShadowViewAnimation();
        }
    }

    private void showShadowViewAnimation() {
        AlphaAnimation shadowAnimation = new AlphaAnimation(0.0f, 1.0f);
        shadowAnimation.setDuration(SHOW_SHADOW_ANIM_DURATION);
        mShadowView.setAnimation(shadowAnimation);
        mShadowView.startAnimation(shadowAnimation);
    }

    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    public void updateCommonMenuState(Tab currentTab) {
        if (currentTab == null) {
            return;
        }
        int disableColor = getContext().getResources().getColor(R.color.bottom_menu_disabled_text_color);
        int enableColor = getContext().getResources().getColor(R.color.bottom_menu_text_color);
        int clickColor = R.drawable.browser_common_menu_item_bg;
        boolean isHomePage = currentTab.isNativePage();

//        boolean isSavedOffLine = false;
//        if (currentTab.getWebView() != null && currentTab.getWebView().getUrl() != null) {
//            String url = currentTab.getWebView().getUrl();
//            if (url.startsWith("file://")) {
//                isSavedOffLine = true;
//            }
//        }

        mRefreshImage.setEnabled(!isHomePage);
        mRefreshLayout.setEnabled(!isHomePage);
        mShareImage.setEnabled(!isHomePage);
        mShareLayout.setEnabled(!isHomePage);
        mNewBookmarkLayout.setEnabled(!isHomePage);
        if (isHomePage) {
            mRefreshText.setTextColor(disableColor);
            mShareText.setTextColor(disableColor);
            mNewBookmarkText.setTextColor(disableColor);
        } else {
            mRefreshText.setTextColor(enableColor);
            mShareText.setTextColor(enableColor);
            mNewBookmarkText.setTextColor(enableColor);
        }

        if (((Controller) mUiController).canAddBookmark()) {
            mNewBookmarkImage.setImageResource(R.drawable.ic_browser_menu_bookmark_highlight);
        } else if (isHomePage) {
            mNewBookmarkImage.setImageResource(R.drawable.ic_browser_menu_add_bookmark_disable);
        } else {
            mNewBookmarkImage.setImageResource(R.drawable.ic_browser_menu_add_bookmark);
        }

//        if (!currentTab.isNativePage() && ((Controller) mUiController).canAddBookmark() && !isSavedOffLine) {
//            //已经收藏的网页
//            mBookmarkHistoryImage.setImageResource(R.drawable.ic_browser_menu_bookmark_highlight);
//        } else {
//            mBookmarkHistoryImage.setImageResource(R.drawable.browser_bookmark_bg);
//        }


        if (currentTab.isPrivateBrowsingEnabled()) {
            mIncognitoImage.setImageResource(R.drawable.ic_browser_menu_incognito_on);
            mIncognitoImage.setIsSelected(true);
        } else {
            mIncognitoImage.setImageResource(R.drawable.ic_browser_menu_incognito_off);
            mIncognitoImage.setIsSelected(false);
        }

        if (BrowserSettings.getInstance().getNightMode()) {
            mEyeProtectedImage.setImageResource(R.drawable.ic_browser_menu_eyes_protector_on);
            mEyeProtectedImage.setIsSelected(true);
        } else {
            mEyeProtectedImage.setImageResource(R.drawable.ic_browser_menu_eyes_protector_off);
            mEyeProtectedImage.setIsSelected(false);
        }

        if (BrowserSettings.getInstance().loadImages()) {
            mNoImageView.setIsSelected(false);
            mNoImageView.setImageResource(R.drawable.ic_browser_menu_no_image_off);
        } else {
            mNoImageView.setImageResource(R.drawable.ic_browser_menu_noimage_on);
            mNoImageView.setIsSelected(true);
        }

        if (BrowserSettings.getInstance().showStatusBar()) {
            mShowStatusBarImage.setImageResource(R.drawable.ic_browser_menu_show_status_bar);
            mShowStatusBarImage.setIsSelected(false);
        } else {
            mShowStatusBarImage.setImageResource(R.drawable.ic_browser_menu_hide_status_bar);
            mShowStatusBarImage.setIsSelected(true);
        }

//        mNewBookmarkImage.setImageResource(R.drawable.ic_browser_menu_history);
//        mDownloadImage.setImageResource(R.drawable.ic_browser_menu_download);
//        mToolsBoxImage.setImageResource(R.drawable.ic_browser_menu_tools);
//        mSettingImage.setImageResource(R.drawable.ic_browser_menu_setting);
//        mShareImage.setImageResource(R.drawable.ic_browser_menu_share);

        mRefreshImage.setImageResource(R.drawable.browser_refresh_bg);

        mDownloadText.setTextColor(enableColor);
        mBookmarkHistoryText.setTextColor(enableColor);
        mDivider.setBackgroundColor(getResources().getColor(R.color.context_menu_divider_bg));
        mSettingLayout.setBackgroundResource(clickColor);
        mShareLayout.setBackgroundResource(clickColor);
        mDownloadLayout.setBackgroundResource(clickColor);
        mNewBookmarkLayout.setBackgroundResource(clickColor);
        mToolsBoxLayout.setBackgroundResource(clickColor);
        mBookmarkHistoryLayout.setBackgroundResource(clickColor);
    }

    public void showTextViewToast(int id, boolean isOn) {
        String toast = null;
        String onOrOff = null;
        Resources resources = getResources();
        switch (id) {
            case R.id.incognito_common_menu_id:
                toast = resources.getString(R.string.menu_browser_incognito);
                break;
            case R.id.noImage_common_menu_id:
                toast = resources.getString(R.string.menu_browser_no_image);
                break;
            case R.id.status_bar_toolbar_id:
                toast = resources.getString(R.string.menu_browser_full_screen);
                break;
            case R.id.eye_protect_toolbar_id:
                toast = resources.getString(R.string.menu_browser_night);
                break;
            default:
                break;
        }

        if (isOn) {
            onOrOff = getResources().getString(R.string.toolbox_menu_on);
        } else {
            onOrOff = getResources().getString(R.string.toolbox_menu_off);
        }
        if (TextUtils.isEmpty(toast) || TextUtils.isEmpty(onOrOff)) {
            return;
        }

        mToastView.setText(toast + SPLIT + onOrOff);
        AnimationSet animationSet = new AnimationSet(true);

        AlphaAnimation toastShow = new AlphaAnimation(0.0f, 1.0f);
        toastShow.setDuration(300);
        toastShow.setFillAfter(true);
        animationSet.addAnimation(toastShow);

        AlphaAnimation toastDismiss = new AlphaAnimation(1.0f, 0.0f);
        toastDismiss.setDuration(300);
        toastDismiss.setFillAfter(true);
        toastDismiss.setStartOffset(600);
        animationSet.addAnimation(toastDismiss);

        mToastView.startAnimation(animationSet);
        if (mUiController != null) {
            updateCommonMenuState(mUiController.getCurrentTab());
        }
    }

    private void showMenuToolboxAnimation() {
        if (mMenuToolBoxView == null || mIsDoBoxInAnimation || mIsDoBoxOutAnimation) {
            return;
        }
        mFirstMenuLayout.clearAnimation();
        mMenuToolBoxView.clearAnimation();
        mMenuToolBoxView.setVisibility(VISIBLE);
        mFirstMenuLayout.setVisibility(VISIBLE);
        TranslateAnimation translateAnimationMenuOut = new TranslateAnimation(0, 0,
                getContext().getResources().getDimension(R.dimen.toolbar_toolbox_animation_translate_distance), 0);
        translateAnimationMenuOut.setDuration(TOOLBOX_OPEN_ANIMATION_TIME);
        mMenuToolBoxView.startAnimation(translateAnimationMenuOut);

        TranslateAnimation translateAnimationToolboxIn = new TranslateAnimation(0, 0, 0,
                -getContext().getResources().getDimension(R.dimen.toolbar_toolbox_animation_translate_distance));
        translateAnimationToolboxIn.setDuration(TOOLBOX_OPEN_ANIMATION_TIME);
        mFirstMenuLayout.startAnimation(translateAnimationToolboxIn);

        translateAnimationToolboxIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsDoBoxInAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFirstMenuLayout.setVisibility(INVISIBLE);
                mIsDoBoxInAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void hideMenuToolboxAnimation() {
        if (mMenuToolBoxView == null || mIsDoBoxOutAnimation || mIsDoBoxInAnimation) {
            return;
        }
        mFirstMenuLayout.clearAnimation();
        mMenuToolBoxView.clearAnimation();
        mMenuToolBoxView.setVisibility(VISIBLE);
        mFirstMenuLayout.setVisibility(VISIBLE);
        TranslateAnimation translateAnimationMenuIn = new TranslateAnimation(0, 0,
                0, getContext().getResources().getDimension(R.dimen.toolbar_toolbox_animation_translate_distance));
        translateAnimationMenuIn.setDuration(TOOLBOX_OPEN_ANIMATION_TIME);
        mMenuToolBoxView.startAnimation(translateAnimationMenuIn);

        TranslateAnimation translateAnimationToolboxOut = new TranslateAnimation(0, 0,
                -getContext().getResources().getDimension(R.dimen.toolbar_toolbox_animation_translate_distance), 0);
        translateAnimationToolboxOut.setDuration(TOOLBOX_OPEN_ANIMATION_TIME);
        mFirstMenuLayout.startAnimation(translateAnimationToolboxOut);
        translateAnimationToolboxOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsDoBoxOutAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsDoBoxOutAnimation = false;
                mMenuToolBoxView.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public void onConfigurationChanged(Configuration config) {
        if (mAnimatorMenuView == null) {
            return;
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAnimatorMenuView.getLayoutParams();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏
            params.leftMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.toolbar_menu_margin_Landscape);
            params.rightMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.toolbar_menu_margin_Landscape);
        } else {
            params.leftMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.toolbar_commonmenu_padding);
            params.rightMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.toolbar_commonmenu_padding);
        }
        mAnimatorMenuView.setLayoutParams(params);
    }
}
