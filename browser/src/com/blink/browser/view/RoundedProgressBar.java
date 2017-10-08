package com.blink.browser.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.blink.browser.R;

/**
 * This is a rounded progress bar
 */
public class RoundedProgressBar extends View {
    private static final int STROKE = 0;
    private static final int STROKE_TEXT = 1; // reserve to draw text, if need
    private static final int STROKE_FILL = 2;
    private static final int CIRCLE_DEGREE = 360;
    private static final float HALF_FACTOR = 0.5f;
    private static final float DIVIDE_BY = 100.0f;

    private int mStartPos = -90;
    private Paint mPaint;
    private int mBackColor;
    private int mFontColor;
    private float mBorderWidth;
    private int mMode;

    private float mHalfBorder;
    private int mMax;
    private int mValue;
    private Paint.Style mStyle;
    private boolean mIsFill;

    public RoundedProgressBar(Context context) {
        super(context, null);
    }

    public RoundedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundedProgressBar);

        mBackColor = mTypedArray.getColor(R.styleable.RoundedProgressBar_backColor, Color.GRAY);
        mFontColor = mTypedArray.getColor(R.styleable.RoundedProgressBar_frontColor, Color.WHITE);
        mBorderWidth = mTypedArray.getDimension(R.styleable.RoundedProgressBar_borderWidth, getResources().getDimensionPixelSize(R.dimen.rounded_progress_border_width));
        mHalfBorder = mBorderWidth * HALF_FACTOR;
        mMode = mTypedArray.getInteger(R.styleable.RoundedProgressBar_mode, STROKE);
        mIsFill = mMode == STROKE_FILL;
        mStyle = Paint.Style.STROKE;
        mTypedArray.recycle();
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getValue() {
        return mValue;
    }

    public synchronized void setValue(int newValue) {
        newValue = Math.max(0, newValue);
        newValue = Math.min(mMax, newValue);
        mValue = newValue;
        postInvalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int center = (int) (getWidth() * HALF_FACTOR);
        int radius = (int) (center - mHalfBorder);
        mPaint.setColor(mBackColor);
        mPaint.setStyle(mStyle);
        mPaint.setStrokeWidth(mBorderWidth);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(center, center, radius, mPaint);

        int percent = (int) (mValue * DIVIDE_BY / mMax);

        mPaint.setStrokeWidth(mBorderWidth);
        mPaint.setColor(mFontColor);
        RectF oval = new RectF(center - radius, center - radius, center + radius, center + radius);
        int angle = (int) (CIRCLE_DEGREE * percent / DIVIDE_BY);
        mPaint.setStyle(mStyle);
        canvas.drawArc(oval, mStartPos, angle, mIsFill, mPaint);
    }

}
