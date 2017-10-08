package com.blink.browser.homepages;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.blink.browser.BrowserSettings;
import com.blink.browser.MainPageController;
import com.blink.browser.R;
import com.blink.browser.search.SearchEnginePreference;
import com.blink.browser.view.SearchCardView;
import com.blink.browser.widget.AnimationListener;

public class HomePageHeadView extends RelativeLayout implements ScreenRotateable {

    private static final int LOGOTYPE_HAVE = 0;//有搜索引擎的logo
    private static final int LOGOTYPE_TO_DEFAULT = 1;//替换成默认logo
    public static final int LOGOTYPE_DEFAULT = 2;//已经是默认logo

    public FrameLayout mSearchEngineLayout;
    private SearchCardView mSearchCardView;
    private MainPageController mMainPageController;
    private ImageView mSearchEngineLogo;
    private Context mContext;
    private boolean mIncognito;
    private int mLogoType = LOGOTYPE_HAVE;

    public HomePageHeadView(Context context, MainPageController mainPageController, boolean isPortrait, boolean incognito) {
        super(context);
        mContext = context;
        mMainPageController = mainPageController;
        mIncognito = incognito;
        initView(isPortrait, incognito);
    }

    public FrameLayout.LayoutParams createLayoutParams() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void initView(boolean isPortrait, boolean incognito) {
        if (mSearchEngineLayout != null) {
            mSearchEngineLayout.removeAllViews();
        }
        removeAllViews();
        View rootView;
        if (isPortrait) {
            rootView = View.inflate(getContext(), R.layout.head_title_view_portrait, this);
        } else {
            rootView = View.inflate(getContext(), R.layout.head_title_view_landscape, this);
        }
        mSearchEngineLayout = (FrameLayout) rootView.findViewById(R.id.search_engine_layout);
        mSearchEngineLogo = (ImageView) rootView.findViewById(R.id.search_engine_logo);
        mSearchCardView = new SearchCardView(mContext, mMainPageController);
        mSearchEngineLayout.addView(mSearchCardView);
        updateSearchEngineLogo(false);
        mSearchCardView.onIncognito(incognito);
    }


    @Override
    public void onScreenRotate(boolean isPortrait) {
        initView(isPortrait, mIncognito);
    }

    public ImageView getBrowserLogo() {
        return mSearchEngineLogo;
    }

    public FrameLayout getSearchBar() {
        return mSearchEngineLayout;
    }

    public void setState(float state) {
        mSearchCardView.setState(state);
    }

    public void onResume() {
        mSearchCardView.onResume();
    }

    public void onPause() {
    }

    public void setTouch(boolean isCanClick) {
        mSearchCardView.setIsCanClick(isCanClick);
    }

    public void updateSearchEngineLogo(boolean isRunAnima) {
        if (mSearchEngineLogo == null || mContext == null) {
            return;
        }

        final String searchEngineName = BrowserSettings.getInstance().getSearchEngineName();

        if (!SearchEnginePreference.isHaveSearchEngineLogo(mContext, searchEngineName)) {
            if (mLogoType == LOGOTYPE_DEFAULT) {
                return;
            } else { //这里不会出现logoType = LOGOTYPE_TO_DEFAULT情况，所以不用判断
                mLogoType = LOGOTYPE_TO_DEFAULT;
            }
        } else if (mLogoType == LOGOTYPE_DEFAULT) {
            mLogoType = LOGOTYPE_HAVE;
        }

        if (!isRunAnima) {
            SearchEnginePreference.setSearchEngineLogo(mContext, mSearchEngineLogo, searchEngineName, mLogoType);
            if (mLogoType == LOGOTYPE_TO_DEFAULT) {
                mLogoType = LOGOTYPE_DEFAULT;
            }
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator oldScaleX = ObjectAnimator.ofFloat(mSearchEngineLogo, "scaleX", 1, 0);
        ObjectAnimator newScaleX = ObjectAnimator.ofFloat(mSearchEngineLogo, "scaleX", 0, 1);
        oldScaleX.setInterpolator(new DecelerateInterpolator());
        oldScaleX.setDuration(100);
        newScaleX.setInterpolator(new DecelerateInterpolator());
        newScaleX.setDuration(100);
        animatorSet.play(newScaleX).after(oldScaleX);
        oldScaleX.addListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                SearchEnginePreference.setSearchEngineLogo(mContext, mSearchEngineLogo, searchEngineName, mLogoType);
                if (mLogoType == LOGOTYPE_TO_DEFAULT) {
                    mLogoType = LOGOTYPE_DEFAULT;
                }
            }
        });
        animatorSet.start();
    }

    public void onIncognito(boolean incognito) {
        mIncognito = incognito;
        mSearchCardView.onIncognito(incognito);
    }
}
