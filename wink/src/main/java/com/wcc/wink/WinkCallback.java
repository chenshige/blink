package com.wcc.wink;

import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.DownloadState;

/**
 * 下载过程与管理回调
 * Created by wenbiao.xie on 2016/6/21.
 */
public interface WinkCallback {
    /**
     * 增加相关任务
     * @param entities 任务实体
     */
    void onAdd(DownloadInfo... entities);

    /**
     * 删除相关任务
     * @param entities 任务实体
     */
    void onRemove(DownloadInfo... entities);

    /**
     * 清除所有任务数据
     */
    void onClear();

    /**
     * 下载进度变化回调
     * @param entity 下载实体对象
     */
    void onProgressChanged(DownloadInfo entity);

    /**
     * 下载任务状态变化回调
     * @param entity 下载实体对象
     */
    void onStatusChanged(DownloadInfo entity, DownloadState state);
}
