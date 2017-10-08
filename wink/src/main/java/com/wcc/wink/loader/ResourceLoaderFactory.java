package com.wcc.wink.loader;

import android.content.Context;

/**
 * Created by wenbiao.xie on 2016/6/14.
 */
public interface ResourceLoaderFactory<T, Y> {
    /**
     * Build a concrete ResourceLoader for this model type.
     *
     * @param context A context that cannot be retained by the factory but can be retained by the {@link ResourceLoader}
     * @param factories A map of classes to factories that can be used to construct additional {@link ResourceLoader}s that
     *                  this factory's {@link ResourceLoader} may depend on
     * @return A new {@link ResourceLoader}
     */
    ResourceLoader<T, Y> build(Context context, GenericLoaderFactory factories);

    /**
     * A lifecycle method that will be called when this factory is about to replaced.
     */
    void teardown();
}
