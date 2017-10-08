package com.blink.browser.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;

/**
 * 自定已风格的Dialog
 * 基类的Dialog,项目中的Dialog可以即集成此类
 */
public class BrowserDialog extends Dialog implements View.OnClickListener {

    private int mIcon;
    private String mTitle;
    private String mNegative;
    private String mPositive;
    private String mNeutral;
    private String mMassage;
    private Button mNegativeButton;
    private Button mPositiveButton;
    private Button mNeutralButton;
    private LinearLayout mContentLayout;
    private View mParentView;
    private View mView;
    private int mHeight = 0;
    private int mWidth = 0;
    private DialogInterface.OnClickListener mPositiveListener;
    private DialogInterface.OnClickListener mNegativeListener;
    private DialogInterface.OnClickListener mNeutralListener;

    public BrowserDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public BrowserDialog(Context context) {
        this(context, R.style.BrowserDialog);
    }

    public BrowserDialog(Context context, String massge) {
        this(context, R.style.BrowserDialog);
        setBrowserMessage(massge);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        mParentView = LayoutInflater.from(getContext()).inflate(R.layout.browser_dialog, null);
        setContentView(mParentView);
        changeContentView();

        RelativeLayout mDialogTitle = (RelativeLayout) mParentView.findViewById(R.id.dialog_title);
        ImageView icon = (ImageView) mParentView.findViewById(R.id.icon);
        TextView title = (TextView) mParentView.findViewById(R.id.title);
        mNegativeButton = (Button) mParentView.findViewById(R.id.negative);
        mNeutralButton = (Button) mParentView.findViewById(R.id.neutral);
        mPositiveButton = (Button) mParentView.findViewById(R.id.positive);
        mContentLayout = (LinearLayout) mParentView.findViewById(R.id.content_dialog);
        TextView content = (TextView) mParentView.findViewById(R.id.content_text);
        LinearLayout negativePositive = (LinearLayout) mParentView.findViewById(R.id.negative_positive);

        mNegativeButton.setOnClickListener(this);
        mNeutralButton.setOnClickListener(this);
        mPositiveButton.setOnClickListener(this);

        if (mIcon == 0 && TextUtils.isEmpty(mTitle)) {
            mDialogTitle.setVisibility(View.GONE);
        } else {
            mDialogTitle.setVisibility(View.VISIBLE);
            icon.setImageResource(mIcon);
            title.setText(mTitle);
        }

        if (TextUtils.isEmpty(mMassage)) {
            content.setVisibility(View.GONE);
        } else {
            content.setVisibility(View.VISIBLE);
            content.setText(mMassage);
        }

        if (mView != null) {
            mContentLayout.addView(mView);
        }

        if (TextUtils.isEmpty(mNegative) && TextUtils.isEmpty(mPositive) && TextUtils.isEmpty(mNeutral)) {
            negativePositive.setVisibility(View.GONE);
        } else {
            negativePositive.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(mNegative)) {
                mNegativeButton.setVisibility(View.GONE);
            } else {
                mNegativeButton.setText(mNegative);
                mNegativeButton.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(mNeutral)) {
                mNeutralButton.setVisibility(View.GONE);
            } else {
                mNeutralButton.setText(mNeutral);
                mNeutralButton.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(mPositive)) {
                mPositiveButton.setVisibility(View.GONE);
            } else {
                mPositiveButton.setText(mPositive);
                mPositiveButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public BrowserDialog setBrowserContentView(View v) {
        if (v != null) {
            mView = v;
            if (mContentLayout != null) {
                mContentLayout.addView(mView);
            }

        }
        return this;
    }


    public BrowserDialog setHeight(int height) {
        mHeight = height;
        return this;
    }

    public BrowserDialog setWidth(int width) {
        mWidth = width;
        return this;
    }

    /**
     * 设置标题图标
     *
     * @param rid
     */
    public BrowserDialog setBrowserIcon(int rid) {
        mIcon = rid;
        return this;
    }

    /**
     * 设置标题图标
     *
     * @param attrId
     */
    public BrowserDialog setBrowserIconAttr(int attrId) {
        TypedValue out = new TypedValue();
        getContext().getTheme().resolveAttribute(attrId, out, true);
        mIcon = out.resourceId;
        return this;
    }

    /**
     * 表提设置
     *
     * @param vaule 字符串
     */
    public BrowserDialog setBrowserTitle(String vaule) {
        mTitle = vaule;
        return this;
    }

    /**
     * 表提设置
     *
     * @param rid 字符串id
     */
    public BrowserDialog setBrowserTitle(int rid) {
        mTitle = getContext().getText(rid).toString();
        return this;
    }

    /**
     * 内容设置
     *
     * @param massge 字符串
     */
    public BrowserDialog setBrowserMessage(String massge) {
        mMassage = massge;
        return this;
    }

    /**
     * 内容设置
     *
     * @param rid 字符串id
     */
    public BrowserDialog setBrowserMessage(int rid) {
        mMassage = getContext().getText(rid).toString();
        return this;
    }

    /**
     * 取消按钮设置
     *
     * @param vaule 字符串
     */
    public BrowserDialog setBrowserNegativeButton(String vaule) {
        mNegative = vaule;
        return this;
    }

    /**
     * 取消按钮设置
     *
     * @param rid 字符串id
     */
    public BrowserDialog setBrowserNegativeButton(int rid) {
        mNegative = getContext().getText(rid).toString();
        return this;
    }

    /**
     * 取消按钮监听设置
     */
    public BrowserDialog setBrowserNegativeButtonListener(DialogInterface.OnClickListener listener) {
        mNegativeListener = listener;
        return this;
    }

    /**
     * 中间按钮设置
     *
     * @param vaule 字符串
     */
    public BrowserDialog setBrowserNeutralButton(String vaule) {
        mNeutral = vaule;
        return this;
    }

    /**
     * 中间按钮设置
     *
     * @param rid 字符串id
     */
    public BrowserDialog setBrowserNeutralButton(int rid) {
        mNeutral = getContext().getText(rid).toString();
        return this;
    }

    /**
     * 中间按钮监听设置
     */
    public BrowserDialog setBrowserNeutralButtonListener(DialogInterface.OnClickListener listener) {
        mNeutralListener = listener;
        return this;
    }

    /**
     * 确定按钮设置
     *
     * @param vaule 字符串
     */
    public BrowserDialog setBrowserPositiveButton(String vaule) {
        mPositive = vaule;
        return this;
    }

    /**
     * 确定按钮设置
     *
     * @param rid 字符串id
     */
    public BrowserDialog setBrowserPositiveButton(int rid) {
        mPositive = getContext().getText(rid).toString();
        return this;
    }

    /**
     * 确定按钮监听设置
     */
    public BrowserDialog setBrowserPositiveButtonListener(DialogInterface.OnClickListener listener) {
        mPositiveListener = listener;
        return this;
    }

    public Button getPositiveButton() {
        return mPositiveButton;
    }

    public Button getNegativeButton() {
        return mNegativeButton;
    }

    public Button getNeutralButton() {
        return mNeutralButton;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.negative:
                onNegativeButtonClick();
                break;
            case R.id.positive:
                onPositiveButtonClick();
                break;
            case R.id.neutral:
                onNeutralButtonClick();
                break;
        }
    }

    /**
     * 点击确定事件
     */
    public void onPositiveButtonClick() {
        super.dismiss();
        if (mPositiveListener != null) {
            mPositiveListener.onClick(this, R.id.positive);
        }
    }

    /**
     * 点击取消事件
     */
    public void onNegativeButtonClick() {
        super.dismiss();
        if (mNegativeListener != null) {
            mNegativeListener.onClick(this, R.id.negative);
        }
    }

    /**
     * 点击中间事件
     */
    public void onNeutralButtonClick() {
        super.dismiss();
        if (mNeutralListener != null) {
            mNeutralListener.onClick(this, R.id.neutral);
        }
    }

    @Override
    public void show() {
        super.show();
    }

    private void changeContentView() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mParentView.getLayoutParams();
        int width = DisplayUtil.getScreenWidth(getContext());
        int height = DisplayUtil.getScreenHeight(getContext());

        mWidth = mWidth > 0 ? mWidth : (height > width ? width : height) * 9 / 10;
        params.width = mWidth;
        params.height = mHeight > 0 ? mHeight : ViewGroup.LayoutParams.WRAP_CONTENT;
        mParentView.setLayoutParams(params);
    }

    public void onConfigurationChanged() {
        changeContentView();
    }
}
