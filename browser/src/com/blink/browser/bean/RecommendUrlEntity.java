package com.blink.browser.bean;

import com.blink.browser.database.IgnoreField;

/**
 * <p/>
 * recommend url in BrowserSQLiteHelper recommendentity table
 */
public class RecommendUrlEntity extends CommonEntity {
    public static final int VIEW_TYPE_NATIVE = 1;
    public static final int VIEW_TYPE_WEBVIEW = 2;
    public static final int VIEW_TYPE_ADD = 3;

    public static final int OPT_VIEW_STATUS_LONGPRESS = 1;
    public static final int OPT_VIEW_STATUS_UNLONGPRESS = 0;
    public static final int OPT_SUPPORT_STATUS_BOTH = 1;
    public static final int OPT_SUPPORT_STATUS_UNBOTH = 0;

    public static final int WEIGHT_HOT_WEBSITE = 1;
    public static final int WEIGHT_BOOKMARK_WEBSITE = 2;
    public static final int WEIGHT_RECOMMEND_WEBSITE = -1;
    public static final int WEIGHT_DEFAULT_WEBSITE = 0;

    //服务器端返回的是displayName,这里忽略title
    private String displayName;
    // 需要暂存不同国家推荐网址
    private String language;
    //1代表热门网站,2代表在书签或历史中保存有网站icon的网站,负数代表推荐位网站,0代表非推荐非热门网站
    private int weight = WEIGHT_DEFAULT_WEBSITE;
    private long sid;

    // 新增排序记录
    private int ord;

    /*
    是否允许长按
    1 是允许　（默认）
    0 不允许
     */
    @IgnoreField
    public int optLongPressStatus = OPT_VIEW_STATUS_LONGPRESS;

    /*
    长按后支持的状态，为了支持有些数据只支持发送到桌面
    1 全部支持 （全部支持）
    0 只支持发送到桌面
     */
    @IgnoreField
    public int optSupportStatus = OPT_SUPPORT_STATUS_BOTH;

    /**
     * 该属性用于判断当前的item的以何种方式打开View(webview,native...)
     */
    @IgnoreField
    public int viewType = VIEW_TYPE_WEBVIEW;

    public RecommendUrlEntity() {
    }

    public RecommendUrlEntity(String[] arg) {
        if (arg == null || arg.length < 3) {
            throw new RuntimeException("Check common_url.xml !");
        }
        this.setDisplayName(arg[0]);
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

    public long getSid() {
        return sid;
    }

    public void setSid(long sid) {
        this.sid = sid;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public class Column extends CommonColumn {
        public static final String DISPLAY_NAME = "displayname";
        public static final String WEIGHT = "weight";
        public static final String SID = "sid";
        public static final String ORD = "ord";
        public static final String LANGUAGE = "language";
    }

    public int getOrd() {
        return ord;
    }

    public void setOrd(int order) {
        this.ord = order;
    }

    public String toString() {
        return "{" + "id:" + getId() +
                ",ord:" + ord +
                ",url:" + getUrl() +
                ",weight:" + weight +
                ",language:" + language +
                ",imageUrl:" + getImageUrl() +
                ",displayName:" + getDisplayName() + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RecommendUrlEntity) {
            return this.getId() == ((RecommendUrlEntity) obj).getId();
        } else {
            return false;
        }
    }
}
