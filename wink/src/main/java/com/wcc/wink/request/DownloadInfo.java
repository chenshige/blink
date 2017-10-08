package com.wcc.wink.request;

import android.graphics.drawable.Drawable;

import com.wcc.wink.Resource;
import com.wcc.wink.loader.ResourceLoader;

/**
 * Created by wenbiao.xie on 2016/6/13.
 */
public class DownloadInfo implements Resource {

    private long id;

    private String key;

    private String title;
    /**
     * 获取下载状态
     */
    private int downloadState;
    /**
     * 获取下载总大小
     */
    private long totalSizeInBytes;
    /**
     * 获取已下载大小
     */
    private long downloadedSizeInBytes;
    /**
     * 获取下载进度
     */
    private int downloadProgress;
    /**
     * 获取下载类型
     */
    private int downloadMode;
    /**
     * 本地文件存储路径
     */
    private String localFilePath;

    /**
     * 获取下载URL
     */
    private String url;

    /**
     * 下载跟踪数据
     */
    private final Tracer tracer = new Tracer();

    private Class resourceClass;
    private String resourceKey;

    Resource resource;
    ResourceLoader<Resource, DownloadInfo> loader;

    int downloaderType;

    /**
     * 下载分段信息
     */
    DownloadRange[] ranges;

    private Drawable apkIcon;

    private String apkName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDownloadState() {
        return downloadState;
    }

    public void setDownloadState(int downloadState) {
        this.downloadState = downloadState;
    }

    public long getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    public void setTotalSizeInBytes(long s) {
        totalSizeInBytes = s;
    }


    public long getDownloadedSizeInBytes() {
        return downloadedSizeInBytes;
    }

    public void setDownloadedSizeInBytes(long s) {
        downloadedSizeInBytes = s;
    }

    void incrDownloadedBytes(int s) {
        downloadedSizeInBytes += s;
        if (totalSizeInBytes > 0) {
            int p = (int) (downloadedSizeInBytes * 100 / totalSizeInBytes);
            downloadProgress = Math.min(p, 100);
        }
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public int getDownloadMode() {
        return downloadMode;
    }

    public void setDownloadMode(int downloadMode) {
        this.downloadMode = downloadMode;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String s) {
        localFilePath = s;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 重置下载数据
     */
    public void reset() {
        totalSizeInBytes = 0;
        downloadedSizeInBytes = 0;
        downloadProgress = 0;
        ranges = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public Class getResourceClass() {
        return resourceClass;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String key) {
        resourceKey = key;
    }

    public void setResource(Resource resource) {
        if (this.resource == resource)
            return;
        this.resource = resource;
        if (resource != null) {
            this.resourceClass = resource.getClass();
            this.resourceKey = resource.getKey();
        }
    }

    public Resource getResource() {
        return resource;
    }

    public ResourceLoader<Resource, DownloadInfo> getLoader() {
        return loader;
    }

    public void setLoader(ResourceLoader<Resource, DownloadInfo> loader) {
        this.loader = loader;
    }

    public boolean fromResource(Class<? extends Resource> clz) {
        return clz.equals(resourceClass);
    }

    public void setDownloaderType(int downloaderType) {
        this.downloaderType = downloaderType;
    }

    public void setRanges(DownloadRange[] ranges) {
        this.ranges = ranges;
    }

    public int getDownloaderType() {
        return downloaderType;
    }

    public DownloadRange[] getRanges() {
        return ranges;
    }

    public Drawable getApkIcon() {
        return apkIcon;
    }

    public void setApkIcon(Drawable icon) {
        this.apkIcon = icon;
    }

    public String getApkName() {
        return apkName;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public static class DownloadRange {
        public long start;
        public long length;
        public long downloaded;

        public long end() {
            return start + length;
        }

        public long currentPosition() {
            return start + downloaded;
        }

        public int progress() {
            if (length == 0 || downloaded == 0) return 0;
            return (int) (downloaded * 100 / length);
        }

        public boolean finished() {
            return length > 0 && downloaded == length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DownloadRange range = (DownloadRange) o;

            if (start != range.start) return false;
            return length == range.length;

        }

        @Override
        public int hashCode() {
            int result = (int) (start ^ (start >>> 32));
            result = 31 * result + (int) (length ^ (length >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "{" +
                    "downloaded= " + downloaded +
                    ", start= " + start +
                    ", length= " + length +
                    ", progress= " + progress() + '%' +
                    '}';
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadInfo that = (DownloadInfo) o;

        if (id != 0 && that.id != 0 && id != that.id) return false;
        return key != null ? key.equals(that.key) : that.key == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
