package com.blink.browser.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.blink.browser.R;
import com.blink.browser.widget.BrowserMultiselectDialog;

import java.util.Map;

public class BrowserMultiselectListPreference extends ListPreference {

    private BrowserMultiselectDialog mDialog;
    private Map<Integer, String> mSelected;
    private int mVisibility;

    public BrowserMultiselectListPreference(Context context) {
        this(context, null);
    }

    public BrowserMultiselectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ImageView imageView = (ImageView) view.findViewById(R.id.divider);
        if (imageView != null) {
            imageView.setVisibility(mVisibility);
        }
        return view;
    }

    public void setDeviderVisibility(int visibility) {
        this.mVisibility = visibility;
    }

    @Override
    protected void showDialog(Bundle state) {
        mDialog = new BrowserMultiselectDialog(getContext(), getEntries()) {
            @Override
            public void dialogDismiss(Map<Integer, String> selected) {
                if (getOnPreferenceChangeListener() != null) {
                    getOnPreferenceChangeListener().onPreferenceChange(BrowserMultiselectListPreference.this, selected);
                }
                mSelected.clear();
                mSelected.putAll(selected);
            }
        };
        mDialog.setBrowserTitle(getTitle().toString())
                .setBrowserNegativeButton(getNegativeButtonText().toString())
                .setBrowserPositiveButton(getPositiveButtonText().toString());

        if (mSelected != null && mSelected.size() > 0) {
            mDialog.setValue(mSelected);
        }

        mDialog.show();
    }

    public void setSelectValue(Map<Integer, String> selected) {
        if (selected != null && selected.size() > 0) {
            if (mSelected == null) {
                mSelected = new ArrayMap<>();
            }
            mSelected.clear();
            mSelected.putAll(selected);
        }
    }

    public void onPause() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }
        mDialog.dismiss();
    }
}
