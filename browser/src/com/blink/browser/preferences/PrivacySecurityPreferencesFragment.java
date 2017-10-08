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

import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;

public class PrivacySecurityPreferencesFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_security_preferences);
    }


    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_privacy_security_title).toString());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Fragment newFragment;
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_WEBSITE_SETTINGS:
                newFragment = new WebsiteSettingsFragment();
                startFragment(newFragment);
                break;
        }
        return false;
    }

}
