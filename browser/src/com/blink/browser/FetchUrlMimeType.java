/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.blink.browser.download.BrowserDownloadManager;
import com.blink.browser.util.FileUtils;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.wcc.wink.Wink;
import com.wcc.wink.util.Connections;

import java.io.IOException;


/**
 * This class is used to pull down the http headers of a given URL so that
 * we can analyse the mimetype and make any correction needed before we give
 * the URL to the download manager.
 * This operation is needed when the user long-clicks on a link or image and
 * we don't know the mimetype. If the user just clicks on the link, we will
 * do the same steps of correcting the mimetype down in
 * android.os.webkit.LoadListener rather than handling it here.
 */
class FetchUrlMimeType extends Thread {
    private Controller mTarget;
    private Context mContext;
    private String mUri;
    private String mCookies;
    private String mUserAgent;
    private String mFileName;
    private String mReferer;
    private OkHttpClient mClient;

    public FetchUrlMimeType(Context context, String uri, String cookies, String userAgent, String filename, String
            referer, Controller target) {
        mContext = context;
        mUri = uri;
        mCookies = cookies;
        mUserAgent = userAgent;
        mFileName = filename;
        mReferer = referer;
        mTarget = target;
    }

    @Override
    public void run() {
        Request.Builder builder = null;
        try {
            builder = new Request.Builder().url(mUri);
        } catch (Exception e) {
            e.printStackTrace();
            builder = null;
        }
        if (builder == null) return;

        final String userAgent = Wink.get().getSetting().getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) {
            builder.header("User-Agent", userAgent);
        }
        if (!TextUtils.isEmpty(mReferer)) {
            builder.header("Referer", mReferer);
        }
        if (!TextUtils.isEmpty(mCookies)) {
            builder.addHeader("cookie", mCookies);
        }

        mClient = Connections.getOkHttpClient();
        final Call call = mClient.newCall(builder.build());
        String mimeType = "";
        String contentDisposition = null;
        Response response = null;
        try {
            response = call.execute();
            if (response.isSuccessful()) {
                mimeType = response.header("Content-Type", null);
                contentDisposition = response.header("Content-Disposition");

                if (mimeType != null) {
                    if (mimeType.contains(";charset=")) {
                        mimeType = mimeType.substring(0, mimeType.indexOf(";charset="));
                    }

                    if (mimeType.equalsIgnoreCase("text/plain") ||
                            mimeType.equalsIgnoreCase("application/octet-stream")) {
                        String newMimeType =
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                        MimeTypeMap.getFileExtensionFromUrl(mUri));
                        if (!TextUtils.isEmpty(newMimeType)) {
                            mimeType = newMimeType;
                        }
                    }

                    mFileName = FileUtils.getApkPath(mUri, contentDisposition, mimeType);
                    String specialEx = "[`~!@#$%^&*()+=|{}':;',\\[\\]<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
                    mFileName = mFileName.replaceAll(specialEx, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        BrowserDownloadManager.getInstance().startDownload((Activity) mContext, mUri, mUserAgent,
                contentDisposition,
                mimeType, mReferer, false, mFileName, mTarget);
    }
}
