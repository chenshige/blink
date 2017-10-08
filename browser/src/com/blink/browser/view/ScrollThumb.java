package com.blink.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Interpolator;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.blink.browser.BrowserSettings;
import com.blink.browser.BrowserWebView;
import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;

import java.lang.reflect.Method;

public class ScrollThumb {
    /** Log */
    private static final String TAGLOG ="ScrollThumb";
    /** This for might shake. */
    private static final int TOUCH_SHAKE = 10;

    /** The host view that ScrollThumb will be drawn.*/
    private final ViewGroup mView;
    private final Context mContext;
    private final BrowserWebView mAwContents;
    /** The resource of ScrollThumb.*/
    private Drawable mThumbDrawable;
    private Drawable mThumbDrawableNight;

    /** The size of ScrollThumb. */
    private final int mThumbWidth;
    private final int mThumbHeight;

    /** The current ScrollThumb position. */
    private int mScrollbarPosition = -1;
    /** Whether decorations should be laid out from right to left. */
    private boolean mLayoutFromRight;

    /** Whether the ScrollThumb is enabled. */
    private boolean mEnabled;

    /** The padding of ScrollThumb from right to left. */
    private int mPadding;

    /** Whether the ScrollThumb should be created by velocity. */
    private static int mVelocityThreshold = 3500;
    /** Whether View is night mode. */
    private boolean mNightMode;

    /** The initial touched position. */
    private float mInitialTouchPos;
    /** The time of the last action down. */
    private long mLastDownTime;
    /** The time of this event. */
    private long mThisEventTime;
    /** The time of the last event.*/
    private long mLastEventTime = 0;
    /** Whether the action is long pressed. */
    private boolean mLongPressed;
    /** Whether the ScrollThumb is dragging. */
    private boolean mDragging;

    private ScrollabilityCache mScrollCache;
    private Rect mScrollPositionRect;
    private static final Handler mHandler = new Handler();
    private static int sTop;
    private static int sStatusBarHeight;

    public ScrollThumb(BrowserWebView awContents, Context context, boolean isNightMode) { //ViewGroup view,
        mAwContents = awContents;
        mView = awContents;
        mContext = context;
        sStatusBarHeight = DisplayUtil.getStatusBarHeight(mContext);
        sTop = mContext.getResources().getDimensionPixelOffset(R.dimen.toolbar_height);
        mThumbWidth = (int)context.getResources().getDimension(R.dimen.scroll_thumb_bar_width);
        mThumbHeight = (int)context.getResources().getDimension(R.dimen.scroll_thumb_bar_height);
        mNightMode = isNightMode;
        // Get ScrollThumb drawable if it has an image.
        mThumbDrawable = context.getResources().getDrawable(R.drawable.scroll_thumb);
        mThumbDrawableNight = context.getResources().getDrawable(R.drawable.scroll_thumb);

        mPadding = 5;

        setScrollThumbPosition(mView.getVerticalScrollbarPosition());
        initializeScrollbarsInternal();
    }

    /**
     * @return Whether the ScrollThumb is enabled.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * @param enabled Whether the ScrollThumb is enabled.
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * @param padding Whether the ScrollThumb should be laied out by padding
     */
    public void setPadding(int padding) {
        mPadding = padding;
    }

    /**
     * @return Whether the ScrollThumb is dragging.
     */
    public boolean isDragging() {
        return mDragging;
    }

    /**
     * @return Whether the ScrollThumb is visible.
     */
    public boolean isVisible() {
        return mScrollCache.state != ScrollabilityCache.OFF;
    }

    /**
     * @param threshold Whether the ScrollThumb should be shown by fling velocity
     */
    public static void setVelocityThreshold(int threshold) {
        mVelocityThreshold = threshold;
    }

    /**
     * @param threshold Whether the ScrollThumb should be shown
     */
    public static boolean checkVelocityThreshold(BrowserWebView awContents, ViewGroup view, float threshold) {
        return mVelocityThreshold <= Math.abs(threshold)
                && awContents.getContentHeight() >= view.getHeight();
    }

