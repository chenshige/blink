package com.blink.browser.download;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.StorageUtils;
import com.blink.browser.util.SystemTintBarUtils;
import com.blink.browser.widget.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

public class DownloadActivity extends FragmentActivity implements View.OnClickListener {

    public static final int DOWNLOAD_RENAME = 1;
    public static final int DOWNLOADING = 2;
    public static final int DOWNLOADED = 3;
    private List<Fragment> mList = new ArrayList<>();
    private TextView mActionBarTitle;
    private String[] mTitles;
    private ImageView mActionBarLeftIcon;
    private ViewPager mViewPager;
    private TextView mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_download);
        initActionBar();
        initView();
        DisplayUtil.changeScreenBrightnessIfNightMode(this);
        SystemTintBarUtils.setSystemBarColor(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BrowserSettings.getInstance().getShowStatusBar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams
                    .FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.layout_custom_actionbar);
            View actionbarView = actionBar.getCustomView();
            mActionBarLeftIcon = (ImageView) actionbarView.findViewById(R.id.actionbar_left_icon);
            mActionBarLeftIcon.setOnClickListener(this);
            mActionBarTitle = (TextView) actionbarView.findViewById(R.id.actionbar_title);
            mActionBarTitle.setText(R.string.downloading);
        }
    }

    private void initView() {
        mStorage = (TextView) findViewById(R.id.storage_space);
        mList.add(new DownloadingFragment());
        mList.add(new DownloadedFragment());

        mStorage.setText(StorageUtils.getAllSDSpace(this));
        initTabs();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        swtichDownloaded(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        swtichDownloaded(intent);
    }

    private void initTabs() {
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.pager_tabs);
        mViewPager = (ViewPager) findViewById(R.id.pager_view);
        mTitles = new String[]{getResources().getString(R.string.downloading), getResources().getString(R
                .string.downloaded)};
        mViewPager.setOffscreenPageLimit(mTitles.length);
        mViewPager.setAdapter(new BasePagerAdapter(getSupportFragmentManager(), mTitles));
        tabs.setViewPager(mViewPager);
        tabs.setOnPageChangeListener(mOnPageChangeListener);
    }

    class BasePagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {
        String[] mTitles;
        int[] mIcon;
        int[] mIconSelect;

        public BasePagerAdapter(FragmentManager fm, String[] titles) {
            super(fm);
            this.mTitles = titles;
            mIcon = new int[]{R.drawable.ic_browser_tab_download, R.drawable.ic_browser_menu_downloaded};
            mIconSelect = new int[]{R.drawable.ic_browser_tab_download_seleted, R.drawable
                    .ic_browser_menu_downloaded_avtived};
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
    public void onClick(View v) {
        if (R.id.actionbar_left_icon == v.getId()) {
            this.finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mActionBarTitle.setText(mTitles[position]);
            if (position == 1) {
                ((DownloadedFragment) mList.get(position)).refresh();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void swtichDownloaded(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            int state = intent.getExtras().getInt(BrowserContract.DOWNLOAD_STATE);
            if (state == DOWNLOADED) {
                mViewPager.setCurrentItem(1);
            }
        }
    }
}
