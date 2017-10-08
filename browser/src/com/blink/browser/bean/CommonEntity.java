package com.blink.browser.bean;

import com.blink.browser.DatabaseManager;

import java.io.Serializable;

/**
 * <p/>
 * imageIcon is byte[] type,id is int type,others is String
 */
public class CommonEntity implements Serializable{

    class CommonColumn {
        public static final String URL = "url";
        public static final String IMAGE_ICON = "imageicon";
        public static final String ID = "_id";
        public static final String IMAGE_URL = "imageurl";
        public static final String TITLE = "title";
    }

    private long id;
    private String title;
    private byte[] imageIcon;
    private String imageUrl;
    private String url;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public byte[] getImageIcon() {
        return imageIcon;
    }

    public void setImageIcon(byte[] imageIcon) {
        this.imageIcon = imageIcon;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int updateToDb() {
        return DatabaseManager.getInstance().updateBy(this);
    }
}
