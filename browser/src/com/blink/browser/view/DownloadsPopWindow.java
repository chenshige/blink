package com.blink.browser.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.ViewUtils;
import com.wcc.wink.request.DownloadInfo;


public class DownloadsPopWindow {
    private View mView;
    private PopupWindow mPopWindow;
    private boolean mDownloading;

    private DownloadInfo mDownloadInfo;

    public DownloadsPopWindow(Context context) {
        this.mView = View.inflate(context, R.layout.layout_download_popwindow, null);

        mPopWindow = new PopupWindow(mView, DisplayUtil.dip2px(context, 168), -2, true);
        mPopWindow.setFocusable(true);
        mPopWindow.setOutsideTouchable(true);
        mPopWindow.setBackgroundDrawable(new BitmapDrawable());
    }

    public void show(View parentView) {
        int[] location = new int[2];
        parentView.getLocationOnScreen(location);
        int height = ViewUtils.getHeightOfView(mPopWindow.getContentView());
        int x = location[0];
        int y = location[1];
        int screenHeight = DisplayUtil.getScreenHeight(parentView.getContext());
        if ((y + height) > screenHeight) {
            y = location[1] - height + parentView.getHeight();
            if ((y + height) > screenHeight) {
                y = screenHeight - height;
            }
        }
        if (mDownloading) {
            ((TextView) mView.findViewById(R.id.rename)).setTextColor(ContextCompat.getColor(this.mView.getContext(),
                    R.color.item_text_color));
        } else {
            ((TextView) mView.findViewById(R.id.rename)).setTextColor(ContextCompat.getColor(this.mView.getContext(),
                    R.color.settings_item_title_normal_color));
        }

        int pop_x = x + parentView.getWidth() - DisplayUtil.dip2px(parentView.getContext(), 180);
        mPopWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, pop_x > DisplayUtil.dip2px(parentView.getContext(),
                16) ?
                pop_x : x + DisplayUtil.dip2px(parentView.getContext(), 16), y);
    }

    public void dismiss() {
        mPopWindow.dismiss();
    }

    public void setClickListener(View.OnClickListener listener) {
        mView.findViewById(R.id.popwindow_copylink).setOnClickListener(listener);
        if (!mDownloading) {
            mView.findViewById(R.id.popwindow_rename).setOnClickListener(listener);
        }
        mView.findViewById(R.id.popwindow_delete).setOnClickListener(listener);
        mView.findViewById(R.id.popwindow_share).setOnClickListener(listener);
        dismiss();
    }

    public void setDownloading(Boolean downloading) {
        mDownloading = downloading;
    }

    public DownloadInfo getDownloadInfo() {
        return mDownloadInfo;
    }

    public void setDownloadInfo(DownloadInfo mDownloadInfo) {
        this.mDownloadInfo = mDownloadInfo;
    }
}
