package com.wcc.wink;


/**
 * Created by wenbiao.xie on 2016/6/14.
 */
public interface Resource {
    /**
     * 获取资源主键
     * @return
     */
    String getKey();

    /**
     * 获取资源标题
     * @return
     */
    String getTitle();

    /**
     * 获取资源下载地址
     * @return
     */
    String getUrl();
}
