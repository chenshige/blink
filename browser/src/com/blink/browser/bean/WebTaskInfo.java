package com.blink.browser.bean;

import android.graphics.Bitmap;
import android.webkit.WebView;

import java.io.Serializable;

public class WebTaskInfo implements Serializable, Cloneable{

    public static int PAGE_TYPE_DEFAULT = 1;
    public static int PAGE_TYPE_IMAGE = 2;
    public static int PAGE_TYPE_WEBVIEW = 3;

    public static int REFERSH_TYPE_NEW = 1;
    public static int REFERSH_TYPE_NEED = 2;
    public static int REFERSH_TYPE_NO_NEED = 3;

    private int id;
    private String title;
    private String url;
    private String localFile;
    private Bitmap thumbnail;
    private WebView webView;
    private String iconPath;
    private long orderTime;
    private int pageType;
    private int refershType = REFERSH_TYPE_NEW;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRefershType() {
        return refershType;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public void setPageType(int pageType) {
        this.pageType = pageType;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public void setRefershType(int refershType) {
        this.refershType = refershType;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public WebView getWebView() {
        return webView;
    }

    public int getPageType() {
        return pageType;
    }

    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public long getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(long orderTime) {
        this.orderTime = orderTime;
    }

    public WebTaskInfo clone () {
        WebTaskInfo o = null;
        try {
            o = (WebTaskInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return o;
    }

//    @Override
//    public String toString() {
//        return "WebTaskInfo{" +
//                "id=" + id +
//                ", title='" + title + '\'' +
//                ", url='" + url + '\'' +
//                ", localFile='" + localFile + '\'' +
//                ", thumbnail=" + thumbnail +
//                ", webView=" + webView +
//                ", pageType=" + pageType +
//                ", refershType=" + refershType +
//                '}';
//    }
}
