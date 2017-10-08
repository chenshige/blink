package com.blink.browser.preferences;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import com.blink.browser.BrowserSettings;
import com.blink.browser.PreferenceKeys;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.DefaultBrowserSetUtils;
import com.blink.browser.util.OtherAPPUtils;
import com.blink.browser.util.SharedPreferencesUtils;

import static com.blink.browser.PreferenceKeys.PREF_DOWNLOAD;

/**
 * config setting
 */
public class SettingsPreferencesFragment extends BasePreferenceFragment implements Preference
        .OnPreferenceClickListener, Preference.OnPreferenceChangeListener, onFragmentCallBack {

    private static final int NO_BROWSER_SET = 0;
    private static final int THIS_BROWSER_SET_AS_DEFAULT = 1;
    private static final int OTHER_BROWSER_SET_AS_DEFAULT = 2;

    private BrowserPreference mEnginePreference;
    private Preference mClearData;
    private BrowserDialogPreference mResetDefault;
    private BrowserSettings mSettings;
    private int mDefaultBrowserSetStatus = NO_BROWSER_SET;
    private String[] userAgent;
    private Preference mDeveloperOptionsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.settings_preferences);
        init();
        updateDefaultBrowserSettingStatus();
    }

    private void init() {
        mSettings = BrowserSettings.getInstance();

        mEnginePreference = (BrowserPreference) findPreference(PreferenceKeys.PREF_SEARCH_ENGINE);
        mEnginePreference.setSelectValue(mSettings.getSearchEngineName());
        mEnginePreference.setOnPreferenceClickListener(this);

        mClearData = findPreference(PreferenceKeys.PREF_CLEAR_DATA);
        mClearData.setOnPreferenceClickListener(this);

        Preference preference = findPreference(PreferenceKeys.PREF_AD_BLOCK);
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(PreferenceKeys.PREF_CONFIRM_ON_EXIT);
        preference.setOnPreferenceChangeListener(this);

        preference = findPreference(PreferenceKeys.PREF_TEXT_SIZE);
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(PREF_DOWNLOAD);
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(PreferenceKeys.PREF_USER_AGENT);
        preference.setPersistent(false);
        userAgent = getResources().getStringArray(R.array.pref_user_agent_choices);
        String value = userAgent[mSettings.getUserAgent()];
        ((BrowserPreference) preference).setSelectValue(value);
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING);
        ((BrowserPreference) preference).setDeviderVisibility(View.GONE);
        value = mSettings.getDefaultTextEncoding();
        ((BrowserPreference) preference).setSelectValue(value);
        preference.setOnPreferenceClickListener(this);

        //preference = findPreference(PreferenceKeys.PREF_SHOW_STATUS_BAR);
        //preference.setOnPreferenceChangeListener(this);

        preference = findPreference(PreferenceKeys.PREF_LOCK_TOOLBAR);
        preference.setOnPreferenceChangeListener(this);

        preference = findPreference(PreferenceKeys.PREF_NOTIFICATION_TOOL_SHOW);
        ((BrowserSwitchPreference) preference).setChecked(mSettings.getNotificationToolShow());
        preference.setOnPreferenceChangeListener(this);

        preference = findPreference(PreferenceKeys.PREF_PRIVACY_SECURITY);
        preference.setOnPreferenceClickListener(this);

