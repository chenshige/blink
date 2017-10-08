package com.wcc.wink.request;

public enum DownloadState {
    intialized,     // 初始化状态
    ready,            // 等待状态
    active,         // 活动状态
    paused,         // 暂停状态
    stopped,        // 停止状态
    completed,      // 下载完成状态
    deleted;        // 删除状态，只存在于内存之中


    public int value() {
        return ordinal();
    }

    public static DownloadState valueOf(int state) {

        DownloadState[] states = values();
        for (int i = 0; i < states.length; i++) {
            if (states[i].ordinal() == state)
                return states[i];
        }

        throw new IllegalArgumentException("invalid wink state value");
    }
}
