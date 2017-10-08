package com.blink.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.blink.browser.R;

public class CircleImageView extends ImageView {
    private int mRadius;
    private Paint mCirclePaint;

    public CircleImageView(Context context) {
        super(context);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        mRadius = getWidth() / 2;
        mCirclePaint = new Paint();
        mCirclePaint.setColor(getContext().getResources().getColor(R.color.normal_text_color));
        mCirclePaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mRadius, mCirclePaint);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width > height) {
            width = height;
        }
        setMeasuredDimension(width, height);
    }

    public void setRadius(int radius) {
        mRadius = radius;
    }

    public void setFillColor(int color) {
        mCirclePaint.setColor(color);
    }
}
