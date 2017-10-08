/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.blink.browser.download.BrowserDownloadManager;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.Logger;
import com.blink.browser.util.NetworkUtils;
import com.blink.browser.util.OtherAPPUtils;
import com.blink.browser.util.WebAddress;
import com.blink.browser.widget.BrowserDialog;

/**
 * Handle download requests
 */
public class DownloadHandler {

    private static final String LOGTAG = "DownloadHandler";

    /**
     * Notify the host application a download should be done, or that
     * the data should be streamed if a streaming viewer is available.
     *
     * @param activity           Activity requesting the download.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype           The mimetype of the content reported by the server
     * @param referer            The referer associated with the downloaded url
     * @param privateBrowsing    If the request is coming from a private browsing tab.
     */
    public static void onDownloadStart(Activity activity, String url,
                                       String userAgent, String contentDisposition, String mimetype,
                                       String referer, boolean privateBrowsing, Controller controller) {
        // if we're dealing wih A/V content that's not explicitly marked
        //     for download, check if it's streamable.
        String fileExtension = FileUtils.getFileExtensionFromUrl(url).toLowerCase();
        if (TextUtils.isEmpty(mimetype) && fileExtension != null) {
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        }

        if (fileExtension != null && fileExtension.equals("apk") && !TextUtils.isEmpty(mimetype) && !mimetype.equals
                ("application/vnd.android.package-archive")) {
            mimetype = "application/vnd.android.package-archive";
        }

        if (mimetype != null && !mimetype.equals("application/vnd.android.package-archive") && !mimetype.contains
                ("image") && (TextUtils.isEmpty
                (contentDisposition)
                || !contentDisposition.regionMatches(true, 0, "attachment", 0, 10))) {
            // query the package manager to see if there's a registered handler
            //     that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimetype);

            ResolveInfo info = activity.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            //play flv video will open a link with flv mimetype
            //do not send intent when open flv mimeType

            if (info != null && !"video/x-flv".equals(mimetype) && fileExtension != null && !fileExtension
                    .equalsIgnoreCase("exe") &&
                    !fileExtension.equalsIgnoreCase("aac") && !fileExtension.equalsIgnoreCase("midi") &&
                    !fileExtension.equalsIgnoreCase("mid") && !fileExtension.equalsIgnoreCase("xmidi")
                    && !fileExtension.equalsIgnoreCase("spm") && !fileExtension.equalsIgnoreCase("imy") &&
                    !fileExtension.equalsIgnoreCase("wav") && !fileExtension.equalsIgnoreCase("amr") &&
                    !fileExtension.equalsIgnoreCase("m4a") && !fileExtension.equalsIgnoreCase("mp4") &&
                    !fileExtension.equalsIgnoreCase("sdp") && (fileExtension.equalsIgnoreCase("3gp") && (url.contains
                    ("aac") || url.contains("amr_wb")))) {
                ComponentName myName = activity.getComponentName();
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.

                if (!myName.getPackageName().equals(info.activityInfo.packageName) || !myName.getClassName().equals
                        (info.activityInfo.name) && fileExtension != null
                        && !(fileExtension.equalsIgnoreCase("icp") || fileExtension.equalsIgnoreCase("cip"))) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        activity.startActivity(intent);
                        return;
                    } catch (ActivityNotFoundException ex) {
                        Logger.debug(LOGTAG, "activity not found for " + mimetype
                                + " over " + Uri.parse(url).getScheme(), ex);

                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }

        String apkname = FileUtils.getApkPath(url, contentDisposition, mimetype);
        String specialEx = "[`~!@#$%^&*()+=|{}':;',\\[\\]<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        apkname = apkname.replaceAll(specialEx, "");

        downloadDialog(activity, url, userAgent, contentDisposition,
                mimetype, referer, privateBrowsing, apkname, controller);
    }

    // This is to work around the fact that java.net.URI throws Exceptions
    // instead of just encoding URL's properly
    // Helper method for onDownloadStartNoStream
    private static String encodePath(String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (needed == false) {
            return path;
        }

        StringBuilder sb = new StringBuilder("");
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Notify the host application a download should be done, even if there
     * is a streaming viewer available for thise type.
     *
     * @param activity           Activity requesting the download.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype           The mimetype of the content reported by the server
     * @param referer            The referer associated with the downloaded url
     * @param privateBrowsing    If the request is coming from a private browsing tab.
     * @param controller
     */
    public static void onDownloadStartNoStream(final Activity activity,
                                               final String url, final String userAgent, final String
                                                       contentDisposition,
                                               final String mimetype, final String referer, final boolean
                                                       privateBrowsing, final String filename, final Controller
                                                       controller) {
        // Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
                title = R.string.download_no_sdcard_dlg_title;
            }

            new BrowserDialog(activity)
                    .setBrowserTitle(title)
                    .setBrowserMessage(msg)
                    .setBrowserPositiveButton(R.string.ok)
                    .show();
            return;
        }

        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        WebAddress webAddress;
        try {
            webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
        } catch (Exception e) {
            // This only happens for very bad urls, we want to chatch the
            // exception here
            Log.e(LOGTAG, "Exception trying to parse url:" + url);
            return;
        }

        String addressString = webAddress.toString();
        Uri uri = Uri.parse(addressString);
        String cookies = CookieManager.getInstance().getCookie(url);
        if (mimetype == null) {
            if (TextUtils.isEmpty(addressString)) {
                return;
            }
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            new FetchUrlMimeType(activity, addressString, cookies,
                    userAgent, filename, referer, controller).start();
        } else {
            new Thread("Browser download") {
                public void run() {
                    BrowserDownloadManager.getInstance().startDownload(activity, url, userAgent, contentDisposition,
                            mimetype, referer,
                            privateBrowsing, filename, controller);

                }
            }.start();
        }
    }

