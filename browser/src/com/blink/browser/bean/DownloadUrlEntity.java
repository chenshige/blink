package com.blink.browser.bean;

import android.graphics.drawable.Drawable;

import com.blink.browser.database.IgnoreField;

/**
 * <p/>
 * recommend url in BrowserSQLiteHelper DownloadUrlEntity table
 */
public class DownloadUrlEntity extends CommonEntity {
    private int status = 0;
    private int refrence = 0;
    private String mimetype = "";
    private String originurl = "";
    @IgnoreField
    private int progress = 0;
    private long size = 0;
    @IgnoreField
    private long downsize = 0;
    @IgnoreField
    private String filename = "";
    @IgnoreField
    private Drawable icon = null;
    @IgnoreField
    private String filesize = "";
    private String time = "0";
    @IgnoreField
    private String section = null;

    public DownloadUrlEntity() {
    }

    public DownloadUrlEntity(String[] arg) {
        this.setTitle(arg[0]);
        this.setUrl(arg[1]);
        this.setImageUrl(arg[2]);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getRefrence() {
        return refrence;
    }

    public void setRefrence(int refrence) {
        this.refrence = refrence;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDownsize() {
        return downsize;
    }

    public void setDownsize(long downsize) {
        this.downsize = downsize;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getFilesize() {
        return filesize;
    }

    public void setFilesize(String filesize) {
        this.filesize = filesize;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getOriginurl() {
        return originurl;
    }

    public void setOriginurl(String originurl) {
        this.originurl = originurl;
    }

    public class Column extends CommonColumn {
        public static final String STATUS = "status";
        public static final String REFRENCE = "refrence";
        public static final String MIMETYPE = "mimetype";
        public static final String TIME = "time";
        public static final String SIZE = "size";
        public static final String ORIGINURL = "originurl";
    }
}
