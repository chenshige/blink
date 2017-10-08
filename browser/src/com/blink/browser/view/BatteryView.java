package com.blink.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BatteryView extends View {
    private static final int ZERO = 0;
    private int mValue;
    private int mMax = 100;
    private int mBodyPart = 95;
    private int mWidthScale = 9;
    private int mHeightScale = 16;

    private Paint mPaint;

    public BatteryView(Context context) {
        super(context);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawRect(canvas);
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


    public void setMax(int max) {
        mMax = max;
    }

    private void drawRect(Canvas canvas) {
        if (mValue == mMax) {
            RectF rectHead = new RectF(2 * getWidth() / mWidthScale, ZERO, getWidth() - 2 * getWidth() / mWidthScale , 2 * getHeight() / mHeightScale);
            canvas.drawRect(rectHead, mPaint);
        }

        int body = mValue;
        if (body > mBodyPart) {
            body = mBodyPart;
        }

        int height = body * (getHeight() - 2 * getHeight() / mHeightScale) / mBodyPart;
        RectF rect = new RectF(ZERO, getHeight() - height, getWidth(), getHeight());
        canvas.drawRect(rect, mPaint);
    }
}

