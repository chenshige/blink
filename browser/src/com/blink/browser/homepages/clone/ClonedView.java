package com.blink.browser.homepages.clone;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * 被克隆的View
 */
public class ClonedView extends View {

    private ViewCloneable mViewCloneable;

    public static ClonedView create(Context context, ViewCloneable viewCloneable) {
        ClonedView clonedView = new ClonedView(context);
        clonedView.setViewCloneable(viewCloneable);
        return clonedView;
    }


    public ClonedView(Context context) {
        super(context);
    }

    public ClonedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClonedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setViewCloneable(ViewCloneable viewCloneable) {
        this.mViewCloneable = viewCloneable;
    }

    public void refresh() {
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mViewCloneable != null) mViewCloneable.clone(canvas);
    }
}
