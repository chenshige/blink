package com.blink.browser.bean;

import java.util.List;

/**
 * 热门url
 */
public class HotUrlList {
    private int result;
    private String message;
    private long timeStamp;

    private List<HotUrlEntity> recommendUrl;


    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public List<HotUrlEntity> getRecommendUrl() {
        return recommendUrl;
    }

    public void setRecommendUrl(List<HotUrlEntity> recommendUrl) {
        this.recommendUrl = recommendUrl;
    }
}
