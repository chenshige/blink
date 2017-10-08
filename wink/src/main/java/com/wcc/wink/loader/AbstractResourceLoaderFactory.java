package com.wcc.wink.loader;


import com.wcc.wink.request.DownloadInfo;

/**
 * Created by wenbiao.xie on 2016/11/10.
 */

public abstract class AbstractResourceLoaderFactory<T> implements ResourceLoaderFactory<T, DownloadInfo> {

    @Override
    public void teardown() {
        // Do nothing.
    }
}
