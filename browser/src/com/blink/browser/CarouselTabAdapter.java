// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.ThreadUtils;
import com.blink.browser.view.carousellayoutmanager.CarouselLayoutManager;
import com.blink.browser.view.carousellayoutmanager.CarouselRecyclerView;


public class CarouselTabAdapter extends RecyclerView.Adapter<CarouselTabAdapter.TabViewHolder> {
    private Context mContext;
    private UiController mUiController;
    private PhoneUi mUi;
    private final View mFooterView;
    private final View mHeaderView;
    private static final int GAP_SIZE = 1000;
    private static final int VIEW_TYPE_HEADER = 1000;
    private static final int VIEW_TYPE_FOOTER = 2000;
    private CarouselRecyclerView mCarouselRecyclerView;
    private NavScreen mNavScreen;

    public CarouselTabAdapter(Context context, UiController uiController, PhoneUi phoneUi, NavScreen navScreen, CarouselRecyclerView carouselRecyclerView) {
        mContext = context;
        mUiController = uiController;
        mUi = phoneUi;
        mNavScreen = navScreen;
        mCarouselRecyclerView = carouselRecyclerView;
        mFooterView = createGapView();
        mHeaderView = createGapView();
    }

    private double dpToPx(double dp) {
        Resources resources = mContext.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return  dp * ((double) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private View createGapView() {
        final View view = new View(mContext);
        final int width = mContext.getResources().getDimensionPixelSize(R.dimen.nav_tab_width);
        final int height = ViewGroup.LayoutParams.MATCH_PARENT;

        final ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, height);

        params.height = 1;

        view.setLayoutParams(params);

        return view;
    }


    private class FooterHolder extends TabViewHolder {
        public FooterHolder(View v) {
            super(v);
        }
    }

    private class HeaderHolder extends TabViewHolder {
        public HeaderHolder(View v) {
            super(v);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return VIEW_TYPE_HEADER;

        if (position == getItemCount() - 1)
            return VIEW_TYPE_FOOTER;

        return super.getItemViewType(position);
    }

    @Override
    public TabViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER)
            return new HeaderHolder(mHeaderView);

        if (viewType == VIEW_TYPE_FOOTER)
            return new FooterHolder(mFooterView);
        return new TabViewHolder(LayoutInflater.from(mContext).inflate(R.layout.tab_capture_view, parent, false));
    }

    @Override
    public void onBindViewHolder(final TabViewHolder holder, final int position) {
        if (position != 0 && position != getItemCount() - 1) {
            int fixedPostion = position - 1;
            final Tab tab = mUiController.getTabControl().getTab(fixedPostion);
            if (tab != null) {
                if (tab.isPrivateBrowsingEnabled()) {
                    holder.mCloseView.setImageResource(R.drawable.ic_browser_incognito_clear);
                    holder.mCloseView.setBackgroundColor(ContextCompat.getColor(mContext, R.color
                            .nav_tab_title_incognito_bg));
                    holder.mTitle.setBackgroundColor(ContextCompat.getColor(mContext, R.color.nav_tab_title_incognito_bg));
                    holder.mTitle.setTextColor(ContextCompat.getColor(mContext, R.color.white));
                } else {
                    holder.mCloseView.setImageResource(R.drawable.ic_browser_close);
                    holder.mCloseView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.nav_tab_title_normal_bg));
                    holder.mTitle.setBackgroundColor(ContextCompat.getColor(mContext, R.color.nav_tab_title_normal_bg));
                    holder.mTitle.setTextColor(ContextCompat.getColor(mContext, R.color.normal_text_color));
                }
                holder.mTitle.setText(tab.getTitle());
                holder.mCloseView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mNavScreen.setBlockEvents(true);
                        mUiController.setBlockEvents(true);
                        setCloseTabAnimation(holder, tab);
                        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS, AnalyticsSettings.ID_CLOSE);
                    }
                });
                Bitmap capture = tab.getScreenshot();
                if (capture == null && !tab.getCaptureSuccess()) {
                    capture = mUiController.getTabControl().getHomeCapture();
                }
                holder.mTabView.setImageBitmap(capture);
                holder.mTabView.setScaleType(ImageView.ScaleType.MATRIX);
                if (capture != null) {
                    float sfx = mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_width) / (float) (capture.getWidth());
                    Matrix m = new Matrix();
                    m.setScale(sfx, sfx);
                    holder.mTabView.setImageMatrix(m);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tryOpenHomeTab(tab)) return;
                        closeTabView(tab);
                    }
                });
            }
        }
    }

    private void setCloseTabAnimation(final TabViewHolder holder, final Tab tab)
    {
        holder.itemView.clearAnimation();
        Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.slide_out_right);
        if (mCarouselRecyclerView.getLayoutManager() instanceof CarouselLayoutManager) {
            if (((CarouselLayoutManager) mCarouselRecyclerView.getLayoutManager()).getOrientation() == CarouselLayoutManager.HORIZONTAL) {
                animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.slide_out_top);
            }
        }
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ThreadUtils.runOnUiThreadBlocking(new Runnable() {
                    @Override
                    public void run() {
                        onCloseTab(tab);
                        holder.itemView.setAlpha(0);
                        notifyItemRemoved(holder.getAdapterPosition());
                        mNavScreen.setBlockEvents(false);
                        mUiController.setBlockEvents(false);
                        if (!mUiController.getTabControl().isIncognitoShowing() && !mUiController.getTabControl().hasAnyOpenNormalTabs()) {
                            mUi.createNewTabWithNavScreen(false);
                        }
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        holder.itemView.startAnimation(animation);
    }

    @Override
    public int getItemCount() {
        return mUiController.getTabControl().getTabCount() + 2;
    }

    private boolean tryOpenHomeTab(Tab tab) {
        if (tab != null) {
            if (tab.isNativePage()) {
                mUiController.getTabControl().setIncognitoShowing(tab.isPrivateBrowsingEnabled());
                mUiController.loadNativePage(tab);
                return true;
            } else {
                tab.setNativePage(false);
            }
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS,AnalyticsSettings.ID_WINDOWSCLICK);
        }
        return false;
    }

    protected void closeTabView(Tab tab) {
        closeTabView(tab, true);
    }

    protected void closeTabView(Tab tab, boolean animate) {
        mUi.panelSwitch(UI.ComboHomeViews.VIEW_WEBVIEW, mUiController.getTabControl().getTabPosition(tab), animate);
    }

    public void onCloseTab(Tab tab) {
        if (tab != null) {
            mUiController.closeTab(tab);
        }
    }

    class TabViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitle;
        public ImageView mCloseView;
        public ImageView mTabView;
        public View mItemView;

        public TabViewHolder(View itemView) {
            super(itemView);
            mItemView = itemView;
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mCloseView = (ImageView) itemView.findViewById(R.id.close);
            mTabView = (ImageView) itemView.findViewById(R.id.tab_view);
        }
    }
}
