package com.blink.browser.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.blink.browser.R;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.util.RecommendUrlUtil;

public class RoundImageView extends ImageView {

    private static final float WEB_ICON_SCALE_VALUE = 2f / 3f;
    private String mText;
    private Bitmap mIcon;
    private TextPaint mTextPaint;
    public static final int RECTANGLE = 0;
    public static final int ROUND_RECTANGLE = 1;
    public static final int CIRCLE = 2;
    private float[] mSmallRadius;
    private int mType;
    private float[] mLargeRadius = new float[]{90, 90, 90, 90, 90, 90, 90, 90};
    private int mRoundBg;
    private int mBackBg;
    private int mCornerRadius;
    private boolean mNative;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mViewSizeF = new RectF();
    private Rect mViewSize = new Rect();
    private Rect mWebIconDisplayLocation = new Rect();
    private Rect mWebIconSize = new Rect();

    public RoundImageView(Context context) {
        super(context);
        init(context, null);
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        setScaleType(ScaleType.CENTER_CROP);
        initPaint();
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RoundImageView);
        mType = a.getInt(R.styleable.RoundImageView_type, 0);
        mCornerRadius = a.getDimensionPixelSize(R.styleable.RoundImageView_borderRadius, DisplayUtil.dip2px(context, 5));
        a.recycle();
        mSmallRadius = new float[]{mCornerRadius, mCornerRadius, mCornerRadius,
                mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius};
    }

    private void initPaint() {
        mTextPaint = new TextPaint();
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setText(String text) {
        if (TextUtils.equals(text, mText)) return;
        this.mText = text;
        this.mIcon = null;
        postInvalidate();
    }

    public void setIcon(String url, Bitmap icon) {
        mNative = false;
        int bgColor = getColorByIcon(icon);
        if (bgColor == 0 || bgColor == Color.WHITE) {
            bgColor = getColorByText(url);
        }
        setRoundBg(bgColor);
        setBitmapAndBackground(null);
        this.mIcon = icon;
        this.mText = null;
        postInvalidate();
    }

    @ColorInt
    private int getColorByText(String text) {
        return getColorByHash(text.hashCode());
    }

    private int getColorByHash(int hash) {
        int indexOfColor = Math.abs(hash) % BrowserContract.webIconWebArray.length;
        return ContextCompat.getColor(getContext(), BrowserContract.webIconWebArray[indexOfColor]);
    }

    @ColorInt
    private int getColorByIcon(Bitmap icon) {
        int topMiddleX = icon.getWidth() / 2;
        return icon.getPixel(topMiddleX, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        mViewSizeF.set(0, 0, viewWidth, viewHeight);
        mPaint.setColor(mRoundBg);
        if (mType == CIRCLE) {
            canvas.drawOval(mViewSizeF, mPaint);
        } else if (mType == ROUND_RECTANGLE) {
            canvas.drawRoundRect(mViewSizeF, mCornerRadius, mCornerRadius, mPaint);
        } else {
            canvas.drawRoundRect(mViewSizeF, 0, 0, mPaint);
        }
        super.onDraw(canvas);
        mViewSize.set(0, 0, viewWidth, viewHeight);
        if (mType == ROUND_RECTANGLE) {
            int nSave = openRounds(canvas, mViewSize, mSmallRadius);
            canvas.restoreToCount(nSave);
        } else if (mType == CIRCLE) {
            int nSave = openRounds(canvas, mViewSize, mLargeRadius);
            canvas.restoreToCount(nSave);
        }
        if (!mNative) {
            if (mText != null) {
                mTextPaint.setTextSize(viewHeight * 2 / 5);
                canvas.drawText(mText, (viewWidth / 2), (viewHeight / 2) + mTextPaint.getFontMetrics().bottom, mTextPaint);
            }
            if (mIcon != null) {
                mWebIconSize.set(0, 0, mIcon.getWidth(), mIcon.getHeight());
                int iconDisplayWidth = (int) (viewWidth * WEB_ICON_SCALE_VALUE);
                int iconDisplayHeight = (int) (viewHeight * WEB_ICON_SCALE_VALUE);
                int left = (viewWidth - iconDisplayWidth) / 2;
                int top = (viewHeight - iconDisplayHeight) / 2;
                mWebIconDisplayLocation.set(left, top, left + iconDisplayWidth, top + iconDisplayHeight);
                canvas.drawBitmap(mIcon, mWebIconSize, mWebIconDisplayLocation, mPaint);
            }
        }
    }

    public static int openRounds(Canvas canvas, Rect rt, float[] rounds) {
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG));
        Path round = new Path();
        RectF rtf = new RectF();
        rtf.set(rt);
        round.addRoundRect(rtf, rounds, Path.Direction.CW);
        int nSave = canvas.save();
        canvas.clipPath(round);
        return nSave;
    }


    public void setBackgroundBg(int color) {
        mBackBg = color;
    }

    public void setRoundBg(int color) {
        mRoundBg = color;
    }

    /**
     * 生成默认图标
     *
     * @param url 用于计算颜色值的text
     */
    public void setDefaultIconByUrl(String url) {
        mNative = false;
        if (!TextUtils.isEmpty(url)) {
            String webName = RecommendUrlUtil.getWebSimpleNameByUrl(url);
            setRoundBg(getColorByText(url));
            setBitmapAndBackground(null);
            setText(webName);
        }
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        if (bm == null) return;
        mNative = true;
        setText(null);
        setRoundBg(Color.TRANSPARENT);
        setBitmapAndBackground(ImageUtils.makeRoundCornerImage(bm, mCornerRadius));
    }

    @Override
    public void setImageResource(int resId) {
        mNative = true;
        setText(null);
        super.setImageResource(resId);
        setBackgroundColor(mBackBg);
    }

    private void setBitmapAndBackground(Bitmap bm) {
        if (bm != null) {
            super.setImageBitmap(bm);
        }
        setBackgroundColor(mBackBg);
    }
}
