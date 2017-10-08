package com.blink.browser.download;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import com.blink.browser.Browser;
import com.blink.browser.BrowserSettings;
import com.blink.browser.Controller;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.support.WinkEvent;
import com.blink.browser.util.ActivityUtils;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.StorageUtils;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.view.ToastPopView;
import com.blink.browser.widget.BrowserDialog;

import com.tcl.framework.network.http.NetworkError;
import com.tcl.framework.notification.NotificationCenter;
import com.tcl.framework.notification.Subscriber;
import com.wcc.wink.Resource;
import com.wcc.wink.Wink;
import com.wcc.wink.WinkError;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.request.SimpleURLResource;

import java.io.File;
import java.util.List;

/**
 * Created by wenhao on 2016/6/16.
 */
public class BrowserDownloadManager {
    private DownloadScanner mScanner;
    private ToastPopView mToast;
    private Activity mActivity;
    private Controller mController;

    public static BrowserDownloadManager getInstance() {
        return InstanceHolder.instance;
    }

    public static class InstanceHolder {
        static final BrowserDownloadManager instance = new BrowserDownloadManager();
    }


    public void onDestroy() {
        if (mScanner != null) {
            mScanner.shutdown();
        }
        if (mToast != null && mActivity != null && !mActivity.isFinishing()) {
            mToast.dismiss();
            mToast = null;
        }
        mActivity = null;
    }

