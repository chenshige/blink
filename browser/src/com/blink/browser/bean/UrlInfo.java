package com.blink.browser.bean;

public class UrlInfo {
    private int id;
    private String displayname;
    private int status;
    private String url; // url的详细地址
    private long datetime; // 访问时间
    private int weight; // 访问权重

    private boolean isHeader;

    private int datatype;

    public UrlInfo() {
    }

    public UrlInfo(String url, boolean header) {
        this.url = url;
        this.isHeader = header;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public void setIsHeader(boolean isHeader) {
        this.isHeader = isHeader;
    }

    public int getDatatype() {
        return datatype;
    }

    public void setDatatype(int datatype) {
        this.datatype = datatype;
    }

}
