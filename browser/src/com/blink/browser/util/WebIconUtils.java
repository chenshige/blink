package com.blink.browser.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.blink.browser.provider.BrowserContract;

/**
 * 网站icon的公共工具类
 */

public class WebIconUtils {

    public static Bitmap getWebIconFromLocalDb(Context context, String url) {
        final ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(BrowserContract.Images.CONTENT_URI,
                    new String[]{BrowserContract.Images.URL, BrowserContract.Images.TOUCH_ICON},
                    BrowserContract.Images.URL + "=?", new String[]{url}, null);
            if (cursor != null && cursor.moveToFirst()) {
                byte[] imageIcon = cursor.getBlob(1);
                if (imageIcon != null) {
                    return ImageUtils.decodeByteToBitmap(imageIcon);
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

}
