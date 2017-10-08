package com.wcc.wink.request;

/**
 * Created by wenbiao.xie on 2016/6/21.
 */
public interface RuntimePolicy {

    int maxRunningPoolSize();

    int slientRunningPoolSize();
}
