package com.blink.browser.view.carousellayoutmanager;

import android.support.annotation.NonNull;
import android.view.View;

import com.blink.browser.R;

/**
 * Implementation of {@link CarouselLayoutManager.PostLayoutListener} that makes interesting scaling of items. <br />
 * We are trying to make items scaling quicker for closer items for center and slower for when they are far away.<br />
 * Tis implementation uses atan function for this purpose.
 */
public class CarouselZoomPostLayoutListener implements CarouselLayoutManager.PostLayoutListener {

    @Override
    public ItemTransformation transformChild(@NonNull final View child, final float itemPositionToCenterDiff, final int orientation) {
        final float scale;
        if (itemPositionToCenterDiff < -2) {
            scale = 0.9f;
        } else {
            scale = 1 + itemPositionToCenterDiff * 0.05f;
        }
        // because scaling will make view smaller in its center, then we should move this item to the top or bottom to make it visible
        final float translateY;
        final float translateX;
        if (CarouselLayoutManager.VERTICAL == orientation) {
            final float translateYGeneral = child.getMeasuredHeight() * (1 - scale) / 2f;
            translateY = Math.signum(itemPositionToCenterDiff) * translateYGeneral - child.getContext().getResources().getDimensionPixelSize(R.dimen.navscreen_tab_views_offset);
            translateX = 0;
        } else {
            final float translateXGeneral = child.getMeasuredWidth() * (1 - scale) / 2f;
            translateX = Math.signum(itemPositionToCenterDiff) * translateXGeneral;
            translateY = 0;
        }

        return new ItemTransformation(scale, scale, -translateX, -translateY);
    }
}
