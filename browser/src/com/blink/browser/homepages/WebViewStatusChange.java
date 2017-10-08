package com.blink.browser.homepages;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.blink.browser.DatabaseManager;
import com.blink.browser.bean.InputUrlEntity;
import com.blink.browser.database.SqlBuild;

public class WebViewStatusChange {
    public static final int RECEIVED_TITLE = 1;
    public static final int RECEIVED_TOUCHICOM = 1 << 1;

    public int mReceiveStatus = RECEIVED_TITLE | RECEIVED_TOUCHICOM;
    public long mInputId = -1;
    private String mInputUrl;

    public WebViewStatusChange(int receivedType) {
        mReceiveStatus = receivedType;
    }

    public WebViewStatusChange(String inputUrl) {
        mInputUrl = inputUrl;
    }

    /**
     * 能够成功加载dom获取到的网页标题信息
     *
     * @param url
     * @param origUrl
     * @param title
     */
    public void onReceivedTitle(String url, String origUrl, String title) {
        if (TextUtils.isEmpty(mInputUrl)) {
            return;
        }
        if ((mReceiveStatus & RECEIVED_TITLE) == RECEIVED_TITLE) {
            Cursor cursor = DatabaseManager.getInstance().findBySql(String.format(SqlBuild.INPUT_URL_CHECK), new String[]{mInputUrl});
            long id = -1, count = -1;
            long now = System.currentTimeMillis();
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                    count = cursor.getLong(1);
                }
                if (id > 0) {
                    ContentValues values = new ContentValues();
                    values.put(InputUrlEntity.Column.INPUTCOUNT, ++count);
                    values.put(InputUrlEntity.Column.MODIFIED_TIME, now);
                    DatabaseManager.getInstance().updateById(InputUrlEntity.class, values, id);
                    return;
                }

                InputUrlEntity inputUrl = new InputUrlEntity();
                inputUrl.setUrl(url);
                inputUrl.setTitle(title);
                inputUrl.setInputWord(mInputUrl);
                inputUrl.setCount(0);
                inputUrl.setInputTime(now);
                inputUrl.setModifiedTime(now);
                mInputId = DatabaseManager.getInstance().insert(inputUrl);
                mReceiveStatus &= ~RECEIVED_TITLE;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /**
     * 获取网页的touchicon,
     * <link rel="apple-touch-icon" href="/custom_icon.png">
     * <link rel="apple-touch-icon" sizes="76x76" href="/custom_icon.png">
     * <link rel="apple-touch-icon-precomposed" sizes="72x72" href="/custom_icon.png">
     *
     * @param iconUrl
     */
    public void onReceivedTouchIconUrl(String iconUrl) {
        if ((mReceiveStatus & RECEIVED_TOUCHICOM) == RECEIVED_TOUCHICOM) {
            if (mInputId != -1) {
                ContentValues values = new ContentValues();
                values.put(InputUrlEntity.Column.IMAGE_URL, iconUrl);
                DatabaseManager.getInstance().updateById(InputUrlEntity.class, values, mInputId);
            }
            mReceiveStatus &= ~RECEIVED_TOUCHICOM;
        }
    }
}
