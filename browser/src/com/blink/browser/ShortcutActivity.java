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
 * limitations under the License.
 */

package com.blink.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.SystemTintBarUtils;

public class ShortcutActivity extends FragmentActivity
        implements BookmarksPageCallbacks, OnClickListener {

    private BrowserBookmarksPage mBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.shortcut_bookmark_title);
        setContentView(R.layout.pick_bookmark);
        mBookmarks = (BrowserBookmarksPage) getSupportFragmentManager()
                .findFragmentById(R.id.bookmarks);
        mBookmarks.setEnableContextMenu(false);
        mBookmarks.setCallbackListener(this);
        View cancel = findViewById(R.id.cancel);
        if (cancel != null) {
            cancel.setOnClickListener(this);
        }
        DisplayUtil.changeScreenBrightnessIfNightMode(this);
        SystemTintBarUtils.setSystemBarColor(this);
        setBrowserActionBar();
    }

    public void setBrowserActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.layout_custom_actionbar);
            View actionbarView = actionBar.getCustomView();
            actionbarView.findViewById(R.id.actionbar_left).setOnClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BrowserSettings.getInstance().getShowStatusBar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    // BookmarksPageCallbacks
    @Override
    public boolean onBookmarkSelected(Cursor c, boolean isFolder) {
        return true;
    }

    @Override
    public boolean onOpenInNewWindow(String... urls) {
        return false;
    }

    @Override
    public boolean onBookmarkClick(BrowserBookmarksItem item) {
        if (item.isFolder) {
            return false;
        }
        Intent intent = BrowserBookmarksPage.createShortcutIntent(this, item);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
            case R.id.actionbar_left:
                finish();
                break;
        }
    }
}
