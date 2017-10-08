package com.wcc.wink;

import com.wcc.wink.request.DownloadInfo;

import java.util.Collection;

/**
 * 下载速度关注器
 * Created by wenbiao.xie on 2016/10/28.
 */

public interface SpeedWatcher {
    void onSpeedChanged(Collection<DownloadInfo> tasks);
}
