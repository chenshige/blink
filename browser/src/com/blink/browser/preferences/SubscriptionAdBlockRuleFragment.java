// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.preferences;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.adblock.AdBlockDataUpdator;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.SharedPreferencesUtils;
import com.blink.browser.util.ThreadUtils;

/**
 * 订阅广告过滤规则
 */

@SuppressLint("ValidFragment")
public class SubscriptionAdBlockRuleFragment extends BasePreferenceFragment implements
        View.OnClickListener {

    private LinearLayout mView;
    private TextView mAdBlockUpdateText;
    private TextView mEasyListUpdateText;
    private boolean mAdBlockUpdating;
    private boolean mEasyListUpdating;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        mView = new LinearLayout(getActivity());
        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setBrowserActionBarTitle(getString(R.string.subscribe_adblock_rule_title));
        initView(mView);
        mAdBlockUpdating = false;
        mEasyListUpdating = false;
        ThreadUtils.postOnUiThread(new UpdateRunnable());
        return mView;
    }
    private void initView(LinearLayout view) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.subscription_adblock_rules, null);
        v.findViewById(R.id.adblockplus_item).setOnClickListener(this);
        v.findViewById(R.id.easylist_item).setOnClickListener(this);
        v.findViewById(R.id.adblock_update_now).setOnClickListener(this);
        mAdBlockUpdateText = (TextView)v.findViewById(R.id.adblock_rule_update);

        boolean adblockUpdateEnable = BrowserSettings.getInstance().getAdBlockUpdateEnabled();
        v.findViewById(R.id.adblock_rule_check_box).setSelected(adblockUpdateEnable);
        if (!adblockUpdateEnable) {
            mAdBlockUpdateText.setText("");
        }
        boolean easyListUpdateEnable = BrowserSettings.getInstance().getEasyListUpdateEnabled();
        v.findViewById(R.id.easylist_rule_check_box).setSelected(easyListUpdateEnable);
        mEasyListUpdateText = (TextView)v.findViewById(R.id.easylist_rule_update);
        if (!easyListUpdateEnable) {
            mEasyListUpdateText.setText("");
        }
        view.addView(v);
    }
    @Override
    public void onClick(View v) {
        if (!BrowserSettings.getInstance().getAdBlockEnabled()) return;
        boolean enabled = false;
        switch(v.getId()) {
            case R.id.adblockplus_item:
                enabled = BrowserSettings.getInstance().getAdBlockUpdateEnabled();
                v.findViewById(R.id.adblock_rule_check_box).setSelected(!enabled);
                BrowserSettings.getInstance().setAdBlockUpdateEnabled(!enabled);
                if (enabled) {
                    mAdBlockUpdateText.setText(getActivity().getString(R.string.subscribe_update_close));
                } else {
                    mAdBlockUpdateText.setText(getActivity().getString(R.string.subscribe_updating));
                    AdBlockDataUpdator.getInstance().updateServices(
                            BrowserContract.ADBLOCK_UPDATE_URL,
                            SharedPreferencesUtils.ADBLOCK_UPDATE_TIMESTAMP,
                            100L);
                }
                break;
            case R.id.easylist_item:
                enabled = BrowserSettings.getInstance().getEasyListUpdateEnabled();
                v.findViewById(R.id.easylist_rule_check_box).setSelected(!enabled);
                BrowserSettings.getInstance().setEasyListUpdateEnabled(!enabled);
                if (enabled) {
                    mEasyListUpdateText.setText(getActivity().getString(R.string.subscribe_update_close));
                } else {
                    mEasyListUpdateText.setText(getActivity().getString(R.string.subscribe_updating));
                    AdBlockDataUpdator.getInstance().updateServices(
                            BrowserContract.EASYLIST_UPDATE_URL,
                            SharedPreferencesUtils.EASYLIST_UPDATE_TIMESTAMP,
                            100L);
                }
                break;
            case R.id.adblock_update_now:
                if (BrowserSettings.getInstance().getAdBlockUpdateEnabled()){
                    mAdBlockUpdateText.setText(getActivity().getString(R.string.subscribe_updating));
                    SharedPreferencesUtils.put(SharedPreferencesUtils.ADBLOCK_UPDATE_TIMESTAMP, 0L);
                    AdBlockDataUpdator.getInstance().updateServices(
                            BrowserContract.ADBLOCK_UPDATE_URL,
                            SharedPreferencesUtils.ADBLOCK_UPDATE_TIMESTAMP,
                            100L);
                }

                if (BrowserSettings.getInstance().getEasyListUpdateEnabled()) {
                    mEasyListUpdateText.setText(getActivity().getString(R.string.subscribe_updating));
                    SharedPreferencesUtils.put(SharedPreferencesUtils.EASYLIST_UPDATE_TIMESTAMP, 0L);
                    AdBlockDataUpdator.getInstance().updateServices(
                            BrowserContract.EASYLIST_UPDATE_URL,
                            SharedPreferencesUtils.EASYLIST_UPDATE_TIMESTAMP,
                            100L);
                }
                break;
        }
    }

    private class UpdateRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (this) {
                if (BrowserSettings.getInstance().getAdBlockUpdateEnabled() && mAdBlockUpdateText != null){
                    long lastUpdate = (long)SharedPreferencesUtils.get(SharedPreferencesUtils.ADBLOCK_UPDATE_TIMESTAMP, 0L);
                    if (lastUpdate == 0L) {
                        mAdBlockUpdateText.setText(mView.getContext().getString(R.string.subscribe_updating));
                    } else {
                        mAdBlockUpdateText.setText(
                                mView.getContext().getString(R.string.subscribe_update_open,
                                        DateUtils.getRelativeTimeSpanString(lastUpdate, System.currentTimeMillis(), 0)));
                    }

                }

                if (BrowserSettings.getInstance().getEasyListUpdateEnabled() && mEasyListUpdateText != null) {
                    long lastUpdate = (long)SharedPreferencesUtils.get(SharedPreferencesUtils.EASYLIST_UPDATE_TIMESTAMP, 0L);
                    if (lastUpdate == 0L) {
                        mEasyListUpdateText.setText(mView.getContext().getString(R.string.subscribe_updating));
                    } else {
                        mEasyListUpdateText.setText(
                                mView.getContext().getString(R.string.subscribe_update_open,
                                        DateUtils.getRelativeTimeSpanString(lastUpdate, System.currentTimeMillis(), 0)));
                    }

                }
                ThreadUtils.postOnUiThreadDelayed(this, 1000L);
            }
        }

    }
}
