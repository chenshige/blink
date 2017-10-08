package com.blink.browser.homepages.clone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * 可被克隆的 RelativeLayout
 */
public class CloneableRelativeLayout extends RelativeLayout implements ExtendViewCloneable {

    public CloneableRelativeLayout(Context context) {
        super(context);
    }

    public CloneableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CloneableRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void clone(Canvas canvas) {
        draw(canvas);
    }

    @Override
    public Rect getLayout() {
        Rect rect = new Rect();
        rect.left = getLeft();
        rect.right = getRight();
        rect.bottom = getBottom();
        rect.top = getTop();
        return rect;
    }
}
