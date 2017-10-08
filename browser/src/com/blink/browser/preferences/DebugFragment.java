package com.blink.browser.preferences;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.webkit.WebView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.ChannelUtil;

public class DebugFragment extends BasePreferenceFragment implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.debug_preferences);
        initData();
    }

    private void initData() {

        Preference preference = findPreference(PreferenceKeys.PREF_OPEN_DEBUG);
        preference.setOnPreferenceChangeListener(this);
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BrowserSettings.getInstance().getOpenDebugStatus());
        }

        preference = findPreference(PreferenceKeys.PREF_CHANNEL);
        ((BrowserPreference) preference).setSelectValue(ChannelUtil.getChannelId());

        preference = findPreference(PreferenceKeys.PREF_NEW_FEATURES);
        getPreferenceScreen().removePreference(preference);
        preference.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_deverloper_options).toString());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object values) {
        boolean isOpen = (boolean) values;
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_OPEN_DEBUG:
                if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(isOpen);
                }
                break;
            case PreferenceKeys.PREF_NEW_FEATURES:
                break;
        }
        return true;
    }
}
