package com.wcc.wink.model;

/**
 * Created by wenbiao.xie on 2016/7/18.
 */
public interface Copyable<T> {
    void copyTo(T target);
}
