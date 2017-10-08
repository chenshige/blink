package com.blink.browser.database;

public class SqlBuild {
    /**
     * table inputurl
     */
    public static final String INPUT_URL_SQL = "select  inu._id, inu.title,inu.url,img.touch_icon imageicon from (select _id,title,url from " + BrowserSQLiteHelper.TABLE_INPUTURL + " order by count desc limit 8) inu left join images img on  inu.url=img.url_key";
    public static final String INPUT_URL_CHECK = "select _id ,count from " + BrowserSQLiteHelper.TABLE_INPUTURL + " where inputword = ?";

    /**
     * table recommend view
     */
    public static final String RECOMMEND_URL_SQL = "select rte._id _id, rte.displayname displayname,rte.weight weight ,rte.ord ord,rte.imageurl imageurl,rte.imageicon imageicon,rte.url url,img.touch_icon touch_icon,img.favicon favicon from (select _id,imageicon,weight,displayname,url,ord,imageurl from " + BrowserSQLiteHelper.TABLE_RECOMMEND_WEB_URL + " ) rte left join images img on rte.weight=0 and rte.url = img.url_key ";

    public static final String INPUT_word_SQL = "select _id,url,inputword from " + BrowserSQLiteHelper.TABLE_INPUTURL + " order by modifiedtime desc limit 5";

}
