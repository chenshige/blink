package com.wcc.wink.loader;


import com.wcc.wink.WinkError;

/**
 * Created by wenbiao.xie on 2016/7/13.
 */
public class DirectUrlFetcher<T> implements UrlFetcher<T> {


    @Override
    public int load(T data, boolean force) throws Exception {
        return WinkError.NetworkError.SUCCESS;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void cancel() {

    }
}
