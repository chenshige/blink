package com.blink.browser.menu;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;

import java.util.List;

public class MenuViewPagerAdapter extends PagerAdapter {
    private List<LinearLayout> mArray;


    public MenuViewPagerAdapter(Context context, List<LinearLayout> array) {
        this.mArray = array;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mArray.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        // TODO Auto-generated method stub
        return arg0 == arg1;
    }

    @Override
    public Object instantiateItem(View arg0, int arg1) {
        ((ViewPager) arg0).addView(mArray.get(arg1));
        return mArray.get(arg1);
    }

    @Override
    public void destroyItem(View arg0, int arg1, Object arg2) {
        ((ViewPager) arg0).removeView((View) arg2);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }
}