    private static void downloadDialog(final Activity activity, final String url,
                                       final String userAgent, final String contentDisposition, final String mimetype,
                                       final String referer, final boolean privateBrowsing, final String filename,
                                       final Controller target) {
        if (BrowserSettings.getInstance().getDownloadConfirm()) {
            Handler handler = new Handler(activity.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BrowserDialog dialog = new BrowserDialog(activity, R.style.DownloadDialog) {
                        @Override
                        public void onPositiveButtonClick() {
                            super.onPositiveButtonClick();
                            try {
                                download(activity, url, userAgent, contentDisposition,
                                        mimetype, referer, privateBrowsing, filename, target);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onNegativeButtonClick() {
                            super.onNegativeButtonClick();
                        }
                    };
                    dialog.setBrowserContentView(getCheckBoxView(activity, activity.getResources().getString(R.string
                            .download) + " " + filename, new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            BrowserSettings.getInstance().setDownloadConfirm(!isChecked);
                        }
                    }));
                    dialog.setBrowserPositiveButton(R.string.download);
                    dialog.setBrowserNegativeButton(R.string.cancel);
                    dialog.show();
                }
            });
        } else {
            try {
                download(activity, url, userAgent, contentDisposition,
                        mimetype, referer, privateBrowsing, filename, target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void checkNetwork(final Activity activity, final String url,
                                     final String userAgent, final String contentDisposition, final String mimetype,
                                     final String referer, final boolean privateBrowsing, final String filename,
                                     final Controller controller) {
        if (!NetworkUtils.isWifiConnect()) {
            Handler handler = new Handler(activity.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BrowserDialog dialog = new BrowserDialog(activity, R.style.DownloadDialog) {
                        @Override
                        public void onPositiveButtonClick() {
                            super.onPositiveButtonClick();

                            try {
                                onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                                        mimetype, referer, privateBrowsing, filename, controller);
                            } catch (Exception e) {
                            }
                        }

                        @Override
                        public void onNegativeButtonClick() {
                            super.onNegativeButtonClick();
                        }
                    };
                    dialog.setBrowserMessage(R.string.download_allow_mobile);
                    dialog.setBrowserPositiveButton(R.string.allow_download);
                    dialog.setBrowserNegativeButton(R.string.allow_download_no);
                    dialog.show();
                }
            });
        } else {
            try {
                onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                        mimetype, referer, privateBrowsing, filename, controller);
            } catch (Exception e) {
            }
        }
    }

    public static View getCheckBoxView(Context context, String text,
                                       CompoundButton.OnCheckedChangeListener l) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.checkbox_dialog, null);
        ((TextView) view.findViewById(R.id.content)).setText(text);
        CheckBox check = (CheckBox) view.findViewById(R.id.checkbox);
        if (null != l) {
            check.setOnCheckedChangeListener(l);
        }
        return view;
    }

    private static void download(final Activity activity, final String url,
                                 final String userAgent, final String contentDisposition, final String mimetype,
                                 final String referer, final boolean privateBrowsing, final String filename,
                                 final Controller target) {
        if (BrowserSettings.getInstance().getDownloadADM()) {
            if ((OtherAPPUtils.isAppInstalled(activity, OtherAPPUtils.ADM_APP))) {
                OtherAPPUtils.downloadWithADM(activity, url, mimetype);
            } else if (OtherAPPUtils.isAppInstalled(activity, OtherAPPUtils.ADM_APP_PRO)) {
                OtherAPPUtils.downloadWithADMPRO(activity, url, mimetype);
            } else {
                checkNetwork(activity, url, userAgent, contentDisposition,
                        mimetype, referer, privateBrowsing, filename, target);
            }
        } else {
            checkNetwork(activity, url, userAgent, contentDisposition,
                    mimetype, referer, privateBrowsing, filename, target);
        }
    }
}
