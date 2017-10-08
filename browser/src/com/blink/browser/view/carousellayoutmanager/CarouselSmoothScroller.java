package com.blink.browser.view.carousellayoutmanager;

import android.content.Context;
import android.support.v7.widget.LinearSmoothScroller;
import android.view.View;

/**
 * Custom implementation of {@link android.support.v7.widget.RecyclerView.SmoothScroller} that can work only with {@link CarouselLayoutManager}.
 *
 * @see CarouselLayoutManager
 */
public abstract class CarouselSmoothScroller extends LinearSmoothScroller {
    private Context mContext;
    protected CarouselSmoothScroller(final Context context) {
        super(context);
        mContext = context;
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public int calculateDyToMakeVisible(final View view, final int snapPreference) {
        final CarouselLayoutManager layoutManager = (CarouselLayoutManager) getLayoutManager();
        if (null == layoutManager || !layoutManager.canScrollVertically()) {
            return 0;
        }

        return layoutManager.getOffsetForCurrentView(view);
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public int calculateDxToMakeVisible(final View view, final int snapPreference) {
        final CarouselLayoutManager layoutManager = (CarouselLayoutManager) getLayoutManager();
        if (null == layoutManager || !layoutManager.canScrollHorizontally()) {
            return 0;
        }
        return layoutManager.getOffsetForCurrentView(view);
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return (int) Math.ceil(Math.abs(dx) * 40.0f / mContext.getResources().getDisplayMetrics().densityDpi);
    }
}
