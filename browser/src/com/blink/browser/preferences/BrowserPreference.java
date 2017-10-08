package com.blink.browser.preferences;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.search.SearchEnginePreference;

/**
 * radio ListPreference
 */
public class BrowserPreference extends Preference {

    private int mVisibility;
    private String mSelectValue;
    private ImageView mSelectIcon;


    public BrowserPreference(Context context) {
        super(context);
    }

    public BrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BrowserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public String getSelectValue() {
        return mSelectValue;
    }

    public void setSelectValue(String selectValue) {
        if (!TextUtils.isEmpty(selectValue)) {
            mSelectValue = selectValue;
            if (mSelectIcon != null) {
                SearchEnginePreference.setSearchEngineIcon(getContext(), mSelectIcon, mSelectValue);
            }
        }
    }

    public void setDeviderVisibility(int visibility) {
        this.mVisibility = visibility;
    }

    protected void onBindView(View view) {
        super.onBindView(view);

        TextView select = (TextView) view.findViewById(R.id.select_value);
        if (select != null && !TextUtils.isEmpty(mSelectValue)) {
            select.setText(mSelectValue);
        }

        mSelectIcon = (ImageView) view.findViewById(R.id.select_icon);
        if (mSelectIcon != null) {
            SearchEnginePreference.setSearchEngineIcon(getContext(), mSelectIcon, mSelectValue);
        }

        ImageView imageView = (ImageView) view.findViewById(R.id.divider);
        if (imageView != null) {
            imageView.setVisibility(mVisibility);
        }

    }

}
