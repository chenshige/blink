package com.wcc.wink;

import android.text.TextUtils;

import com.wcc.wink.model.SQLiteInterceptor;
import com.wcc.wink.util.WLog;

import java.io.File;
import java.io.IOException;

/**
 * Created by wenbiao.xie on 2016/8/22.
 */
public class WinkSetting implements Cloneable {
    final static int POOL_MAX_THREAD = 3;
    final static int POOL_SILENT_MAX_THREAD = 1;
    final static long MIN_SD_SPACE = 10 * 1024 * 1024L;
    final static String DEFAULT_THREAD_NAME = "wink_task";

    private int maxRunningSize;
    private int maxSilentRunningSize;
    private long minimumSdSpace;
    private boolean needCheckLengthBeforeDownload;
    private String userAgent;
    private SQLiteInterceptor interceptor;
    private File simpleResourceStorageDirectory;

    static WinkSetting getDefault() {
        WinkSetting setting = new WinkSetting();
        setting.setMaxRunningSize(POOL_MAX_THREAD);
        setting.setMaxSilentRunningSize(POOL_SILENT_MAX_THREAD);
        setting.setMinimumSdSpace(MIN_SD_SPACE);
        setting.setNeedCheckLengthBeforeDownload(true);
        return setting;
    }

    private WinkSetting() {}

    WinkSetting(int maxRunningSize, int maxSilentRunningSize, long minimumSdSpace,
                boolean needCheckLengthBeforeDownload,
                String userAgent, SQLiteInterceptor interceptor, String simpleResourceStoragePath) {
        this.maxRunningSize = maxRunningSize;
        this.maxSilentRunningSize = maxSilentRunningSize;
        this.minimumSdSpace = minimumSdSpace;
        this.needCheckLengthBeforeDownload = needCheckLengthBeforeDownload;
        this.userAgent = userAgent;
        this.interceptor = interceptor;
        if (!TextUtils.isEmpty(simpleResourceStoragePath)) {
            try {
                File dir = new File(simpleResourceStoragePath);
                if (dir.exists()) {
                    if (dir.isFile()) {
                        dir.delete();
                        dir.mkdirs();
                    }
                }

                else {
                    dir.mkdirs();
                }

                this.simpleResourceStorageDirectory = dir;
            } catch (SecurityException e) {
                throw e;
            }

            catch (Exception e) {
                WLog.printStackTrace(e);
            }
        }

    }

    WinkSetting(WinkSetting other) {
        this.maxRunningSize = other.maxRunningSize;
        this.maxSilentRunningSize = other.maxSilentRunningSize;
        this.needCheckLengthBeforeDownload = other.needCheckLengthBeforeDownload;
        this.minimumSdSpace = other.minimumSdSpace;
        this.userAgent = other.userAgent;
        this.interceptor = other.interceptor;
        this.simpleResourceStorageDirectory = other.simpleResourceStorageDirectory;
    }

    public File getSimpleResourceStorageDirectory() {
        return simpleResourceStorageDirectory;
    }

    public void setSimpleResourceStoragePath(String simpleResourceStoragePath) {
        if (simpleResourceStorageDirectory != null
                && simpleResourceStorageDirectory.getAbsolutePath().equals(simpleResourceStoragePath)) {
            return;
        }

        if (!TextUtils.isEmpty(simpleResourceStoragePath)) {
            try {
                File dir = new File(simpleResourceStoragePath);
                if (dir.exists()) {
                    if (dir.isFile()) {
                        dir.delete();
                        dir.mkdirs();
                    }
                }

                else {
                    dir.mkdirs();
                }

                this.simpleResourceStorageDirectory = dir;
            } catch (SecurityException e) {
                throw e;
            }

            catch (Exception e) {
                WLog.printStackTrace(e);
            }
        } else {
            this.simpleResourceStorageDirectory = null;
        }
    }

    public int getMaxRunningSize() {
        return maxRunningSize;
    }

    void setMaxRunningSize(int maxRunningSize) {
        this.maxRunningSize = maxRunningSize;
    }

    public int getMaxSilentRunningSize() {
        return maxSilentRunningSize;
    }

    void setMaxSilentRunningSize(int maxSilentRunningSize) {
        this.maxSilentRunningSize = maxSilentRunningSize;
    }

    public long getMinimumSdSpace() {
        return minimumSdSpace;
    }

    void setMinimumSdSpace(long minimumSdSpace) {
        this.minimumSdSpace = minimumSdSpace;
    }

    public boolean isNeedCheckLengthBeforeDownload() {
        return needCheckLengthBeforeDownload;
    }

    void setNeedCheckLengthBeforeDownload(boolean needCheckLengthBeforeDownload) {
        this.needCheckLengthBeforeDownload = needCheckLengthBeforeDownload;
    }

    public String getUserAgent() {
        return userAgent;
    }

    void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public SQLiteInterceptor getSQLInterceptor() {
        return interceptor;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        WinkSetting setting = (WinkSetting) super.clone();
        setting.maxRunningSize = this.maxRunningSize;
        setting.maxSilentRunningSize = this.maxSilentRunningSize;
        setting.needCheckLengthBeforeDownload = this.needCheckLengthBeforeDownload;
        setting.minimumSdSpace = this.minimumSdSpace;
        setting.userAgent = this.userAgent;
        setting.interceptor = this.interceptor;
        setting.simpleResourceStorageDirectory = this.simpleResourceStorageDirectory;
        return setting;
    }
}
