package com.wcc.wink;

/**
 * 下载通知控制器
 * Created by wenbiao.xie on 2016/6/15.
 */
public interface WinkNotificationController {
    /**
     * 显示下载任务完成通知
     * @param resource 下载信息对象
     */
    void showCompletedNotification(Resource resource);
    /**
     * 显示下载任务出错信息
     * @param resource 下载资源
     */
    void showErrorNotification(Resource resource, int err);

    /**
     * 显示下载任务删除
     * @param resource 下载信息对象
     */
    void showDeletedNotification(Resource resource);
    /**
     * 显示下载任务进度信息
     * @param resource 下载信息对象
     */
    void showProgressNotification(Resource resource, int progress);
    /**
     * 显示下载任务暂停
     * @param resource 下载信息对象
     */
    void showPauseNotification(Resource resource);
}
