package com.blink.browser.bean;

/**
 * <p/>
 * searchEngine in BrowserSQLiteHelper SearchEngineEntity table
 */
public class SearchEngineEntity extends CommonEntity {

//    public String engine_name; //搜索引擎名称
//    public String engine_icon; //搜索引擎icon地址
//    public String imageUrl; //搜索引擎icon地址
    public String engine_url; //搜索引擎搜索url
    public String engine_order; //搜索引擎排序
    public String create_time; //搜索引擎创建时间

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String encoding; //编码方式
    public int is_default; //是否为默认引擎, 0为false， 1为true

    public String getEngine_order() {
        return engine_order;
    }

    public void setEngine_order(String engine_order) {
        this.engine_order = engine_order;
    }

    public String getEngine_url() {
        return engine_url;
    }

    public void setEngine_url(String engine_url) {
        this.engine_url = engine_url;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public int is_default() {
        return is_default;
    }

    public void setIs_default(int is_default) {
        this.is_default = is_default;
    }

    public SearchEngineEntity() {}

    public SearchEngineEntity(String[] arg) {
        if (arg == null || arg.length < 3) {
            throw new RuntimeException("Check common_url.xml !");
        }
    }

    public class Column extends CommonColumn {
        public static final String ID = "_id";
        public static final String ENGINE_NAME = "title";
        public static final String ENGINE_ICON = "imageicon";
        public static final String ENGINE_QUERY_Url = "engine_url";
        public static final String ENGINE_ORDER = "engine_order";
        public static final String CREATE_TIME = "create_time";
        public static final String IS_DEFAULT = "is_default";
        public static final String IMAGE_URL = "imageurl";
        public static final String URL = "url";
        public static final String ENCODING = "encoding";
    }

    public String toString(){
        StringBuffer buf = new StringBuffer();
        buf.append("{").append("id:").append(getId())
           .append(",url:").append(engine_url)
           .append(",engineOrder:").append(engine_order)
           .append(",mIsDefault:").append(is_default)
           .append(",imageUrl:").append(getImageIcon())
           .append(",displayName:").append(getTitle()).append("}") ;
        return buf.toString();
    }
}
