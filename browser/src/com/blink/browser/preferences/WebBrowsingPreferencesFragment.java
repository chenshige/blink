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
import android.support.v4.util.ArrayMap;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

import java.util.Map;

public class WebBrowsingPreferencesFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private BrowserSettings mBrowserSettings;
    private BrowserListPreference mUserAgentListPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.webbrowsing_preferences);
        initData();
    }

    private void initData() {
        mBrowserSettings = BrowserSettings.getInstance();

        Preference preference = findPreference(
                PreferenceKeys.PREF_TEXT_SIZE);
        preference.setOnPreferenceClickListener(this);

        mUserAgentListPreference = (BrowserListPreference) findPreference(
                PreferenceKeys.PREF_USER_AGENT);
        mUserAgentListPreference.setSummary(mUserAgentListPreference.getEntries()[mBrowserSettings.getUserAgent()]);
        mUserAgentListPreference.setValue(mBrowserSettings.getUserAgent() + "");
        mUserAgentListPreference.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        mUserAgentListPreference.onPause();
        super.onPause();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Fragment newFragment;
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_TEXT_SIZE:
                newFragment = new FontSizeFragment();
                startFragment(newFragment);
                break;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_USER_AGENT:
                preference.setSummary(((BrowserListPreference) preference).getEntries()[Integer.parseInt((String)
                        newValue)]);
                break;
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_web_browsing).toString());
    }
}
