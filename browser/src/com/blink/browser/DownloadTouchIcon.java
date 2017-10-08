/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blink.browser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.webkit.WebView;

import com.blink.browser.provider.BrowserContract;
import com.blink.browser.provider.BrowserContract.Images;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

class DownloadTouchIcon extends AsyncTask<String, Void, Void> {
    private static final String TAG = "DownloadTouchIcon";

    private final ContentResolver mContentResolver;
    private Cursor mCursor;
    private final String mOriginalUrl;
    private final String mUrl;
    private final String mUserAgent; // Sites may serve a different icon to different UAs
    private Message mMessage;

    /* package */ Tab mTab;

    /**
     * Use this ctor to store the touch icon in the bookmarks database for
     * the originalUrl so we take account of redirects. Used when the user
     * bookmarks a page from outside the bookmarks activity.
     */
    public DownloadTouchIcon(Tab tab, ContentResolver cr, WebView view) {
        mTab = tab;
        mContentResolver = cr;
        // Store these in case they change.
        mOriginalUrl = view.getOriginalUrl();
        mUrl = view.getUrl();
        mUserAgent = view.getSettings().getUserAgentString();
    }

    /**
     * Use this ctor to download the touch icon and update the bookmarks database
     * entry for the given url. Used when the user creates a bookmark from
     * within the bookmarks activity and there haven't been any redirects.
     * TODO: Would be nice to set the user agent here so that there is no
     * potential for the three different ctors here to return different icons.
     */
    public DownloadTouchIcon(ContentResolver cr, String url) {
        mTab = null;
        mContentResolver = cr;
        mOriginalUrl = null;
        mUrl = url;
        mUserAgent = null;
    }

    /**
     * Use this ctor to not store the touch icon in a database, rather add it to
     * the passed Message's data bundle with the key
     * {@link BrowserContract.Bookmarks#TOUCH_ICON} and then send the message.
     */
    public DownloadTouchIcon(Message msg, String userAgent) {
        mMessage = msg;
        mContentResolver = null;
        mOriginalUrl = null;
        mUrl = null;
        mUserAgent = userAgent;
    }

    @Override
    public Void doInBackground(String... values) {
        if (mContentResolver != null) {
            mCursor = Bookmarks.queryCombinedForUrl(mContentResolver,
                    mOriginalUrl, mUrl);
        }

        boolean inDatabase = mCursor != null && mCursor.getCount() > 0;

        if (inDatabase || mMessage != null) {
            Logger.info(TAG, "download touch icon " + values[0]);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(values[0]);
                URLConnection urlConnection = url.openConnection();
                if (urlConnection instanceof HttpURLConnection) {
                    connection = (HttpURLConnection) urlConnection;
                    if (mUserAgent != null) {
                        connection.addRequestProperty("User-Agent", mUserAgent);
                    }

                    if (connection.getResponseCode() == 200) {
                        InputStream content = connection.getInputStream();
                        Bitmap icon = null;
                        try {
                            byte[] bytes = readBytes(content);
                            if (bytes.length < BuildUtil.WEB_SITE_ICON_LENGHT) {
                                icon = ImageUtils.decodeByteToBitmap(bytes);
                            }
                        } finally {
                            try {
                                content.close();
                            } catch (IOException ignored) {
                            }
                        }

                        if (inDatabase) {
                            storeIcon(icon);
                        } else if (mMessage != null) {
                            Bundle b = mMessage.getData();
                            b.putParcelable(BrowserContract.Bookmarks.TOUCH_ICON, icon);
                        }
                        icon.recycle();
                        icon = null;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (mCursor != null) {
                    mCursor.close();
                }
            }
        }

        if (mMessage != null) {
            mMessage.sendToTarget();
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    private void storeIcon(Bitmap icon) {
        // Do this first in case the download failed.
        if (mTab != null) {
            // Remove the touch icon loader from the BrowserActivity.
            mTab.mTouchIconLoader = null;
        }

        if (icon == null || mCursor == null || isCancelled()) {
            return;
        }

        if (mCursor.moveToFirst()) {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, os);

            ContentValues values = new ContentValues();
            if (os.toByteArray().length < BuildUtil.WEB_SITE_ICON_LENGHT) {
                values.put(Images.TOUCH_ICON, os.toByteArray());
            }

            do {
                values.put(Images.URL, mUrl);//mCursor.getString(0)); just save the Touch Icon for mUrl not for the mOriginalUrl
                mContentResolver.update(Images.CONTENT_URI, values, null, null);
            } while (mCursor.moveToNext());
        }
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
}
