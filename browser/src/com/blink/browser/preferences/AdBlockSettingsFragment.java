// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.widget.BrowserSwitchButton;

public class AdBlockSettingsFragment extends BasePreferenceFragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private BrowserSettings mSettings;

    private BrowserSwitchButton mSwitchButton;
    private TextView mImgAdBlockCount;
    private TextView mJsAdBlockCount;
    private TextView mPopupAdBlockCount;
    private TextView mClear;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_adblock_settings, container, false);
        setBrowserActionBarTitle(getString(R.string.pref_ad_block));
        mSettings = BrowserSettings.getInstance();
        init(view);
        return view;
    }

    private void init(View rootView) {
        mSwitchButton = (BrowserSwitchButton) rootView.findViewById(R.id.switch_button);
        mImgAdBlockCount = (TextView) rootView.findViewById(R.id.pic_ad_block_count_text);
        mJsAdBlockCount = (TextView) rootView.findViewById(R.id.js_ad_block_count_text);
        mPopupAdBlockCount = (TextView) rootView.findViewById(R.id.popup_ad_block_count_text);
        mClear = (TextView) rootView.findViewById(R.id.clear);

        mSwitchButton.setFocusable(false);
        mSwitchButton.setChecked(mSettings.getAdBlockEnabled());
        mSwitchButton.setOnCheckedChangeListener(this);

        refreshAdBlockCount();

        mClear.setOnClickListener(this);
        rootView.findViewById(R.id.subscription_adblock_rules_layout).setOnClickListener(this);
        rootView.findViewById(R.id.edit_rules_layout).setOnClickListener(this);
        rootView.findViewById(R.id.set_js_disable_layout).setOnClickListener(this);

    }


    private void refreshAdBlockCount() {
        mImgAdBlockCount.setText(getString(R.string.ad_block_num, mSettings.getImgAdBlockCount()));
        mJsAdBlockCount.setText(getString(R.string.ad_block_num, mSettings.getJsAdBlockCount()));
        mPopupAdBlockCount.setText(getString(R.string.ad_block_num, mSettings.getPopupAdBlockCount()));
        mClear.setEnabled(mSettings.getAdBlockCount() > 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear:
                mSettings.clearAdBlockCount();
                refreshAdBlockCount();
                break;
            case R.id.subscription_adblock_rules_layout:
                SubscriptionAdBlockRuleFragment subscriptionAdBlockRuleFragment = new SubscriptionAdBlockRuleFragment();
                startFragment(subscriptionAdBlockRuleFragment);
                break;
            case R.id.edit_rules_layout:
                EditAdBlockRuleFragment editAdBlockRuleFragment = new EditAdBlockRuleFragment();
                startFragment(editAdBlockRuleFragment);
                break;
            case R.id.set_js_disable_layout:
                EditJsDisableHostsFragment editJsDisableHostsFragment = new EditJsDisableHostsFragment();
                startFragment(editJsDisableHostsFragment);
                break;
            default:
                //do nothing
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSettings.setAdBlockEnabled(isChecked);
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                .ID_ADBLOCK);

        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADBLOCK_EVENTS, isChecked ?
                AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
    }
}
