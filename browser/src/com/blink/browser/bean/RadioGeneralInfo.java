package com.blink.browser.bean;

/**
 *  contain plugin_state、default_zoom、default_text_encoding、preload_when、link_prefetch_when
 */
public class RadioGeneralInfo {
    private String key;
    private String values;
    private boolean selected;

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public String getValues(){
        return values;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected(){
        return selected;
    }
}