//        preference = findPreference(PreferenceKeys.PREF_ADVANCED);
//        ((BrowserPreference) preference).setDeviderVisibility(View.GONE);
//        preference.setOnPreferenceClickListener(this);


        preference = findPreference(PreferenceKeys.PREF_DEFAULT_BROWSER);
        if (!DefaultBrowserSetUtils.canSetDefaultBrowser(getActivity())) {
            getPreferenceScreen().removePreference(preference);
        } else {
            preference.setOnPreferenceChangeListener(this);
        }

        preference = findPreference(PreferenceKeys.PREF_FORCE_USERSCALABLE);
        ((BrowserSwitchPreference) preference).setDeviderVisibility(View.GONE);

        preference = findPreference(PreferenceKeys.PREF_APP_NAME);
        ((BrowserPreference) preference).setDeviderVisibility(View.GONE);
        preference.setOnPreferenceClickListener(this);

        //preference = findPreference(PreferenceKeys.PREF_FEEDBACK);
        //preference.setOnPreferenceClickListener(this);

        mDeveloperOptionsPref = findPreference(PreferenceKeys.PREF_DEVERLOPER_OPTIONS);
        boolean isOpen = (boolean) SharedPreferencesUtils.get(SharedPreferencesUtils.OPEN_DEVELOPER_OPTIONS, false);
        if (!isOpen) {
            getPreferenceScreen().removePreference(mDeveloperOptionsPref);
        }
        mDeveloperOptionsPref.setOnPreferenceClickListener(this);

        //preference = findPreference(PreferenceKeys.PREF_APP_SCORE);
        //if (ChannelUtil.isGoogleChannel()) {
        //    preference.setOnPreferenceClickListener(this);
        //} else {
        //    getPreferenceScreen().removePreference(preference);
        //}

        mResetDefault = (BrowserDialogPreference) findPreference(
                PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES);
        mResetDefault.setOnPreferenceChangeListener(this);
        mResetDefault.setOnPreferenceClickListener(this);

    }

    private void updateDefaultBrowserSettingStatus() {
        Activity activity = getActivity();
        BrowserPreference preference = (BrowserPreference) findPreference(PreferenceKeys
                .PREF_DEFAULT_BROWSER);
        if (preference == null) {
            return;
        }
        ResolveInfo defaultBrowser = DefaultBrowserSetUtils.findDefaultBrowser(activity);
        String currentDefaultBrowserPkgName = defaultBrowser.activityInfo.packageName;
        if ("android".equals(currentDefaultBrowserPkgName)) {
            //this means no default browser set.
            mDefaultBrowserSetStatus = NO_BROWSER_SET;
            //preference.setChecked(false);
        } else if (TextUtils.equals(currentDefaultBrowserPkgName, activity.getPackageName())) {
            //this means my browser is the default browser.
            mDefaultBrowserSetStatus = THIS_BROWSER_SET_AS_DEFAULT;
            //preference.setChecked(true);
        } else {
            //this means one default browser set. we need clear it.
            mDefaultBrowserSetStatus = OTHER_BROWSER_SET_AS_DEFAULT;
            //preference.setChecked(false);
        }
    }

    @Override
    public void onPause() {
        mResetDefault.onPause();
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getActivity().getTitle().toString());
        updateDefaultBrowserSettingStatus();
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        Fragment newFragment;
        Bundle bundle;
        switch (preference.getKey()) {
            case PreferenceKeys.PREF_SEARCH_ENGINE:
                RadioFragment radioFragment = new RadioFragment();
                bundle = new Bundle();
                bundle.putCharSequence(RadioFragment.KEY, PreferenceKeys.PREF_SEARCH_ENGINE);
                radioFragment.setArguments(bundle);
                radioFragment.setOnFragmentCallBack(new onFragmentCallBack() {
                    @Override
                    public void onFragmentCallBack(String key, Object newValue) {
                        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SEARCHENGINESWITCH_EVENTS, AnalyticsSettings
                                .ID_STATUS, (String) newValue);
                        mEnginePreference.setSelectValue((String) newValue);
                        mSettings.getPreferences().edit().putString(PreferenceKeys.PREF_SEARCH_ENGINE, (String)
                                newValue)
                                .apply();
                    }
                });
                startFragment(radioFragment);
                break;
            case PreferenceKeys.PREF_APP_SCORE:
                appScore();
                break;
            case PreferenceKeys.PREF_TEXT_SIZE:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_TEXTSIZE);
                newFragment = new FontSizeFragment();
                startFragment(newFragment);
                break;
            case PreferenceKeys.PREF_DOWNLOAD:
                DownloadPreferencesFragment downloadPreferencesFragment = new DownloadPreferencesFragment();
                startFragment(downloadPreferencesFragment);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings.ID_DOWNLOAD);
                break;
            case PreferenceKeys.PREF_USER_AGENT:
                RadioFragment radioFragment1 = new RadioFragment();
                bundle = new Bundle();
                bundle.putCharSequence(RadioFragment.KEY, preference.getKey());
                radioFragment1.setArguments(bundle);
                radioFragment1.setOnFragmentCallBack(this);
                startFragment(radioFragment1);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event
                        .ADVANCED_EVENTS, AnalyticsSettings.ID_USERAGENT);
                break;
            case PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING:
                RadioFragment textEncodingFragment = new RadioFragment();
                bundle = new Bundle();
                bundle.putCharSequence(RadioFragment.KEY, preference.getKey());
                textEncodingFragment.setArguments(bundle);
                textEncodingFragment.setOnFragmentCallBack(this);
                startFragment(textEncodingFragment);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event
                        .ADVANCED_EVENTS, AnalyticsSettings.ID_TEXTCODING);
                break;
            case PreferenceKeys.PREF_PRIVACY_SECURITY:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_PRIVACYSECURITY);
                newFragment = new PrivacySecurityPreferencesFragment();
                startFragment(newFragment);
                break;
