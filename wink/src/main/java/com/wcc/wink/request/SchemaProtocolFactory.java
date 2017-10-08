package com.wcc.wink.request;


import android.content.Context;
/**
 * Created by wenbiao.xie on 2016/11/1.
 */

public interface SchemaProtocolFactory {
    /**
     * 获取支持的协议集
     * @return 返回协议集合
     */
    String[] supportSchemas();
    /**
     * 创建下载器，用于执行实际下载行为
     *
     * 一般来说，资源实体的下载默认只支持http与https，对于特殊协议规范的支持，可通过该接口进行扩展
     * 如ftp, ed2k, magnet等下载协议
     * 对于扩展引入的实体需要在{@link DownloaderFactory} 中注册相应的工厂类
     *
     * @param context 上下文对象
     * @param task 下载任务对象
     * @return 返回创建成功的下载对象
     */
    Downloader build(Context context, WinkRequest task);
}
