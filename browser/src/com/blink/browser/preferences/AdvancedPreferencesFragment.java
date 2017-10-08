/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.blink.browser.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

public class AdvancedPreferencesFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, onFragmentCallBack {

    private BrowserSettings mBrowserSettings;
    private String[] userAgent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.advanced_preferences);
        initData();
    }

    private void initData() {
        mBrowserSettings = BrowserSettings.getInstance();
        String value;

        Preference preference = findPreference(
                PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH);
        ((BrowserPreference) preference).setDeviderVisibility(View.GONE);
        preference.setOnPreferenceClickListener(this);
        setDownloadPath(preference, mBrowserSettings.getDownloadPath());

        preference = findPreference(
                PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING);
        value = mBrowserSettings.getDefaultTextEncoding();
        ((BrowserPreference) preference).setSelectValue(value);
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(
                PreferenceKeys.PREF_USER_AGENT);
        preference.setPersistent(false);
        userAgent = getResources().getStringArray(R.array.pref_user_agent_choices);
        value = userAgent[mBrowserSettings.getUserAgent()];
        ((BrowserPreference) preference).setSelectValue(value);
        preference.setOnPreferenceClickListener(this);
        preference = findPreference(PreferenceKeys.PREF_NOTIFICATION_TOOL_SHOW);
        ((BrowserSwitchPreference) preference).setChecked(mBrowserSettings.getNotificationToolShow());
        preference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {

            case PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING:
                preference.setSummary((String) newValue);
                break;
            case PreferenceKeys.PREF_USER_AGENT:
                preference.setSummary(((BrowserListPreference) preference).getEntries()[Integer.parseInt((String)
                        newValue)]);
                break;
            case PreferenceKeys.PREF_NOTIFICATION_TOOL_SHOW:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_NOTIFICATIONSEARCH, (boolean) newValue ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                if ((boolean) newValue) {
                    mBrowserSettings.showNotification();
                } else {
                    mBrowserSettings.hideNotification();
                }
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_extras_title).toString());
        //TODO:暂时去掉
//        final Preference websiteSettings = findPreference(
//                PreferenceKeys.PREF_WEBSITE_SETTINGS);
//        websiteSettings.setEnabled(false);
//        WebStorage.getInstance().getOrigins(new ValueCallback<Map>() {
//            @Override
//            public void onReceiveValue(Map webStorageOrigins) {
//                if ((webStorageOrigins != null) && !webStorageOrigins.isEmpty()) {
//                    websiteSettings.setEnabled(true);
//                }
//            }
//        });
//        GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() {
//            @Override
//            public void onReceiveValue(Set<String> geolocationOrigins) {
//                if ((geolocationOrigins != null) && !geolocationOrigins.isEmpty()) {
//                    websiteSettings.setEnabled(true);
//                }
//            }
//        });
    }

    public void setDownloadPath(Preference preference, String downloadPath) {
        preference.setSummary(downloadPath);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH:
                BrowserFilePickerFragment filePickerFragment = new BrowserFilePickerFragment();
                Bundle args = new Bundle();
                args.putCharSequence(RadioFragment.KEY, preference.getKey());
                filePickerFragment.setArguments(args);
                filePickerFragment.setOnFragmentCallBack(this);
                startFragment(filePickerFragment);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event
                        .ADVANCED_EVENTS, AnalyticsSettings.ID_CUSTOMDOWNLOADPATH);
                break;
            case PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING:
                RadioFragment radioFragment = new RadioFragment();
                Bundle bundle = new Bundle();
                bundle.putCharSequence(RadioFragment.KEY, preference.getKey());
                radioFragment.setArguments(bundle);
                radioFragment.setOnFragmentCallBack(this);
                startFragment(radioFragment);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event
                        .ADVANCED_EVENTS, AnalyticsSettings.ID_TEXTCODING);
                break;
            case PreferenceKeys.PREF_USER_AGENT:
                RadioFragment radioFragment1 = new RadioFragment();
                Bundle bundle1 = new Bundle();
                bundle1.putCharSequence(RadioFragment.KEY, preference.getKey());
                radioFragment1.setArguments(bundle1);
                radioFragment1.setOnFragmentCallBack(this);
                startFragment(radioFragment1);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event
                        .ADVANCED_EVENTS, AnalyticsSettings.ID_USERAGENT);
                break;
        }
        return false;
    }

    @Override
    public void onFragmentCallBack(String key, Object object) {
        String value = (String) object;
        switch (key) {
            case PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH:
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(key)) {
                    setDownloadPath(findPreference(key), value);
                }
                break;
            case PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING:
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(key)) {
                    ((BrowserPreference) findPreference(key)).setSelectValue(value);
                    BrowserSettings.getInstance().setDefaultTextEncoding(value);
                }
                break;
            case PreferenceKeys.PREF_USER_AGENT:
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(key)) {
                    ((BrowserPreference) findPreference(key)).setSelectValue(userAgent[Integer.parseInt(value)]);
                    mBrowserSettings.setUserAgent(value);
                }
                break;
        }
    }
}