//            case PreferenceKeys.PREF_ADVANCED:
//                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
//                        .ID_ADVANCED);
//                newFragment = new AdvancedPreferencesFragment();
//                startFragment(newFragment);
//                break;
            case PreferenceKeys.PREF_DEVERLOPER_OPTIONS:
                newFragment = new DebugFragment();
                startFragment(newFragment);
                break;
            case PreferenceKeys.PREF_APP_NAME:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_ABOUT);
                AboutFragment aboutFragment = new AboutFragment();
                bundle = new Bundle();
                bundle.putCharSequence(AboutFragment.KEY, preference.getKey());
                aboutFragment.setArguments(bundle);
                aboutFragment.setOnFragmentCallBack(this);
                startFragment(aboutFragment);

                break;
            case PreferenceKeys.PREF_FEEDBACK:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_FEEDBACK);
                newFragment = new FeedbackFragment();
                startFragment(newFragment);
                break;
            case PreferenceKeys.PREF_CLEAR_DATA:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_CLEARDATA);
                newFragment = new ClearDataPreferencesFragment();
                startFragment(newFragment);
                break;
            case PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_RESETDEFAULT);
                break;
            case PreferenceKeys.PREF_AD_BLOCK:
                newFragment = new AdBlockSettingsFragment();
                startFragment(newFragment);
                break;
            case PreferenceKeys.PREF_DEFAULT_BROWSER:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_DEFAULTBROWSER);
                setDefaultBrowser();
                break;
        }

        return false;
    }

    /**
     * on click setDefaultBrowser option
     */
    private void setDefaultBrowser() {
        Activity activity = getActivity();
        switch (mDefaultBrowserSetStatus) {
            case NO_BROWSER_SET:
                startFragment(SetDefaultBrowserFragment.create());
                break;
            case THIS_BROWSER_SET_AS_DEFAULT:
            case OTHER_BROWSER_SET_AS_DEFAULT:
                ResolveInfo defaultBrowser = DefaultBrowserSetUtils.findDefaultBrowser(activity);
                startFragment(ClearDefaultBrowserFragment.create(defaultBrowser));
                break;
            default:
                //do nothing
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            /*case PreferenceKeys.PREF_AD_BLOCK:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_ADBLOCK);

                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADBLOCK_EVENTS, (boolean) newValue ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                return true;*/
            case PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES:
                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_TEXT, preference.getKey());
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
                break;
            /*case PreferenceKeys.PREF_DEFAULT_BROWSER:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_DEFAULTBROWSER);
                setDefaultBrowser();
                break;
            case PreferenceKeys.PREF_SHOW_STATUS_BAR:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_STATUSDISPLAYED);
                if (!(boolean) newValue) {
                    getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
                            .LayoutParams.FLAG_FULLSCREEN);
                } else {
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }

                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.STATUSDISPLAYED_EVENTS, (boolean) newValue ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                return true;*/
            case PreferenceKeys.PREF_NOTIFICATION_TOOL_SHOW:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.ADVANCED_EVENTS, AnalyticsSettings
                        .ID_NOTIFICATIONSEARCH, (boolean) newValue ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                if ((boolean) newValue) {
                    mSettings.showNotification();
                } else {
                    mSettings.hideNotification();
                }
                break;
            case PreferenceKeys.PREF_CONFIRM_ON_EXIT:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_CONFIRMEXIT);

                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CONFIRMEXIT_EVENTS, (boolean) newValue ?
                        AnalyticsSettings.ID_ON : AnalyticsSettings.ID_OFF);
                return true;
            case PreferenceKeys.PREF_LOCK_TOOLBAR:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SETTING_EVENT, AnalyticsSettings
                        .ID_LOCKTOOLBAR);
                mSettings.setLockToolbar((boolean) newValue);
                break;
        }
        return true;
    }

    public void setDownloadPath(Preference preference, String downloadPath) {
        preference.setSummary(downloadPath);
    }

    private void appScore() {
        String appName = getActivity().getPackageName();
        if (OtherAPPUtils.isAppInstalled(getActivity(), OtherAPPUtils.GOOGLE_PLAY)) {
            OtherAPPUtils.startGooglePlayAPP(getActivity(), appName);
        } else {
            OtherAPPUtils.startGooglePlayWeb(getActivity(), appName);
        }
    }

    @Override
    public void finish() {
        super.finish();
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
            case PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING:
                value = (String) object;
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(key)) {
                    ((BrowserPreference) findPreference(key)).setSelectValue(value);
                    BrowserSettings.getInstance().setDefaultTextEncoding(value);
                }
                break;
            case PreferenceKeys.PREF_USER_AGENT:
                value = (String) object;
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(key)) {
                    ((BrowserPreference) findPreference(key)).setSelectValue(userAgent[Integer.parseInt(value)]);
                    mSettings.setUserAgent(value);
                }
                break;
            case PreferenceKeys.PREF_APP_NAME:
                boolean isFlog = (boolean) object;
                if (isFlog && mDeveloperOptionsPref != null) {
                    getPreferenceScreen().addPreference(mDeveloperOptionsPref);
                }
                break;
        }
    }
}
