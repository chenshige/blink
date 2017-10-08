// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.SystemTintBarUtils;

public class SnapshotActivity extends FragmentActivity implements View.OnClickListener, CombinedBookmarksCallbacks {
    public static final String EXTRA_OPEN_SNAPSHOT = "snapshot_id";
    public static final String EXTRA_OPEN_ALL = "open_all";
    private TextView mActionBarTitle;
    private ImageView mActionBarLeftIcon;
    private boolean mEdit = false;
    private BrowserSnapshotPage mPage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setBrowserActionBar();
        setContentView(R.layout.activity_browser_preferences_page);
        SystemTintBarUtils.setSystemBarColor(this);
        DisplayUtil.changeScreenBrightnessIfNightMode(this);

        mPage = new BrowserSnapshotPage();
        getSupportFragmentManager().beginTransaction().replace(R.id.content_layout, mPage)
                .commitAllowingStateLoss();
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

    public void setBrowserActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.layout_custom_actionbar);
            View actionbarView = actionBar.getCustomView();

            ((TextView) actionbarView.findViewById(R.id.actionbar_title)).setText(R.string.tab_snapshots);
            mActionBarLeftIcon = (ImageView) actionbarView.findViewById(R.id.actionbar_left_icon);
            mActionBarLeftIcon.setOnClickListener(this);
            mActionBarTitle = (TextView) actionbarView.findViewById(R.id.actionbar_title);
            mActionBarTitle.setText(R.string.tab_snapshots);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.actionbar_left_icon) {
            if (mEdit) {
                removeActionBarEdit();
            } else {
                this.finish();
            }
        }
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

    public void setActionBarEdit(boolean edit) {
        mEdit = edit;
        if (edit) {
            mActionBarLeftIcon.setImageResource(R.drawable.ic_browser_home_close);
            mActionBarTitle.setText(getResources().getString(R.string.edit));
        } else {
            mActionBarLeftIcon.setImageResource(R.drawable.ic_setting_back_white);
            mActionBarTitle.setText(R.string.tab_snapshots);
        }
    }

    private void removeActionBarEdit() {
        mEdit = false;
        mActionBarLeftIcon.setImageResource(R.drawable.ic_setting_back_white);
        mActionBarTitle.setText(R.string.tab_snapshots);
        mPage.removeSelectClear();
    }
}
