package com.blink.browser.bean;

/**
 * this is most visited record.
 * Created by chendeqiao on 16/7/5.
 */
public class BrowserInfoBean {

    private int id;
    private String title;
    private String url;
    private byte[] touchIcon;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public byte[] getTouchIcon() {
        return touchIcon;
    }

    public void setTouchIcon(byte[] touchIcon) {
        this.touchIcon = touchIcon;
    }
}
