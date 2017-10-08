package com.blink.browser;

import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {
    private boolean mViewIsPrepared;// 界面是否准备好
    private boolean mIsLazyLoad;// 是否开始请求数据


    protected void onViewPrepared() {
        mViewIsPrepared = true;
    }

    // 如果是第一个加载的Fragment，请在OnActivityCreate里面手动调用setUserVisibleHint(true)
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && mViewIsPrepared) {
            loadData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loadData() {
        if (!mIsLazyLoad) {
            lazyLoad();
            mIsLazyLoad = true;
        }
    }

    protected void lazyLoad() {}
}
