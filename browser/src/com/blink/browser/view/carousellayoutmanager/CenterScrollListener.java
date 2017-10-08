package com.blink.browser.view.carousellayoutmanager;

import android.support.v7.widget.RecyclerView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

/**
 * Class for centering items after scroll event.<br />
 * This class will listen to current scroll state and if item is not centered after scroll it will automatically scroll it to center.
 */
public class CenterScrollListener extends RecyclerView.OnScrollListener {

    @Override
    public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof CarouselLayoutManager)) {
            return;
        }

        final CarouselLayoutManager lm = (CarouselLayoutManager) layoutManager;
        lm.setScrollState(newState);
        if (RecyclerView.SCROLL_STATE_IDLE == newState) {
            if (lm.getItemCount() > 2 && (lm.getCenterItemPosition() == 0
                    || (lm.getOffsetForCurrentView(lm.findViewByPosition(lm.getCenterItemPosition())) < 0 && lm.getCenterItemPosition() == 1))) {
                recyclerView.smoothScrollToPosition(1);
            } else if (lm.getItemCount() > 2 && (lm.getCenterItemPosition() == lm.getItemCount() - 1
                    || (lm.getOffsetForCurrentView(lm.findViewByPosition(lm.getCenterItemPosition())) > 0 && lm.getCenterItemPosition() == lm.getItemCount() - 2))) {
                recyclerView.smoothScrollToPosition(lm.getItemCount() - 2);
            }
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.WINDOWS_EVENTS, AnalyticsSettings.ID_SLIDESWITCH);
        }
    }
}
