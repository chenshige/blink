/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.blink.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.SystemTintBarUtils;
import com.blink.browser.widget.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

public class ComboViewActivity extends FragmentActivity implements CombinedBookmarksCallbacks, View.OnClickListener {
    private static final String STATE_SELECTED_TAB = "tab";
    public static final String EXTRA_COMBO_ARGS = "combo_args";
    public static final String EXTRA_INITIAL_VIEW = "initial_view";
    public static final String EXTRA_OPEN_SNAPSHOT = "snapshot_id";
    public static final String EXTRA_OPEN_ALL = "open_all";
    public static final String EXTRA_CURRENT_URL = "url";
    private static final int INDICATOR_TEXT_SIZE = 14;

    private PagerSlidingTabStrip mIndicator;
    private ViewPager mViewPager;
    private List<Fragment> mList;
    private TextView mActionBarTitle;
    private String[] mTitles;
    private ImageView mActionBarLeftIcon;
    private boolean mEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combo_view);
        initActionBar(getText(R.string.bookmark_list).toString());
        mList = new ArrayList<>();
        init();
        DisplayUtil.changeScreenBrightnessIfNightMode(this);
        SystemTintBarUtils.setSystemBarColor(this);
    }

    private void init() {
        mList.add(new BrowserBookmarksPage());
        mList.add(new BrowserHistoryPage());
        mTitles = new String[]{getResources().getString(R.string.bookmark_list), getResources().getString(R.string
                .history)};
        mIndicator = (PagerSlidingTabStrip) findViewById(R.id.tab_page_indicator_combo);
        mViewPager = (ViewPager) findViewById(R.id.viewpager_combo);
        BasePagerAdapter adapter = new BasePagerAdapter(getSupportFragmentManager(), mTitles);
        mViewPager.setAdapter(adapter);
        mIndicator.setOnPageChangeListener(mOnPageChangeListener);
        mIndicator.setViewPager(mViewPager);

        if (getIntent() != null && getIntent().getExtras() != null) {
            int menuid = getIntent().getExtras().getInt(BrowserContract.MENU_ID, R.id.bookmarks_history_button_id);
            if (menuid == R.id.new_bookmark_button_id) {
                mViewPager.setCurrentItem(1);
                ((BrowserHistoryPage) mList.get(1)).AppearFirst();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB,
                getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
    }

    @Override
    public void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public void openInNewTab(String... urls) {
        Intent i = new Intent();
        i.putExtra(EXTRA_OPEN_ALL, urls);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void openSnapshot(long id) {
        Intent i = new Intent();
        i.putExtra(EXTRA_OPEN_SNAPSHOT, id);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.combined, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (R.id.actionbar_left_icon == view.getId()) {
            if (mEdit) {
                removeActionBarEdit();
            } else {
                this.finish();
            }
        }
    }

    private void initActionBar(String title) {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.layout_custom_actionbar);
        View actionbarView = actionBar.getCustomView();
        mActionBarLeftIcon = (ImageView) actionbarView.findViewById(R.id.actionbar_left_icon);
        mActionBarLeftIcon.setOnClickListener(this);
        mActionBarTitle = (TextView) actionbarView.findViewById(R.id.actionbar_title);
        mActionBarTitle.setText(title);
        // SystemTintBarUtils.setSystemBarColor(this);
    }

    class BasePagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {
        String[] mTitles;
        int[] mIcon;
        int[] mIconSelect;

        public BasePagerAdapter(FragmentManager fm, String[] titles) {
            super(fm);
            this.mTitles = titles;
            mIcon = new int[]{R.drawable.ic_browser_tab_bookmark, R.drawable.ic_browser_tab_history};
            mIconSelect = new int[]{R.drawable.ic_browser_tab_bookmark_selected, R.drawable
                    .ic_browser_tab_history_selected};
        }

        @Override
        public Fragment getItem(int position) {
            return mList.get(position);
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        @Override
        public int getPageIconResId(int position) {
            return mIcon[position];
        }

        @Override
        public int getPageSelectIconResId(int position) {
            return mIconSelect[position];
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BrowserAnalytics.onResume(this);
        if (!BrowserSettings.getInstance().getShowStatusBar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams
                    .FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BrowserAnalytics.onPause(this);
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBarTitle.setText(mTitles[position]);

            String event_id = "";
            if (position == 1) {
                event_id = AnalyticsSettings.ID_HISTORY;
                removeActionBarEdit();
            } else if (position == 0) {
                event_id = AnalyticsSettings.ID_BOOKMARK;
                if (mList.get(1) instanceof BrowserHistoryPage) {
                    ((BrowserHistoryPage) mList.get(1)).removeClearButton();
                }
            }

            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SWITCH_EVENTS, event_id);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    public void setActionBarEdit(boolean edit) {
        mEdit = edit;
        if (edit) {
            mActionBarLeftIcon.setImageResource(R.drawable.ic_browser_home_close);
            mActionBarTitle.setText(getResources().getString(R.string.edit));
        } else {
            mActionBarLeftIcon.setImageResource(R.drawable.ic_setting_back_white);
            mActionBarTitle.setText(mTitles[mViewPager.getCurrentItem()]);
        }
    }

    private void removeActionBarEdit() {
        mEdit = false;
        mActionBarLeftIcon.setImageResource(R.drawable.ic_setting_back_white);
        mActionBarTitle.setText(mTitles[mViewPager.getCurrentItem()]);
        if (mList.get(0) instanceof BrowserBookmarksPage) {
            ((BrowserBookmarksPage) mList.get(0)).removeSelectClear();
        }
        if (mList.get(1) instanceof BrowserHistoryPage) {
            ((BrowserHistoryPage) mList.get(1)).addClearButton();
        }
    }
}
