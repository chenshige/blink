package com.wcc.wink;

import android.content.Context;

import com.wcc.wink.model.SQLiteInterceptor;
import com.wcc.wink.util.NetworkSensor;

/**
 * Created by wenbiao.xie on 2016/8/22.
 */
public class WinkBuilder {

    private final Context context;
    private int maxRunningSize;
    private int maxSilentRunningSize;
    private long minimumSdSpace;
    private boolean needCheckLengthBeforeDownload;
    private String userAgent;
    private NetworkSensor networkSensor;
    private SQLiteInterceptor sqLiteInterceptor;
    private String simpleResourceStoragePath;

    public WinkBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public WinkBuilder setMaxSilentRunningSize(int maxSilentRunningSize) {
        if (maxSilentRunningSize < 0)
            throw new IllegalArgumentException("maxSilentRunningSize must be > 0");

        this.maxSilentRunningSize = maxSilentRunningSize;
        return this;
    }

    public WinkBuilder setMinimumSdSpace(long minimumSdSpace) {
        if (minimumSdSpace < 0)
            throw new IllegalArgumentException("minimumSdSpace must be > 0");

        this.minimumSdSpace = minimumSdSpace;
        return this;
    }

    public WinkBuilder setNeedCheckLengthBeforeDownload(boolean needCheckLengthBeforeDownload) {
        this.needCheckLengthBeforeDownload = needCheckLengthBeforeDownload;
        return this;
    }

    public WinkBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public NetworkSensor getNetworkSensor() {
        return networkSensor;
    }

    public void setNetworkSensor(NetworkSensor networkSensor) {
        this.networkSensor = networkSensor;
    }

    public SQLiteInterceptor getSQLiteInterceptor() {
        return sqLiteInterceptor;
    }

    public void setSQLiteInterceptor(SQLiteInterceptor sqLiteInterceptor) {
        this.sqLiteInterceptor = sqLiteInterceptor;
    }

    public String getSimpleResourceStoragePath() {
        return simpleResourceStoragePath;
    }

    public void setSimpleResourceStoragePath(String simpleResourceStoragePath) {
        this.simpleResourceStoragePath = simpleResourceStoragePath;
    }

    Wink build() {

        if (maxRunningSize == 0)
            maxRunningSize = WinkSetting.POOL_MAX_THREAD;

        if (maxSilentRunningSize == 0)
            maxSilentRunningSize = WinkSetting.POOL_SILENT_MAX_THREAD;

        if (minimumSdSpace == 0)
            minimumSdSpace = WinkSetting.MIN_SD_SPACE;

        WinkSetting setting = new WinkSetting(maxRunningSize, maxSilentRunningSize, minimumSdSpace,
                needCheckLengthBeforeDownload, userAgent,
                sqLiteInterceptor,
                simpleResourceStoragePath);

        return new Wink(context, setting);
    }
}
