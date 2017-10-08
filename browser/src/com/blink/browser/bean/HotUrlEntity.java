package com.blink.browser.bean;

import com.blink.browser.database.IgnoreField;

/**
 * hot url in BrowserSQLiteHelper hoturlentity table
 */
public class HotUrlEntity extends CommonEntity {
    public static final int ADD = 1;
    public static final int UPDATE = 2;
    public static final int DELETE = 3;

    //服务器端返回的是displayName,这里忽略title
    private String displayName;

    //服务器端返回的权重
    private int weight;

    //服务器端返回状态值，不用存数据库
    @IgnoreField
    private int status;

    //服务器端返回的id
    private int uId;

    public HotUrlEntity() {
    }

    public HotUrlEntity(String[] arg) {
        if (arg == null || arg.length < 3) {
            throw new RuntimeException("Check common_url.xml !");
        }
        this.setTitle(arg[0]);
        this.setUrl(arg[1]);
        this.setImageUrl(arg[2]);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getuId() {
        return uId;
    }

    public void setuId(int uId) {
        this.uId = uId;
    }

    public class Column extends CommonColumn {
        public static final String DISPLAY_NAME = "displayname";
        public static final String WEIGHT = "weight";
        public static final String UID = "uid";
    }

}
