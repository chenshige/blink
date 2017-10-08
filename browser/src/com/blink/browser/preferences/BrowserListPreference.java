package com.blink.browser.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.widget.BrowserRadioDialog;

/**
 * radio ListPreference
 */
public class BrowserListPreference extends ListPreference {

    private BrowserRadioDialog mDialog;
    private int mVisibility;
    private String mSelectValue;

    public BrowserListPreference(Context context) {
        this(context, null);
    }

    public BrowserListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ImageView imageView = (ImageView) view.findViewById(R.id.divider);
        if (imageView != null) {
            imageView.setVisibility(mVisibility);
        }

        TextView select = (TextView) view.findViewById(R.id.select_value);
        if (select != null && !TextUtils.isEmpty(mSelectValue)) {
            select.setText(mSelectValue);
        }

        return view;
    }

    public String getSelectValue() {
        return mSelectValue;
    }

    public void setSelectValue(String selectValue) {
        this.mSelectValue = selectValue;
    }

    public void setDeviderVisibility(int visibility) {
        this.mVisibility = visibility;
    }

    @Override
    protected void showDialog(Bundle state) {

        mDialog = new BrowserRadioDialog(getContext(), getEntries(), findIndexOfValue(getValue())) {
            @Override
            public void dialogDismiss(int selected) {
                if (getOnPreferenceChangeListener() != null) {
                    getOnPreferenceChangeListener().onPreferenceChange(BrowserListPreference.this, getEntryValues()[selected]);
                }
                setValue(getEntryValues()[selected].toString());

            }
        };

        if (getKey().equals(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING)) {
            // 选择语言　选项太多，固定dialog高度
            mDialog.setHeight(DisplayUtil.getScreenHeight(getContext()) * 3 / 5);
        }
        mDialog.setBrowserTitle(getDialogTitle().toString()).show();
    }

    public void onPause() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }
        mDialog.dismiss();
    }

}
