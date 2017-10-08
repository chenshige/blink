package com.wcc.wink.request;

/**
 * Created by wenbiao.xie on 2016/6/14.
 */
public interface ResourceStatus {
    /**
     * 删除状态
     *
     */
    int DELETED = -1;

    /**
     * 初始状态
     */
    int INIT = 0;

    /**
     * 等待下载状态
     */
    int WAIT = 1;

    /**
     * 下载中
     */
    int DOWNLOADING = 2;

    /**
     * 暂停状态
     */
    int PAUSE = 3;

    /**
     * 下载失败
     */
    int DOWNLOAD_FAILED = 4;

    /**
     * 下载完成状态
     *
     */
    int DOWNLOADED = 5;
}
