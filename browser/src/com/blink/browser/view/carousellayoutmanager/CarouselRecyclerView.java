package com.blink.browser.view.carousellayoutmanager;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

import com.blink.browser.R;

/**
 * Created by ronny on 16-10-18.
 */
public class CarouselRecyclerView extends RecyclerView {
    private int FLING_SCALE_DOWN_DISTANCE = 10000;

    public CarouselRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return super.fling(velocityX, velocityY);
    }

}
