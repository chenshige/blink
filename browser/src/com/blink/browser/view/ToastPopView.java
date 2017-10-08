package com.blink.browser.view;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.blink.browser.Browser;
import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;

public class ToastPopView {
    private View mView;
    private TextView mText;
    private PopupWindow mPopWindow;
    private TextView mTextButton;

    public ToastPopView(Context context) {
        this.mView = View.inflate(context, R.layout.toast_pop_view, null);
        mText = (TextView) mView.findViewById(R.id.toast_text);
        mTextButton = (TextView) mView.findViewById(R.id.toast_text_button);
        mPopWindow = new PopupWindow(mView, DisplayUtil.getScreenWidth(context), -2, true);
        mPopWindow.setFocusable(true);
        mPopWindow.setOutsideTouchable(false);
        mPopWindow.setBackgroundDrawable(new BitmapDrawable());
        mPopWindow.setAnimationStyle(R.style.popwindow_push_bottom);
    }

    public void show(View parentView, int height) {
        if (parentView == null) return;
        int nav = DisplayUtil.getNavBarHeight(Browser.getInstance().getApplicationContext());

        int orientation = parentView.getResources().getConfiguration().orientation;
        int x = 0;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            x = -DisplayUtil.getStatusBarHeight(Browser.getInstance().getApplicationContext());
        }

        mPopWindow.showAtLocation(parentView, Gravity.BOTTOM, x, nav + height);
    }

    public void dismiss() {
        mPopWindow.dismiss();
    }

    public ToastPopView setText(int text) {
        mText.setText(text);
        return this;
    }

    public ToastPopView setButtonText(int text) {
        mTextButton.setText(text);
        return this;
    }

    public ToastPopView setTextOnClickListener(View.OnClickListener listener) {
        mView.setOnClickListener(listener);
        return this;
    }

    public ToastPopView setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mPopWindow.setOnDismissListener(listener);
        return this;
    }

}
