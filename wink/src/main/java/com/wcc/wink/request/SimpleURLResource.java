package com.wcc.wink.request;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.wcc.wink.Resource;
import com.wcc.wink.model.Copyable;
import com.wcc.wink.util.MD5Util;

/**
 * Created by wenbiao.xie on 2016/10/27.
 */

public class SimpleURLResource implements Resource, Copyable<SimpleURLResource> {

    private int id;
    String mURL;
    String mTitle;
    String mKey;
    String mExt;
    String mMimeType;
    String mReferer;

    public SimpleURLResource() {
    }

    public SimpleURLResource(String url) {
        this.mURL = url;
        parse();
    }

    public SimpleURLResource(String url, String title) {
        this.mURL = url;
        this.mTitle = title;
        parse();
    }

    public SimpleURLResource(String url, String title, String mimeType) {
        this.mURL = url;
        this.mTitle = title;
        this.mMimeType = mimeType;
        this.parse();
    }

    public SimpleURLResource(String url, String title, String mimeType, String referer) {
        this.mURL = url;
        this.mTitle = title;
        this.mMimeType = mimeType;
        this.mReferer = referer;
        this.parse();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private void parse() {
        mExt = MimeTypeMap.getFileExtensionFromUrl(mURL);
        if (!TextUtils.isEmpty(mExt) && TextUtils.isEmpty(mMimeType)) {
            mMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mExt);
        }
        Uri uri = Uri.parse(mURL);
        mKey = "url_" + createKey(uri);

        if (!TextUtils.isEmpty(mTitle))
            return;

        String path = uri.getPath();

        if (TextUtils.isEmpty(path)) {
            mTitle = "/";
            return;
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (TextUtils.isEmpty(path)) {
            mTitle = "/";
            return;
        }

        int index = path.lastIndexOf('/');
        if (index < 0) {
            mTitle = path;
        } else {
            mTitle = path.substring(index + 1);
        }
    }

    private String createKey(Uri uri) {
        String ssp = uri.getSchemeSpecificPart();
        try {
            byte[] bytes = MD5Util.encode16(ssp, "utf-8");
            return MD5Util.toHexString(bytes);
        } catch (Exception e) {
            return ssp;
        }
    }

    public void setMimeType(String mMimeType) {
        this.mMimeType = mMimeType;
    }

    public String getMimeType() {
        return mMimeType;
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getUrl() {
        return mURL;
    }

    public String getReferer() {
        return mReferer;
    }

    @Override
    public void copyTo(SimpleURLResource target) {
        target.mURL = this.mURL;
        target.mKey = this.mKey;
        target.mTitle = this.mTitle;
        target.mMimeType = this.mMimeType;
        target.mExt = this.mExt;
        target.mReferer = this.mReferer;
        target.setId(this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SimpleURLResource that = (SimpleURLResource) o;

        if (!TextUtils.equals(mURL, that.mURL))
            return false;

        return true;

    }

    @Override
    public int hashCode() {
        return mURL != null ? mURL.hashCode() : 0;
    }
}
