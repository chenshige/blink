package com.blink.browser.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;

import java.util.Timer;
import java.util.TimerTask;

public class TranslatePopup extends PopupWindow implements View.OnClickListener {
    private Activity mActivity;
    private BtnClickListener mBtnOnClickListener;
    private TextView mNoticeMessage;
    private Button mUndoBotton;
    private TextView mSnakebarEllipsis;
    private TimerTask mTimerTask;
    private int mCount = 0;

    public TranslatePopup(Context context, BtnClickListener listener) {
        super(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
        mBtnOnClickListener = listener;
        initView(context);
    }

    public void initView(Context context) {
        View mContentView = View.inflate(context, R.layout.pop_tool_bar, null);
        mNoticeMessage = (TextView) mContentView.findViewById(R.id.bottom_bar_content);
        mUndoBotton = (Button) mContentView.findViewById(R.id.btn_undo);
        mUndoBotton.setVisibility(View.VISIBLE);
        mSnakebarEllipsis = (TextView) mContentView.findViewById(R.id.snakebar_ellipsis);
        mUndoBotton.setOnClickListener(this);

        this.setContentView(mContentView);
        this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        this.setHeight((int) context.getResources().getDimension(R.dimen.history_close_button_size));
        this.update();
        ColorDrawable dw = new ColorDrawable(context.getResources().getColor(R.color.parent_view_general_background));
        this.setBackgroundDrawable(dw);
        this.setAnimationStyle(R.style.bot_pop_anim);
        this.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mBtnOnClickListener != null) {
                    mBtnOnClickListener.handlerTask();
                }
            }
        });
    }

    public void show() {
        if (!this.isShowing() && mActivity != null) {
            final Timer timer = new Timer();
            this.showAtLocation(mActivity.getWindow().getDecorView(), Gravity.BOTTOM, 0, DisplayUtil.getNavBarHeight(mActivity));
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mCount == 0) {
                                mCount++;
                                mSnakebarEllipsis.setText("");
                            } else if (mCount == 1) {
                                mCount++;
                                mSnakebarEllipsis.setText(".");
                            } else if (mCount == 2) {
                                mCount++;
                                mSnakebarEllipsis.setText("..");
                            } else if (mCount == 3) {
                                mCount = 0;
                                mSnakebarEllipsis.setText("...");
                            }
                        }
                    });
                }
            };
            timer.schedule(mTimerTask, 0, 500);
        } else {
            this.dismiss();
        }
    }

    public void clear() {
        if (mActivity != null && !mActivity.isFinishing()) {
            TranslatePopup.this.dismiss();
        }
    }

    public void setBtnText(String text) {
        if (text != null) {
            mUndoBotton.setText(text);
        }
    }

    public void setContentText(String text) {
        if (text != null) {
            mNoticeMessage.setText(text);
        }
    }

    public void finishMessage(boolean isNow) {
        mTimerTask.cancel();
        mSnakebarEllipsis.setText("");
        if (isNow) {
            clear();
            return;
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                clear();
            }
        }, 5000);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_undo:
                mUndoBotton.setVisibility(View.GONE);
                if (mBtnOnClickListener != null) {
                    mBtnOnClickListener.doClick();
                }
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        clear();
                    }
                }, 800);
                break;
        }
    }

    public interface BtnClickListener {
        void doClick();

        void handlerTask();
    }
}
