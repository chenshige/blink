package com.blink.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class MobileSignalView extends View {
    private static final int ZERO = 0;
    private static final int SCALE = 14;
    private int mValue;
    private int mMax = 100;
    private Paint mPaint;


    public MobileSignalView(Context context) {
        super(context);
    }

    public MobileSignalView(Context context, AttributeSet attrs) {
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
        drawTriangle(canvas);
    }

    private void drawTriangle(Canvas canvas) {
        Path path = new Path();
        path.moveTo(ZERO, getHeight());
        path.lineTo(getWidth() * mValue / mMax, getHeight());
        path.lineTo(getWidth() * mValue / mMax, getHeight() - getHeight() * mValue / mMax);
        path.close();
        canvas.drawPath(path, mPaint);
    }
}
