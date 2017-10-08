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
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

import java.text.NumberFormat;
import java.util.Map;

public class FontSizeFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final int PREF_TEXT_ZOOM_MAX = 30;// value is the maximum value of the TEXT_ZOOM
    private static final int PREF_MIN_FONT_SIZE_MAX = 20;// value is the default maximum value of MIN_FONT_SIZE

    private NumberFormat mFormat;
    private WebView mWebView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = super.onCreateView(inflater, container, bundle);
        mList.setBackgroundResource(R.color.settings_background);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.text_size_preferences);
        initData();
    }

    private void initData() {

        setBrowserActionBarTitle(getText(R.string.pref_font_size).toString());
        mWebView = new WebView(getActivity());
        BrowserSettings settings = BrowserSettings.getInstance();
        mFormat = NumberFormat.getPercentInstance();

        SeekBarSummaryPreference mPreference = (SeekBarSummaryPreference) findPreference(PreferenceKeys.PREF_TEXT_ZOOM);
        mPreference.setMax(PREF_TEXT_ZOOM_MAX);
        mPreference.setOnPreferenceChangeListener(this);
        updateTextZoomSummary(mPreference, settings.getTextZoom());

        mPreference = (SeekBarSummaryPreference) findPreference(PreferenceKeys.PREF_MIN_FONT_SIZE);
        mPreference.setMax(PREF_MIN_FONT_SIZE_MAX);
        mPreference.setOnPreferenceChangeListener(this);
        updateMinFontSummary(mPreference, settings.getMinimumFontSize());
    }

    private void updateMinFontSummary(Preference pref, int minFontSize) {
        pref.setSummary(getActivity().getString(R.string.pref_min_font_size_value, minFontSize));
    }

    private void updateTextZoomSummary(Preference pref, int textZoom) {
        pref.setSummary(mFormat.format(textZoom / 100.0));
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.resumeTimers();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.pauseTimers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
        mWebView = null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (getActivity() == null) {
            // We aren't attached, so don't accept preferences changes from the
            // invisible UI.
            return false;
        }

        Map<String, String> params = new ArrayMap<>();
        String value = null;
        if (PreferenceKeys.PREF_MIN_FONT_SIZE.equals(preference.getKey())) {
            value = BrowserSettings.getAdjustedMinimumFontSize((Integer) objValue) + "PT";
            updateMinFontSummary(preference, BrowserSettings
                    .getAdjustedMinimumFontSize((Integer) objValue));

            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.TEXTSIZE_EVENTS, AnalyticsSettings.ID_PT, value);
        }
        if (PreferenceKeys.PREF_TEXT_ZOOM.equals(preference.getKey())) {
            BrowserSettings settings = BrowserSettings.getInstance();
            value = settings.getAdjustedTextZoom((Integer) objValue) + "%";
            updateTextZoomSummary(preference, settings
                    .getAdjustedTextZoom((Integer) objValue));

            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.TEXTSIZE_EVENTS, AnalyticsSettings.ID_PERCENTAGE,
                    value);
        }

        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, preference.getKey(), value);

        return true;
    }
}
