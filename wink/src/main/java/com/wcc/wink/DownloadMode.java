package com.wcc.wink;

/**
 * Created by wenbiao.xie on 2015/11/6.
 */
public class DownloadMode {
    /**
     * 手动下载：手动点击、断点恢复时仅wifi
     */
    public static final int  NORMAL = 0;

    /**
     * 免流量下载：可更新应用 仅wifi自动下载、不自动安装、仅wifi断点下载
     */
    public static final int AUTO = 1;

    /**
     * 免流量下载并安装：可更新应用 仅wifi自动下载、自动安装、仅支持wifi断点下载
     */
    public static final int AUTO_INSTALL = AUTO + 1;

    public static boolean shouldSilent(int mode) {
        return mode >= AUTO;
    }

    public static boolean freeOfInstall(int mode) {
        return mode == AUTO;
    }
}
