package com.wcc.wink.model;

/**
 * Created by wenbiao.xie on 2016/6/16.
 */
public interface KeyGenerator<K, E> {
    K key(E e);
}
