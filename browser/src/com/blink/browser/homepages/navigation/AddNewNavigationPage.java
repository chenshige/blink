package com.blink.browser.homepages.navigation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.homepages.ScreenRotateable;
import com.blink.browser.util.InputMethodUtils;
import com.blink.browser.util.ToastUtil;

import java.util.Map;

/**
 * Add a new navigation manual
 */
@SuppressLint("ValidFragment")
public class AddNewNavigationPage extends Fragment implements View.OnFocusChangeListener, View.OnClickListener, ScreenRotateable {

    private View mRootView;
    private View mMaskView;
    private EditText mTitle;
    private EditText mAddress;
    private View mLine1, mLine2;
    private TextView mOk, mCancel;
    private WebNavigationEditable mNavigationEditable;
    private boolean mIsNavListFull;
    private OnFocusChangeListener mOnFocusChangeListener;

    @SuppressLint("ValidFragment")
    public AddNewNavigationPage(WebNavigationEditable navigationEditable) {
        this.mNavigationEditable = navigationEditable;
    }

    private boolean checkEditTextNull() {
        return mTitle == null || mAddress == null;
    }

    public void finishEdit() {
        if (checkEditTextNull()) return;
        mTitle.setText("");
        mAddress.setText("http://");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.edit_navigation, container, false);
        initView(mRootView);
        return mRootView;
    }

    private void initView(View rootView) {
        mMaskView = rootView.findViewById(R.id.mask);
        mTitle = (EditText) rootView.findViewById(R.id.title);
        mAddress = (EditText) rootView.findViewById(R.id.address);
        mTitle.setOnFocusChangeListener(this);
        mAddress.setOnFocusChangeListener(this);
        mLine1 = rootView.findViewById(R.id.add_link_line1);
        mLine2 = rootView.findViewById(R.id.add_link_line2);
        mOk = (TextView) rootView.findViewById(R.id.ok);
        mCancel = (TextView) rootView.findViewById(R.id.cancel);
        mOk.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mMaskView.setOnClickListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.title:
                mLine1.setBackgroundResource(hasFocus ? R.color.add_book_mark_save : R.color.add_book_mark_line);
                break;
            case R.id.address:
                mLine2.setBackgroundResource(hasFocus ? R.color.add_book_mark_save : R.color.add_book_mark_line);
                String url = mAddress.getText().toString();
                if (hasFocus) {
                    if (TextUtils.isEmpty(url)) {
                        String hintText = "http://";
                        mAddress.setText(hintText);
                        mAddress.setSelection(hintText.length());
                    }
                } else {
                    if (!TextUtils.isEmpty(url) && "http://".equals(url)) {
                        mAddress.setText("");
                    }
                }
                break;
        }
        if (mOnFocusChangeListener != null) {
            mOnFocusChangeListener.onFocusChange();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok:
                if (mIsNavListFull) {
                    ToastUtil.showShortToast(getContext(), R.string.add_up_to_15);
                    break;
                }
                if (saveEdit()) {
                    mNavigationEditable.onFinishAddNewNavigation();
                }
                break;
            case R.id.cancel:
                mNavigationEditable.onFinishAddNewNavigation();
                finishEdit();
                break;
            default:
                //doNothing
        }
    }

    public int getHeight() {
        return mRootView == null ? 0 : mRootView.getHeight();
    }


    public boolean saveEdit() {
        InputMethodUtils.hideKeyboard(getActivity());
        String title = mTitle.getText().toString();
        String url = mAddress.getText().toString();
        if (mNavigationEditable.addNewNavigation(title, url, true)) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QLMORE_EVENTS, AnalyticsSettings.ID_ADD);
            finishEdit();
            return true;
        }
        return false;
    }

    public void onKeyBoardShow() {
        //do nothing
    }

    public void onKeyBoardHint() {
        //do nothing
    }

    @Override
    public void onScreenRotate(boolean isPortrait) {
        if (!isPortrait) {
            if (checkEditTextNull()) return;
            mOk.setVisibility(View.VISIBLE);
            mCancel.setVisibility(View.VISIBLE);
        }
    }

    public void onNavListFullNotify(boolean isFull) {
        if (mIsNavListFull == isFull) return;
        this.mIsNavListFull = isFull;
        if (mMaskView != null) {
            mMaskView.setVisibility(isFull ? View.VISIBLE : View.GONE);
        }
    }

    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
        this.mOnFocusChangeListener = onFocusChangeListener;
    }

    public interface OnFocusChangeListener {
        void onFocusChange();
    }

}