    /**
     * @param enabled Whether the NightMode is enabled.
     */
    public void setNightModeEnabled(boolean enabled) {
        if (mNightMode == enabled)
            return;

        mNightMode = enabled;
        mScrollCache.mThumb = mNightMode ? mThumbDrawableNight : mThumbDrawable;
    }

    private void setScrollThumbPosition(int position) {
        if (position == View.SCROLLBAR_POSITION_DEFAULT) {
            try {
                Class<?> view = getClass().getClassLoader().loadClass("android.view.View");
                Method method = view.getMethod("isLayoutRtl");
                position = (boolean)method.invoke(null) ?
                        View.SCROLLBAR_POSITION_LEFT : View.SCROLLBAR_POSITION_RIGHT;
            } catch (Exception e) {
                position = View.SCROLLBAR_POSITION_RIGHT;
            }
        }

        if (mScrollbarPosition != position) {
            mScrollbarPosition = position;
            mLayoutFromRight = position != View.SCROLLBAR_POSITION_LEFT;
        }
    }

    /**
     * Cancels a dragging.
     */
    private void cancelDragging() {
        mDragging = false;
    }

    private void beginDragging() {
        mDragging = true;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled() || !isVisible())
            return false;
        final ScrollabilityCache cache = mScrollCache;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isPointInside(ev.getX(), ev.getY())) {
                    mInitialTouchPos = ev.getY();
                    mLastDownTime = ev.getDownTime();
                    mLastEventTime = ev.getDownTime();

                    beginDragging();
                    cache.setTouched(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isPointInside(ev.getX(), ev.getY())) {
                    cancelDragging();
                    cache.setTouched(false);
                } else {
                    setFadingEnabled(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelDragging();
                break;
        }

        return false;
    }

    public boolean onTouchEvent(MotionEvent me) {
        if (!isEnabled() || !isVisible())
            return false;

        final ScrollabilityCache cache = mScrollCache;
        switch (me.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                if (Math.abs(mLastEventTime - me.getEventTime()) < 10) {
                    return false;
                } else {
                    mLastEventTime = me.getEventTime();
                }

                if (mDragging
                        && Math.abs(me.getY() - mInitialTouchPos) > TOUCH_SHAKE) {
                    if (!mLongPressed) {
                        mThisEventTime = me.getEventTime();
                        mLongPressed = mThisEventTime - mLastDownTime > 100 ? true : false;
                    }
                }
                if (cache.state != ScrollabilityCache.OFF) {
                    awakenScrollThumb();
                }
                if (mLongPressed && mDragging) {
                    // If the previous scrollTo is still pending
                    scrollTo(cache.setScrollRatio(me.getRawY()));

                    return true;
                }
            }
            break;
            case MotionEvent.ACTION_UP: {
                mLongPressed = false;
                cache.setTouched(false);
                if (mDragging) {
                    cancelDragging();
                    return true;
                }
            } break;

        }

        return false;
    }

    public boolean onLongClick() {
        if (!isEnabled() || !isVisible())
            return false;

        return mLongPressed;
    }


    /**
     * Scrolls to a specific position
     * @param position
     */
    private void scrollTo(float position) {
        float sumHeight = (float)mAwContents.getContentHeight() * mAwContents.getScale() - (float) mView.getHeight();
        mView.scrollTo(mView.getScrollX(), (int)(sumHeight * position));
    }

    /**
     * Returns whether a coordinate is inside the ScrollThumb's activation area.
     * Touching anywhere within the thumb-width of the
     * ScrollThumb activates scrolling. Otherwise, the user has to touch inside
     *  thumb itself.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return Whether the coordinate is inside the ScrollThumb's activation area.
     */
    private boolean isPointInside(float x, float y) {
        return isPointInsideX(x) && isPointInsideY(y);
    }

    private boolean isPointInsideX(float x) {
        final Rect posRect = mScrollPositionRect;
        x += mView.getScrollX();
        if (mLayoutFromRight) {
            return x >= posRect.left;
        } else {
            return x <= posRect.right;
        }
    }

    private boolean isPointInsideY(float y) {
        final Rect posRect = mScrollPositionRect;
        y += mView.getScrollY();
        float top = posRect.top - 3 * mPadding < 0 ? posRect.top : posRect.top - 2 * mPadding;
        float bottom = posRect.bottom + 3 * mPadding;
        return y >= top && y <= bottom;
    }

    /**
     * Trigger the ScrollThumb to draw. When invoked this method starts an
     * animation to fade the ScrollThumb out after a default delay.
     */
    public boolean awakenScrollThumb() {
        return mScrollCache != null &&
                awakenScrollThumb(mScrollCache.mScrollThumbDefaultDelayBeforeFade, true);
    }

    /**
     * Trigger the ScrollThumb to draw.
     * This method differs from awakenScrollThumb() only in its default duration.
     * initialAwakenScrollThumb() will show the ScrollThumb for longer than
     * usual to give the user more of a chance to notice them.
     *
     * @return true if the animation is played, false otherwise.
     */
    public boolean initialAwakenScrollThumb() {
        return mScrollCache != null &&
                awakenScrollThumb(mScrollCache.mScrollThumbDefaultDelayBeforeFade * 4, true);
    }

    /**
     * Trigger the ScrollThumb to draw. When invoked this method starts an
     * animation to fade the ScrollThumb out after a fixed delay.
     *
     * @param startDelay the delay, in milliseconds, after which the animation
     *        should start; when the delay is 0, the animation starts
     *        immediately.
     * @return true if the animation is played, false otherwise.
     */
    public boolean awakenScrollThumb(int startDelay) {
        return awakenScrollThumb(startDelay, true);
    }

    /**
     *  Trigger the ScrollThumb to draw. When invoked this method starts an
     * animation to fade the ScrollThumb out after a fixed delay.
     *
     * @param startDelay the delay, in milliseconds, after which the animation
     *        should start; when the delay is 0, the animation starts
     *        immediately.
     * @param invalidate Wheter this method should call invalidate.
     * @return true if the animation is played, false otherwise.
     */
    public boolean awakenScrollThumb(int startDelay, boolean invalidate) {
        final ScrollabilityCache scrollCache = mScrollCache;

        if (scrollCache == null
                || !scrollCache.mFadeScrollThumb)
            return false;


        if (scrollCache.mThumb == null)
            scrollCache.mThumb = mNightMode ? mThumbDrawableNight : mThumbDrawable;

        if (invalidate)
            mView.postInvalidate();

        long fadeStartTime = AnimationUtils.currentAnimationTimeMillis() + startDelay;
        scrollCache.fadeStartTime = fadeStartTime;
        scrollCache.state = ScrollabilityCache.ON;
        mHandler.removeCallbacks(scrollCache);
        mHandler.postAtTime(scrollCache, fadeStartTime);

        mAwContents.setHorizontalScrollBarEnabled(false);
        mAwContents.setVerticalScrollBarEnabled(false);
        return true;
    }

    /**
     * Define whether ScrollThumb will fade when the view is not scrolling.
     *
     * @param fadeScrollThumb wheter to enable fading
     *
     */
    public void setFadingEnabled(boolean fadeScrollThumb) {
        initScrollCache();
        final ScrollabilityCache scrollabilityCache = mScrollCache;
        scrollabilityCache.mFadeScrollThumb = fadeScrollThumb;
        if (fadeScrollThumb) {
            scrollabilityCache.state = ScrollabilityCache.OFF;
        } else {
            scrollabilityCache.state = ScrollabilityCache.ON;
        }
    }

    private void initializeScrollbarsInternal() {
        initScrollCache();

        if (mScrollCache.mThumb == null)
            mScrollCache.mThumb = mNightMode ? mThumbDrawableNight : mThumbDrawable;
    }

    /**
     * Initalizes the scrollability cache if necessary.
     */
    private void initScrollCache() {
        if (mScrollCache == null) {
            mScrollCache = new ScrollabilityCache(ViewConfiguration.get(mContext), mView, mAwContents);
            mScrollPositionRect = new Rect();
        }
    }

    private ScrollabilityCache getScrollCache() {
        initScrollCache();
        return mScrollCache;
    }

    /**
     * <p>Request the drawing of the scrollThumb. The
     * scrollThumb are painted only if they have been awakened first.</p>
     *
     * @param canvas the canvas on which to draw the scrollThumb
     *
     * @see #awakenScrollThumb(int)
     */
    public final void onDrawScrollThumb(Canvas canvas) {
        // The ScrollThumb drawn only when the animation is running
        final ScrollabilityCache cache = mScrollCache;
        if (cache != null && isEnabled()) {

            int state = cache.state;
            if (state == ScrollabilityCache.OFF) {
                mAwContents.setHorizontalScrollBarEnabled(true);
                mAwContents.setVerticalScrollBarEnabled(true);
                return;
            }
            boolean invalidate = false;

            if (state == ScrollabilityCache.FADING) {
                // We're fading -- get our fade interpolation
                if (cache.interpolatorValues == null)
                    cache.interpolatorValues = new float[1];

                float[] values = cache.interpolatorValues;

                // Stops the animation if we're done
                if (cache.mScrollThumbInterpolator.timeToValues(values) ==
                        Interpolator.Result.FREEZE_END) {
                    mScrollCache.state = ScrollabilityCache.OFF;
                } else {
                    cache.mThumb.setAlpha(Math.round(values[0]));
                }

                // This will make the ScrollThumb inval themselves after
                // drawing. We only want this when we're fading so that
                // we prevent excessive redraws
                invalidate = true;
            } else {
                // We're just on -- but we may have been fading before so
                // reset alpha
                cache.mThumb.setAlpha(255);
            }

            final Drawable scrollThumb = cache.mThumb;

            final int width = mView.getWidth();
            final int scrollX = mView.getScrollX();
            final int scrollY = mView.getScrollY();

            // Compute the ratio
            if (!cache.isTouched()) {
                cache.setScrollRatio(scrollY);
            }

            int left;
            int top;
            int right;
            int bottom;

            final int size = mThumbWidth;
            final float offset = mView.getTop();
            final float range = mView.getMeasuredHeight() - offset;
            final float ratio = cache.getScrollRatio();
            float position = ratio * range + offset;

            left = scrollX + width - size - mPadding;
            top = scrollY + (int)(position); // - mThumbHeight
            right = left + size;
//            bottom = scrollY + (int)(position) - mView.getPaddingBottom();
            bottom = top + mThumbHeight;
            // When ScrollThumb is at the top
            if (top < mPadding) {
                top = mPadding;
                bottom = mThumbHeight;
            }

            if (bottom > mAwContents.getContentHeight() * mAwContents.getScale() - mPadding) {
                bottom = (int)(mAwContents.getContentHeight() * mAwContents.getScale()) - mPadding;
                top = bottom - mThumbHeight;
            }
            // Save ScrollThumb's positon
            mScrollPositionRect = new Rect(left, top, right, bottom);
            if(mAwContents.getContentHeight()*mAwContents.getScale() < (mAwContents.getHeight()+mAwContents.getScrollY())) {
                bottom = (int) (mAwContents.getContentHeight() * mAwContents.getScale());
                top = bottom - mThumbHeight;
            }
            // Draw ScrollThumb
            onDrawScrollThumb(canvas, scrollThumb, left, top, right, bottom);
            if (invalidate)
                mView.invalidate(left, top, right, bottom);
        }
    }

    /**
     * Draw the ScrollThumb.
     *
     * @param canvas the canvas on which to draw the ScrollThumb
    //     * @param ScrollThumb the scrollthumb's drawable
     */
    private void onDrawScrollThumb(Canvas canvas, Drawable scrollThumb,
                                   int l, int t, int r, int b) {

        scrollThumb.setBounds(l, t, r, b);
        scrollThumb.draw(canvas);
    }

    /**
     * <p>ScrollabilityCache holds various fields used by a View when scrolling
     * is supported. This avoids keeping too many unused fields in most
     * instances of View.</p>
     */
    private static class ScrollabilityCache implements Runnable {
        /**
         * scrollThumb is not visible
         */
        public static final int OFF = 0;

        /**
         * scrollThumb is visible
         */
        public static final int ON = 1;

        /**
         * scrollThumb is fading away
         */
        public static final int FADING = 2;

        public boolean mFadeScrollThumb = true;

        public int mScrollThumbDefaultDelayBeforeFade;
        public int mScrollThumbFadeDuration;

        public Drawable mThumb;
        public float[] interpolatorValues;
        private ViewGroup mHostView;
        private BrowserWebView mHostContents;

        public final Interpolator mScrollThumbInterpolator = new Interpolator(1, 2);

        private static final float[] OPAQUE = { 255 };
        private static final float[] TRANSPARENT = { 0.0f };

        private float mScrollRatio;
        private boolean mTouched = false;

        private float mOffset;
        private float mRange;

        /**
         * When fading should start. This time moves into the future every time
         * a new scroll happens. Measured based on SystemClock.uptimeMillis()
         */
        public long fadeStartTime;

        /**
         * The current state of the scrollThumb: ON, OFF, or FADING
         */
        public int state = OFF;

        public ScrollabilityCache(ViewConfiguration configuration, ViewGroup hostView, BrowserWebView awContents) {
            mScrollThumbDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay() * 3;
            mScrollThumbFadeDuration = ViewConfiguration.getScrollBarFadeDuration() * 4;

            mHostView = hostView;
            mHostContents = awContents;
            mOffset = mHostView.getTop();
            mRange = mHostView.getMeasuredHeight() - mOffset;
        }

        public float setScrollRatio(float pos) {
            mOffset = mHostView.getTop();
            mRange = mHostView.getMeasuredHeight() - mOffset;

            int height = mHostView.getHeight();
            float scale = mHostContents.getScale();
            float sumHeight = mHostContents.getContentHeight() * scale - height;

            if (mTouched) {
                if (BrowserSettings.getInstance().useFullscreen()) {
                    mScrollRatio = pos / mRange;
                } else {
                    mScrollRatio = (pos - sTop -sStatusBarHeight) / mRange;
                }
            } else {
                mScrollRatio = pos / sumHeight;
            }
            if (mScrollRatio <= 0f) {
                mScrollRatio = 0f;
            }
            if (mScrollRatio >= 1f) {
                mScrollRatio = 1f;
            }

            return mScrollRatio;
        }

        public float getScrollRatio() {
            return mScrollRatio;
        }

        public void setTouched(boolean touched) {
            this.mTouched = touched;
        }

        public boolean isTouched() {
            return mTouched;
        }

        public void run() {
            long now = AnimationUtils.currentAnimationTimeMillis();
            if (now >= fadeStartTime) {

                // the animation fades the scrollThumb out by changing
                // the opacity (alpha) from fully opaque to fully
                // transparent
                int nextFrame = (int) now;
                int framesCount = 0;

                Interpolator interpolator = mScrollThumbInterpolator;

                // Start opaque
                interpolator.setKeyFrame(framesCount++, nextFrame, OPAQUE);

                // End transparent
                nextFrame += mScrollThumbFadeDuration;
                interpolator.setKeyFrame(framesCount, nextFrame, TRANSPARENT);

                state = FADING;

                // Kick off the fade animation
                mHostView.invalidate();
            }
        }
    }
}
