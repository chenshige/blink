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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.UI.ComboViews;
import com.blink.browser.util.DisplayUtil;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;

public class SnapshotBar extends LinearLayout implements OnClickListener {

    private ImageView mFavicon;
    private TextView mDate;
    private TextView mTitle;
    private View mBookmarks;
    private TextView mTabSwitcher;
    private View mMore;
    private View mToggleContainer;
    private boolean mIsAnimating;
    private ViewPropertyAnimator mTitleAnimator, mDateAnimator;
    private float mAnimRadius = 20f;
    private View mTabCountContainer;

    public SnapshotBar(Context context) {
        super(context);
    }

    public SnapshotBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnapshotBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFavicon = (ImageView) findViewById(R.id.favicon);
        mDate = (TextView) findViewById(R.id.date);
        mTitle = (TextView) findViewById(R.id.title);
//        mBookmarks = findViewById(R.id.all_btn);
        mMore = findViewById(R.id.more);
        mMore.setOnClickListener(this);
        mToggleContainer = findViewById(R.id.toggle_container);
        mTabSwitcher = (TextView) findViewById(R.id.tab_switcher);
        mTabCountContainer = findViewById(R.id.tabcount_container);

        if (mBookmarks != null) {
            mBookmarks.setOnClickListener(this);
        }
        if (mToggleContainer != null) {
            mTabCountContainer.setOnClickListener(this);
        }
        if (mMore != null) {
            mMore.setOnClickListener(this);
            mMore.setVisibility(VISIBLE);
        }
        if (mToggleContainer != null) {
            mToggleContainer.setOnClickListener(this);
            resetAnimation();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mToggleContainer != null) {
            mAnimRadius = mToggleContainer.getHeight() / 2f;
        }
    }

    void resetAnimation() {
        if (mToggleContainer == null) {
            // No animation needed/used
            return;
        }
        if (mTitleAnimator != null) {
            mTitleAnimator.cancel();
            mTitleAnimator = null;
        }
        if (mDateAnimator != null) {
            mDateAnimator.cancel();
            mDateAnimator = null;
        }
        mIsAnimating = false;
        mTitle.setAlpha(1f);
        mTitle.setTranslationY(0f);
        mTitle.setRotationX(0f);
        mDate.setAlpha(0f);
        mDate.setTranslationY(-mAnimRadius);
        mDate.setRotationX(90f);
    }

    private void showDate() {
        mTitleAnimator = mTitle.animate()
                .alpha(0f)
                .translationY(mAnimRadius)
                .rotationX(-90f);
        mDateAnimator = mDate.animate()
                .alpha(1f)
                .translationY(0f)
                .rotationX(0f);
    }

    private void showTitle() {
        mTitleAnimator = mTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .rotationX(0f);
        mDateAnimator = mDate.animate()
                .alpha(0f)
                .translationY(-mAnimRadius)
                .rotationX(90f);
    }

    @Override
    public void onClick(View v) {
    }

    public void onTabDataChanged(Tab tab) {
        if (!tab.isSnapshot()) return;
        SnapshotTab snapshot = (SnapshotTab) tab;
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        mDate.setText(dateFormat.format(new Date(snapshot.getDateCreated())));

        String title = snapshot.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = UrlUtils.stripUrl(snapshot.getUrl());
        }
        mTitle.setText(title);
        resetAnimation();
    }


    public boolean isAnimating() {
        return mIsAnimating;
    }
}
