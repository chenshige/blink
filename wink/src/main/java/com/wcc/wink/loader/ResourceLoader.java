package com.wcc.wink.loader;

import java.io.File;

/**
 * Created by wenbiao.xie on 2016/6/14.
 */
public interface ResourceLoader<T, Y> {
    /**
     * 根据指定资源创建相应的模型数据
     * @param model 资源对象
     * @return
     */
    Y createResource(T model);

    /**
     * 根据指定资源获取防盗链对象
     * @param model 资源对象
     * @return
     */
    UrlFetcher<T> getUrlFetcher(T model);

    /**
     * 获取资源最后下载时存储的目标文件对象
     * @param model 资源对象
     * @param resource 下载模型数据
     * @return
     */
    File getTargetFile(T model, Y resource);
}
