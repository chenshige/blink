package com.blink.browser.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.ViewUtils;

public class BrowserProgressSeekbar implements SeekBar.OnSeekBarChangeListener {


    public static final int TYPE_TEXT_SIZE = 0;
    public static final int TYPE_BRIGHTNESS = 1;

    private static final int SEEKBAR_DIFFERENCE_FONTSIZE = 6;//与设置中设置字体大小的差值
    private static final int SEEKBAR_MAX_PROGRESS = 14;//设置中设置字体的长度是20,这里是14，用于主页的特殊情况
    private static final int SEEKBAR_MAX_BRIGHTNESS = 100;

    private int mCurrentProgress = 0;
    private int mMax = SEEKBAR_MAX_PROGRESS;

    private Context mContext;
    private View mView;
    private PopupWindow mPopWindow;
    private ImageView mProgressIcon;
    private SeekBar mSeekBar;
    private View mMountView;
    private long mOldTime;
    private int mType;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            dismiss();
        }
    };

    private Thread mThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean isClose = false;
            while (!isClose) {
                long currentTime = System.currentTimeMillis();
                if (mOldTime == 0) {
                    mOldTime = currentTime;
                } else if (currentTime - mOldTime > 1000 * 3) {
                    isClose = true;
                    Message message = new Message();
                    mHandler.sendMessage(message);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public BrowserProgressSeekbar(Context context, View mMountView, int type) {
        mType = type;
        mContext = context;
        this.mMountView = mMountView;
        initView();
    }

    public void initView() {
        mView = View.inflate(mContext, R.layout.browser_progress_seekbar, null);
        init();

        mPopWindow = new PopupWindow(mView, mMountView.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPopWindow.setFocusable(true);
        mPopWindow.setOutsideTouchable(true);
        mPopWindow.setBackgroundDrawable(new BitmapDrawable());
        mPopWindow.setAnimationStyle(R.style.progress_text_size_style);
    }

    private void init() {

        mProgressIcon = (ImageView) mView.findViewById(
                R.id.progress_icon);

        mSeekBar = (SeekBar) mView.findViewById(
                R.id.seekbar);

        switch (mType) {
            case TYPE_TEXT_SIZE:
                mMax = SEEKBAR_MAX_PROGRESS;
                mProgressIcon.setImageResource(R.drawable.ic_browser_progress_font);
                mCurrentProgress = BrowserSettings.getInstance().getPreferences().getInt(PreferenceKeys.PREF_MIN_FONT_SIZE, 0);
                //用于主页的特殊情况（0代表10PT）
                if (mCurrentProgress > SEEKBAR_DIFFERENCE_FONTSIZE) {
                    mCurrentProgress -= SEEKBAR_DIFFERENCE_FONTSIZE;
                } else {
                    mCurrentProgress = 0;
                }
                break;
            case TYPE_BRIGHTNESS:
                mMax = SEEKBAR_MAX_BRIGHTNESS;
                mProgressIcon.setImageResource(R.drawable.ic_browser_progress_brightness);
                float value = BrowserSettings.getInstance().getBrightness();
                value = value == DisplayUtil.DEFAULT_BRIGHTNESS
                        ? DisplayUtil.getSystemBrightness((Activity) mContext) : value;
                DisplayUtil.setScreenBrightness((Activity) mContext, value);
                mCurrentProgress = (int) (value * SEEKBAR_MAX_BRIGHTNESS);
                break;
            default:
                mMax = SEEKBAR_MAX_BRIGHTNESS;
                mCurrentProgress = BrowserSettings.getInstance().getPreferences().getInt(PreferenceKeys.PREF_MIN_FONT_SIZE, 0);
                break;
        }
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mCurrentProgress);
    }


    public void show() {
        if (mMountView == null) {
            return;
        }
        int[] location = new int[2];
        mMountView.getLocationOnScreen(location);
        int height = ViewUtils.getHeightOfView(mPopWindow.getContentView());
        int y = location[1];
        mPopWindow.showAtLocation(mMountView, Gravity.NO_GRAVITY, location[0],
                y - height - mContext.getResources().getDimensionPixelOffset(R.dimen.progress_seekbar_margin_bottom));

        ObjectAnimator scaleXAnimation =
                ObjectAnimator.ofFloat(mView, "scaleX", 0.75f, 1.0f);
        ObjectAnimator scaleYAnimation =
                ObjectAnimator.ofFloat(mView, "scaleY", 0.75f, 1.0f);
        ObjectAnimator alphaAnimation =
                ObjectAnimator.ofFloat(mView, "alpha", 0.0f, 1.0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleXAnimation).with(scaleYAnimation).with(alphaAnimation);
        animatorSet.play(scaleXAnimation);
        animatorSet.setDuration(200);
        animatorSet.start();

        mThread.start();

    }

    public int getType() {
        return mType;
    }

    public boolean isShowing() {
        return mPopWindow != null ? mPopWindow.isShowing() : false;
    }

    public void dismiss() {
        if (mPopWindow != null)
            mPopWindow.dismiss();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mCurrentProgress = progress;
            switch (mType) {
                case TYPE_TEXT_SIZE:
                    //用于主页的特殊情况
                    if (progress != 0) {
                        progress += SEEKBAR_DIFFERENCE_FONTSIZE;
                    }
                    BrowserSettings.getInstance().getPreferences().edit().putInt(PreferenceKeys.PREF_MIN_FONT_SIZE, progress)
                            .apply();
                    int value = BrowserSettings.getAdjustedMinimumFontSize(progress);
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, PreferenceKeys
                            .PREF_MIN_FONT_SIZE, value + "PT");
                    break;
                case TYPE_BRIGHTNESS:
                    DisplayUtil.setScreenBrightness((Activity) mContext, mCurrentProgress / (float) SEEKBAR_MAX_BRIGHTNESS);
                    BrowserSettings.getInstance().setBrightness(mCurrentProgress / (float) SEEKBAR_MAX_BRIGHTNESS);
                    break;
                default:
                    break;
            }

            mOldTime = 0;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
