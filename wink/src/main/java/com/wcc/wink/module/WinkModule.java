package com.wcc.wink.module;

import android.content.Context;

import com.wcc.wink.Wink;
import com.wcc.wink.WinkBuilder;

/**
 * Created by wenbiao.xie on 2016/6/16.
 */
public interface WinkModule {

    /**
     * Lazily apply options to a {@link com.wcc.wink.WinkBuilder} immediately before the Wink singleton is
     * created.
     *
     * <p>
     *     This method will be called once and only once per implementation.
     * </p>
     *
     * @param context An Application {@link Context}.
     * @param builder The {@link com.wcc.wink.WinkBuilder} that will be used to create Wink.
     */
    void applyOptions(Context context, WinkBuilder builder);

    /**
     * Lazily register components immediately after the Wink singleton is created but before any requests can be
     * started.
     *
     * <p>
     *     This method will be called once and only once per implementation.
     * </p>
     *
     * @param context An Application {@link Context}.
     * @param wink The newly created Wink singleton.
     */
    void registerComponents(Context context, Wink wink);


}
