package com.wcc.wink.util;

/**
 * 单例控制类
 * Created by wenbiao.xie on 2015/12/24.
 */
public abstract class Singleton<T> {
    private volatile T mInstance;
    protected abstract T create();

    public final T get() {
        if (mInstance == null) {
            synchronized (this) {
                if (mInstance == null) {
                    mInstance = create();
                }
            }
        }

        return mInstance;
    }
}
