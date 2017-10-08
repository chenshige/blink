package com.blink.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class WifiView  extends View {
    private static final int ZERO = 0;
    private static final int START_ANGLE = 230; // This is the calculated result
    private static final int SWEEP_ANGLE = 80; // This value is what i measure from the png Gui gave
    private static final int DIVISOR_HALF = 2;
    private int mValue;
    private int mMax = 100;
    private Paint mPaint;

    public WifiView(Context context) {
        super(context);
    }

    public WifiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
    }


    public void setValue(int value) {
        mValue = value;
        if (mValue < ZERO) {
            mValue = ZERO;
        } else if (mValue > mMax) {
            mValue = mMax;
        }
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Specify the rect surround the circle which contains this arc
        int left = getWidth() / DIVISOR_HALF - mValue * getHeight() / mMax;
        int top  = getHeight() - mValue * getHeight() / mMax;
        int right = getWidth() / DIVISOR_HALF + mValue * getHeight() / mMax;
        int bottom = getHeight() + mValue * getHeight() / mMax;

        RectF oval = new RectF(left, top, right, bottom);
        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE, true, mPaint);
    }

}
