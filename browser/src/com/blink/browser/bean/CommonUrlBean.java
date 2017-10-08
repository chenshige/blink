package com.blink.browser.bean;

/**
 * 常用网站bean类
 */
public class CommonUrlBean {
    private int id;
    private String key;
    private String webUrl;
    private String imageUrl;
    private String title;
    public boolean isSelected;

    public CommonUrlBean(){}

    public CommonUrlBean(String key, String[] array){
            if(array.length<3){
                throw new RuntimeException(" file: common_url.xml is set error");
            }
            this.key = key;
            this.title = array[0];
            this.webUrl = array[1];
            this.imageUrl = array[2];
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
