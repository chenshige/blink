package com.blink.browser.bean;

import com.blink.browser.database.IgnoreField;

/**
 * input url in BrowserSQLiteHelper inputentity table
 */
public class InputUrlEntity extends CommonEntity {
    public class Column extends CommonColumn {
        public static final String INPUTTIMT = "inputtime";
        public static final String INPUTCOUNT = "count";
        public static final String MODIFIED_TIME = "modifiedTime";
    }

    private long inputTime;
    private int count;
    @IgnoreField
    public boolean isSelected;
    private String inputWord;
    private long modifiedTime;

    public long getInputTime() {
        return inputTime;
    }

    public void setInputTime(long inputTime) {
        this.inputTime = inputTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getInputWord() {
        return inputWord;
    }

    public void setInputWord(String inputWord) {
        this.inputWord = inputWord;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public InputUrlEntity(String[] arg) {
        if (arg == null || arg.length < 3) {
            throw new RuntimeException("Check common_url.xml !");
        }
        this.setTitle(arg[0]);
        this.setUrl(arg[1]);
        this.setImageUrl(arg[2]);
    }

    public InputUrlEntity() {
    }
}
