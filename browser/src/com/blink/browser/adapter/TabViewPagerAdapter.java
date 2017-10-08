// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class TabViewPagerAdapter extends PagerAdapter {
    private String[] mTitles;
    private List<View> mViews;

    public TabViewPagerAdapter(List<View> views, String[] titles) {
        this.mViews = views;
        this.mTitles = titles;
    }

    public TabViewPagerAdapter(List<View> views) {
        this.mViews = views;
    }

    public void setViews(List<View> views) {
        this.mViews = views;
    }

    @Override
    public void destroyItem(ViewGroup v, int position, Object obj) {
        if (position < mViews.size()) {
            v.removeView(mViews.get(position));
        }
    }

    @Override
    public void finishUpdate(ViewGroup arg0) {

    }

    @Override
    public int getCount() {
        return mViews == null ? 0 : mViews.size();
    }

    @Override
    public Object instantiateItem(ViewGroup v, int position) {
        v.addView(mViews.get(position));
        return mViews.get(position);
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {

    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate(ViewGroup arg0) {

    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }
}
