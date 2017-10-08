package com.blink.browser.preferences;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.BrowserDownloadManager;
import com.blink.browser.util.ActivityUtils;
import com.blink.browser.util.DeviceInfoUtils;
import com.blink.browser.util.SharedPreferencesUtils;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.view.LoadingDialog;

import java.lang.ref.WeakReference;

public class AboutFragment extends BasePreferenceFragment implements View.OnClickListener {
    private static final int CLICK_COUNT = 4;
    private static final long CLICK_INTERVAL_MAX_TIME = 3000;

    private static boolean mShowUpdate = false;
    private MyHandler mHandler;
    private static LoadingDialog mLoading;
    private int mChannelCount = 0;
    private long mPreTime = 0;
    private LinearLayout mView;
    private int mOrientation;
    private onFragmentCallBack mCallBack;
    private String mKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        mView = new LinearLayout(getActivity());
        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setBrowserActionBarTitle(getString(R.string.pref_about));
        initView(mView);
        mOrientation = getActivity().getResources().getConfiguration().orientation;
        return mView;
    }

    private void initView(LinearLayout view) {
        Bundle arguments = getArguments();
        mKey = arguments.getString(KEY);

        int about_layout = /*ChannelUtil.isUpdateEnabled() ? R.layout.about_fragment : */R.layout.about_fragment_no_update;
        View v = LayoutInflater.from(getActivity()).inflate(about_layout, null);
        ((TextView) v.findViewById(R.id.version)).setText(getString(R.string.pref_version_summary, DeviceInfoUtils
                .getAppVersionName(getActivity())));
        v.findViewById(R.id.icon).setOnClickListener(this);
        v.findViewById(R.id.update).setOnClickListener(this);
        v.findViewById(R.id.contact_google).setOnClickListener(this);
        v.findViewById(R.id.contact_twitter).setOnClickListener(this);
        v.findViewById(R.id.contact_facebook).setOnClickListener(this);
        v.findViewById(R.id.contact_instagram).setOnClickListener(this);
        v.findViewById(R.id.agreement).setOnClickListener(this);
        v.findViewById(R.id.privacy_policy).setOnClickListener(this);
        view.addView(v);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != mOrientation) {
            mView.removeAllViews();
            mOrientation = newConfig.orientation;
            initView(mView);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.icon:
                if (mPreTime == 0) {
                    mPreTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - mPreTime > CLICK_INTERVAL_MAX_TIME) {
                    mChannelCount = 0;
                    mPreTime = System.currentTimeMillis();
                }
                mChannelCount++;
                if (mChannelCount > CLICK_COUNT) {
                    boolean isOpen = (boolean) SharedPreferencesUtils.get(SharedPreferencesUtils.OPEN_DEVELOPER_OPTIONS, false);
                    if (!isOpen) {
                        ToastUtil.showLongToastByString(getActivity(), getResources().getString(R.string.show_dev_on));
                        if (mCallBack != null) {
                            mCallBack.onFragmentCallBack(mKey, true);
                        }
                        SharedPreferencesUtils.put(SharedPreferencesUtils.OPEN_DEVELOPER_OPTIONS, true);
                    }
                    mChannelCount = 0;
                }
                break;
            case R.id.update:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ABOUT_EVENTS, AnalyticsSettings
                        .ID_UPDATE);
                break;
            case R.id.contact_google:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ABOUT_EVENTS, AnalyticsSettings
                        .ID_GOOGLE);
                openUrl(getString(R.string.contact_google));
                break;
            case R.id.contact_twitter:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ABOUT_EVENTS, AnalyticsSettings
                        .ID_TWITTER);
                openUrl(getString(R.string.contact_twitter));
                break;
            case R.id.contact_facebook:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ABOUT_EVENTS, AnalyticsSettings
                        .ID_FACEBOOK);
                openUrl(getString(R.string.contact_facebook));
                break;
            case R.id.contact_instagram:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ABOUT_EVENTS, AnalyticsSettings
                        .ID_INSTAGRAM);
                openUrl(getString(R.string.contact_instagram));
                break;
            case R.id.agreement:
                openUrl(getString(R.string.pref_agreement_url));
                break;
            case R.id.privacy_policy:
                openUrl(getString(R.string.pref_privacy_policy_url));
                break;
        }
    }

    public void setOnFragmentCallBack(onFragmentCallBack callBack) {
        mCallBack = callBack;
    }

    private void openUrl(String url) {
        ActivityUtils.openUrl(getActivity(), url);
    }

    /**
     * 检查更新
     */
    private void checkUpdate() {
        if (!mShowUpdate) {
            mHandler = new MyHandler(getActivity());
            mShowUpdate = true;
            mLoading = new LoadingDialog(getActivity());
            mLoading.setContext(R.string.check_for_update);
            mLoading.show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mShowUpdate = false;

        BrowserDownloadManager.getInstance().onDestroy();
    }

    static class MyHandler extends Handler {
        WeakReference<Activity> reference;

        MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final Activity activity = reference.get();
            if (activity != null) {
                mShowUpdate = false;
                mLoading.dismiss();
            }
        }
    }

}
