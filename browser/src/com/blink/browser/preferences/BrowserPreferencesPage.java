/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * limitations under the License.
 */

package com.blink.browser.preferences;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.DefaultBrowserSetUtils;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.InputMethodUtils;
import com.blink.browser.util.SystemTintBarUtils;

public class BrowserPreferencesPage extends Activity implements View.OnClickListener {

    public static String PRIVACY_SETTING_FRAGMENT = "SettingsPreferencesFragment";
    private String mPreferenceKey;
    private ImageView mRightActionbarIcon;
    private ImageView mSecondRightActionbarIcon;
    private OnRightIconClickListener mOnRightIconClickListener;

    public static void startPreferencesForResult(Activity callerActivity, int requestCode) {
        final Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        startBrowserActivity(callerActivity, intent, requestCode);
    }

    /**
     * add by simon liu
     */
    public static void startPreferencesToSetDefaultBrowserForResult(Activity callerActivity, int requestCode) {
        Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(DefaultBrowserSetUtils.KEY_DEFAULT_BROWSER_SETTING, true);
        startBrowserActivity(callerActivity, intent, requestCode);
    }

    public static void startPreferenceFragment(Activity callerActivity, String fragmentName) {
        Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        callerActivity.startActivity(intent);
    }

    public static void startPreferenceFragmentForResult(Activity callerActivity, String fragmentName, int requestCode) {
        final Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        startBrowserActivity(callerActivity, intent, requestCode);
    }

    public static void startPreferenceFragmentExtraForResult(Activity callerActivity,
                                                             String fragmentName,
                                                             Bundle bundle,
                                                             int requestCode) {
        final Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle);
        startBrowserActivity(callerActivity, intent, requestCode);
    }

    private static void startBrowserActivity(Activity callerActivity, Intent intent, int requestCode) {
        callerActivity.startActivityForResult(intent, requestCode);
        callerActivity.overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setBrowserActionBar();
        setContentView(R.layout.activity_browser_preferences_page);
        SystemTintBarUtils.setSystemBarColor(this);
        DisplayUtil.changeScreenBrightnessIfNightMode(this);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String fragmentName = (String) extras.getCharSequence(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
                if (!TextUtils.isEmpty(fragmentName)) {
                    try {
                        Class<?> fragmentClass = Class.forName(fragmentName);
                        Object fragment = fragmentClass.newInstance();
                        if (fragment != null && fragment instanceof Fragment) {
                            startFragment(R.id.content_layout, ((Fragment) fragment));
                            return;
                        }
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        startFragment(R.id.content_layout, new SettingsPreferencesFragment());
    }

    private void startFragment(int rid, Fragment fragment) {
        getFragmentManager().beginTransaction().replace(rid, fragment).commitAllowingStateLoss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.content_layout);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void setBrowserActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.layout_custom_actionbar);
            View actionbarView = actionBar.getCustomView();
            actionbarView.findViewById(R.id.actionbar_left).setOnClickListener(this);
            mRightActionbarIcon = (ImageView) actionbarView.findViewById(R.id.actionbar_right_icon);
            mRightActionbarIcon.setOnClickListener(this);
            mSecondRightActionbarIcon = (ImageView) actionbarView.findViewById(R.id.actionbar_second_right_icon);
            mSecondRightActionbarIcon.setOnClickListener(this);
        }
    }

    public void disableRightIcons() {
        mRightActionbarIcon.setVisibility(View.GONE);
        mSecondRightActionbarIcon.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.actionbar_left:
                back();
                break;
            case R.id.actionbar_right_icon:
                if (mOnRightIconClickListener != null) {
                    mOnRightIconClickListener.onRightIconClick(v);
                }
                break;
            case R.id.actionbar_second_right_icon:
                if (mOnRightIconClickListener != null) {
                    mOnRightIconClickListener.onSecondRightIconClick(v);
                }
                break;
        }
    }

    public void back() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            InputMethodUtils.hideKeyboard(this);
            getFragmentManager().popBackStack();
            disableRightIcons();
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
    }

    @Override
    public void onBackPressed() {
        back();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        BrowserAnalytics.onResume(this);
        if (!BrowserSettings.getInstance().getShowStatusBar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BrowserAnalytics.onPause(this);
    }

    public void setRightIcon(@DrawableRes int iconRes) {
        mRightActionbarIcon.setImageResource(iconRes);
        mRightActionbarIcon.setVisibility(View.VISIBLE);
    }

    public void setSecondRightIcon(@DrawableRes int iconRes) {
        mSecondRightActionbarIcon.setImageResource(iconRes);
        mSecondRightActionbarIcon.setVisibility(View.VISIBLE);
    }

    public void setOnRightIconClickListener(OnRightIconClickListener onRightIconClickListener) {
        this.mOnRightIconClickListener = onRightIconClickListener;
    }

    public void setRightIconEnable(boolean enable) {
        mRightActionbarIcon.setEnabled(enable);
    }

    public void setSecondRightIconEnable(boolean enable) {
        mSecondRightActionbarIcon.setEnabled(enable);
    }

    public ImageView getRightActionbarIcon() {
        return mRightActionbarIcon;
    }

    public interface OnRightIconClickListener {
        void onRightIconClick(View v);

        void onSecondRightIconClick(View v);
    }

}