    public void apkInfo(String absPath, Context context, DownloadInfo entity) {
        if (context == null) return;

        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            /* 必须加这两句，不然下面icon获取是default icon而不是应用包的icon */
            appInfo.sourceDir = absPath;
            appInfo.publicSourceDir = absPath;
            String appName = pm.getApplicationLabel(appInfo).toString();// 得到应用名
            Drawable icon1 = pm.getApplicationIcon(appInfo);// 得到图标信息
            Drawable icon2 = appInfo.loadIcon(pm);

            if (icon1 != null) {
                entity.setApkIcon(icon1);
            } else if (icon2 != null) {
                entity.setApkIcon(icon2);
            }

            if (!TextUtils.isEmpty(appName)) {
                entity.setApkName(appName);
            }
        }
    }

    public boolean isExternalStorageWritable(final Context context, String apkname) {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            final int title;
            final String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = context.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = context.getString(R.string.download_no_sdcard_dlg_msg, apkname);
                title = R.string.download_no_sdcard_dlg_title;
            }

            Handler handler = new Handler(context.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    new BrowserDialog(context)
                            .setBrowserTitle(title)
                            .setBrowserMessage(msg)
                            .setBrowserPositiveButton(R.string.ok)
                            .show();
                }
            });

            return false;
        }
        return true;
    }


    public void startDownload(final Activity activity, final String url,
                              String userAgent, String contentDisposition, final String mimetype,
                              String referer, boolean privateBrowsing, String filename, Controller controller) {
        if (!StorageUtils.isSDExist(activity, BrowserSettings.getInstance().getDownloadPath())) {
            BrowserSettings.getInstance().setDownloadPath(FileUtils.getLocalDir());
        }

        if (mActivity != null && !activity.equals(mActivity)) {
            if (mToast != null) {
                mToast.dismiss();
                mToast = null;
            }
        }
        mActivity = activity;

        mController = controller;
        NotificationCenter.defaultCenter().subscriber(WinkEvent.class, mEventSubscriber);

        if (mScanner == null) {
            mScanner = new DownloadScanner(Browser.getInstance().getApplicationContext());
        }

        int err = Wink.get().wink(url, filename, mimetype, referer);
        downloadError(err, mActivity, url, mimetype, filename, controller);
    }


    private void downloadError(final int err, final Activity activity, final String url,
                               final String mimetype, final String apkname, final Controller controller) {

        Handler handler = new Handler(activity.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (err == WinkError.SUCCESS) {
                    if (controller != null) {
                        controller.showDownloadAnimation();
                    }
                    showPopToast(activity, R.string.downloading, false);
                } else if (err == WinkError.EXIST) {
                    ToastUtil.showLongToastByString(activity, apkname + " " + activity.getString(R.string
                            .downloading) + "?");
                } else if (err == WinkError.ALREADY_COMPLETED) {
                    BrowserDialog dialog = new BrowserDialog(activity, R.style.DownloadDialog) {
                        @Override
                        public void onPositiveButtonClick() {
                            super.onPositiveButtonClick();

                            try {
                                Wink.get().delete(new SimpleURLResource(url).getKey(), true);
                                int err = Wink.get().wink(url, apkname);
                                downloadError(err, activity, url, mimetype, apkname, controller);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onNegativeButtonClick() {
                            super.onNegativeButtonClick();
                        }
                    };
                    dialog.setBrowserMessage(apkname + " " + activity.getResources().getString(R.string.downloaded));
                    dialog.setBrowserPositiveButton(R.string.replace_file);
                    dialog.setBrowserNegativeButton(R.string.cancel);
                    dialog.show();
                } else if (err == WinkError.INSUFFICIENT_SPACE) {
                    ToastUtil.showLongToast(activity, R.string.storage_space_lack);
                } else if (err == NetworkError.NO_AVALIABLE_NETWORK) {
                    ToastUtil.showLongToast(activity, R.string.check_network);
                }
            }
        });

    }

    private Subscriber<WinkEvent> mEventSubscriber = new Subscriber<WinkEvent>() {
        @Override
        public void onEvent(WinkEvent event) {
            if (mScanner != null && event.event == WinkEvent.EVENT_STATUS_CHANGE && event.entity.getDownloadState()
                    == ResourceStatus.DOWNLOADED) {
                mScanner.requestScan(event.entity);
                showPopToast(mActivity, R.string.downloaded, true);
                Resource res = event.entity.getResource();
                if (res instanceof SimpleURLResource) {
                    String mime = ((SimpleURLResource) res).getMimeType();
                    if ("application/vnd.android.package-archive".equalsIgnoreCase(mime)) {
                        installApk(event.entity.getLocalFilePath());
                    }
                }
            }
        }
    };


    private void showPopToast(final Activity activity, final int text, final boolean downloaded) {
        if (mActivity == null) return;
        Handler handler = new Handler(activity.getMainLooper());
        if (mToast == null) {
            mToast = new ToastPopView(activity);
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (activity == null || activity.isFinishing() || mToast == null) return;
                    mToast.dismiss();
                }
            });
        }

        int height = 0;
        if (mController != null) height = mController.toolBarHeight();

        final int barHeight = height;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) return;
                mToast.setText(text).setTextOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mToast.dismiss();
                        mToast = null;
                        if (downloaded) {
                            ActivityUtils.startDownloadActivity(activity, DownloadActivity.DOWNLOADED);
                        } else {
                            ActivityUtils.startDownloadActivity(activity, DownloadActivity.DOWNLOADING);
                        }
                    }
                });
                if (activity != null && activity.isFinishing()) {
                    try {
                        mToast.show(activity.getCurrentFocus(), barHeight);
                    } catch (Exception e) {
                    }
                }
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (activity != null && !activity.isFinishing() && mToast != null) {
                    mToast.dismiss();
                    mToast = null;
                }
            }
        }, 4000);
    }

    public void downloadAction(String key, int state) {
        if (key == null) return;

        DownloadInfo info = getDownloadInfo(key);
        switch (state) {
            case ResourceStatus.WAIT:
            case ResourceStatus.DOWNLOADING:
                if (info != null) {
                    Wink.get().stop(key);
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings
                            .ID_PAUSE);
                }
                break;
            case ResourceStatus.DOWNLOAD_FAILED:
                if (info != null) {
                    Wink.get().wink(info.getResource());
                }
                break;
            case ResourceStatus.INIT:
            case ResourceStatus.PAUSE:
                if (info != null) {
                    Wink.get().wink(info.getResource());
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings
                            .ID_CONTINUE);
                }
                break;
            case ResourceStatus.DELETED:
                break;
            case ResourceStatus.DOWNLOADED:
                break;
        }
    }

    public DownloadInfo getDownloadInfo(String key) {
        Wink wink = Wink.get();
        List<DownloadInfo> download = wink.getDownloadingResources();
        if (download == null) return null;

        for (DownloadInfo data : download) {
            if (key.equals(data.getKey())) {
                return data;
            }
        }
        return null;
    }

    private void installApk(String filename) {
        if (mActivity == null || TextUtils.isEmpty(filename)) return;
        File file = new File(filename);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        String type = "application/vnd.android.package-archive";
        intent.setDataAndType(Uri.fromFile(file), type);
        mActivity.startActivity(intent);
    }

}
