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

import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.OtherAPPUtils;

public class DownloadPreferencesFragment extends BasePreferenceFragment implements Preference
        .OnPreferenceClickListener, onFragmentCallBack {
    private Preference mAdmPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.download_preferences);

        Preference preference = findPreference(PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH);
        preference.setOnPreferenceClickListener(this);
        setDownloadPath(preference, BrowserSettings.getInstance().getDownloadPath());

        mAdmPreference = findPreference(PreferenceKeys.PREF_DOWNLOAD_ADM);
    }


    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_download).toString());
        if (!OtherAPPUtils.isAppInstalled(getActivity(), OtherAPPUtils.ADM_APP) && !OtherAPPUtils.isAppInstalled
                (getActivity(), OtherAPPUtils.ADM_APP_PRO)) {
            getPreferenceScreen().removePreference(mAdmPreference);
        } else {
            getPreferenceScreen().addPreference(mAdmPreference);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Fragment newFragment;
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH:
                BrowserFilePickerFragment filePickerFragment = new BrowserFilePickerFragment();
                Bundle bundle = new Bundle();
                bundle.putCharSequence(RadioFragment.KEY, preference.getKey());
                filePickerFragment.setArguments(bundle);
                filePickerFragment.setOnFragmentCallBack(this);
                startFragment(filePickerFragment);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event
                        .DOWNLOAD_EVENTS, AnalyticsSettings.ID_CUSTOMDOWNLOADPATH);
                break;
        }
        return false;
    }

    @Override
    public void onFragmentCallBack(String key, Object object) {
        String value;
        switch (key) {
            case PreferenceKeys.PREF_CUSTOM_DOWNLOAD_PATH:
                value = (String) object;
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(key)) {
                    setDownloadPath(findPreference(key), value);
                }
                break;
        }
    }

    public void setDownloadPath(Preference preference, String downloadPath) {
        preference.setSummary(downloadPath);
    }
}
