package com.blink.browser.menu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.blink.browser.R;

public class CommonMenuCircleImageView extends ImageView {
    private boolean mIsSelected;
    private boolean mIsIncognito;
    private int mRadius = -1;
    private boolean mIsNativeMenu;
    private boolean mIsHideCricle;
    Paint mCircleLinePaint;

    public CommonMenuCircleImageView(Context context) {
        super(context);
        init();
    }

    public CommonMenuCircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        mCircleLinePaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        if (mIsNativeMenu) {
            mCircleLinePaint.setColor(getContext().getResources().getColor(R.color.native_menu_circle_color));
            mCircleLinePaint.setStyle(Paint.Style.STROKE);
        } else {
            if (!mIsSelected) {
                if (!mIsIncognito) {
                    mCircleLinePaint.setColor(getContext().getResources().getColor(R.color.context_menu_round_image_circle_color));
                } else {
                    mCircleLinePaint.setColor(getContext().getResources().getColor(R.color.context_menu_round_image_circle_color_incognito));
                }
                mCircleLinePaint.setStyle(Paint.Style.STROKE);
            } else {
                mCircleLinePaint.setColor(getContext().getResources().getColor(R.color.context_menu_round_image_selected_color));
                mCircleLinePaint.setStyle(Paint.Style.FILL);
            }
        }
        if (mIsHideCricle) {
            mCircleLinePaint.setAlpha(0);
        }
        mCircleLinePaint.setAntiAlias(true);
        mCircleLinePaint.setStrokeWidth(getResources().getDimensionPixelOffset(R.dimen.menu_imageview_circle_width));
        if (mRadius < 0) {
            mRadius = calculateRadius();
        }
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mRadius, mCircleLinePaint);
        canvas.restore();
        canvas.save();
        super.onDraw(canvas);
    }

    private int calculateRadius() {
        ViewGroup parent = ((ViewGroup) getParent());
        if (parent == null) {
            return getHeight() / 2 - 1;
        }
        int parentWidth = parent.getWidth();
        int imageWidth = parentWidth / 4;
        if (getHeight() > imageWidth) {
            return imageWidth / 2 - 1;
        } else {
            return getHeight() / 2 - 1;
        }
    }

    public void setRadius(int radius) {
        mRadius = radius;
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

    public void setIsSelected(boolean isSelected) {
        mIsSelected = isSelected;
        invalidate();
    }

    public void setIsIncognito(boolean isIncognito) {
        mIsIncognito = isIncognito;
        invalidate();
    }

    public void setIsNativeMenu(boolean isNativeMenu) {
        mIsNativeMenu = isNativeMenu;
        invalidate();
    }

    public void setIsHideCircle(boolean hideCircle) {
        mIsHideCricle = hideCircle;
        invalidate();
    }
}
