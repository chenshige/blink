package com.wcc.wink;

import com.wcc.wink.request.WinkRequest;
import com.wcc.wink.request.WinkStat;

/**
 * 下载统计扩展接口
 * Created by wenbiao.xie on 2016/7/22.
 */
public interface WinkStatHandler {
    /**
     * 处理不同阶段下载任务的统计信息与上报
     * @param request 下载任务
     * @param stat 下载相关的一些统计数据
     */
    void handle(WinkRequest request, WinkStat stat);
}
