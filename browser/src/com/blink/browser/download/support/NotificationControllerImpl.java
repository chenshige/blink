package com.blink.browser.download.support;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.blink.browser.BrowserReceiver;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.DownloadActivity;
import com.blink.browser.provider.BrowserContract;
import com.tcl.framework.network.http.NetworkError;
import com.wcc.wink.Resource;
import com.wcc.wink.WinkError;
import com.wcc.wink.WinkNotificationController;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.util.Utils;

public class NotificationControllerImpl implements WinkNotificationController {
    private final static int GLOBAL_ERROR_NOTIFICATION_ID = 0x1234;
    final Context mContext;
    NotificationManager mNotificationManager;
    Resources mRes;

    public NotificationControllerImpl(Context context) {
        this.mContext = context.getApplicationContext();
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mRes = mContext.getResources();
    }

    @Override
    public void showCompletedNotification(Resource di) {
        Intent notificationIntent = new Intent(mContext, DownloadActivity.class);
        notificationIntent.setAction(BrowserContract.DOWNLOAD_ITEM_ACTION);
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_KEY, di.getKey());
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_STATE, DownloadActivity.DOWNLOADED);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        int icon = R.mipmap.ic_launcher_browser;
        if (Build.VERSION.SDK_INT > 20) {
            icon = R.drawable.ic_browser_menu_download_incognito;
        }
        String title = di.getTitle();
        String contentText = mRes.getString(R.string.downloaded);
        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_progress);
        remoteView.setTextViewText(R.id.title, title);
        remoteView.setTextViewText(R.id.message, contentText);
        remoteView.setViewVisibility(R.id.progress, View.GONE);
        remoteView.setImageViewResource(R.id.icon, R.mipmap.ic_launcher_browser);
        remoteView.setViewVisibility(R.id.action, View.GONE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= 16) {
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        if (Build.VERSION.SDK_INT >= 10) {
            builder.setContent(remoteView);
        }

        Notification notification = builder.build();
        notification.sound = null;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.vibrate = null;

        if (Build.VERSION.SDK_INT < 10) {
            notification.contentView = remoteView;
        }

        int id = di.getKey().hashCode();
        if (id < 0)
            id += Integer.MAX_VALUE;

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    private void showInsufficientSpaceNotification() {
        int id = GLOBAL_ERROR_NOTIFICATION_ID;
        Intent notificationIntent = new Intent(mContext, DownloadActivity.class);
        notificationIntent.setAction(BrowserContract.DOWNLOAD_ITEM_ACTION);
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_STATE, DownloadActivity.DOWNLOADING);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        int icon = R.mipmap.ic_launcher_browser;
        if (Build.VERSION.SDK_INT > 20) {
            icon = R.drawable.ic_browser_menu_download_incognito;
        }
        String title = mRes.getString(R.string.storage_space_lack);

        String contentText = mRes.getString(R.string.notification_content_text);

        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_progress);
        remoteView.setTextViewText(R.id.title, title);
        remoteView.setTextViewText(R.id.message, contentText);
        remoteView.setViewVisibility(R.id.progress, View.GONE);
        remoteView.setImageViewResource(R.id.icon, R.mipmap.ic_launcher_browser);
        remoteView.setViewVisibility(R.id.action, View.GONE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= 16) {
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        if (Build.VERSION.SDK_INT >= 10) {
            builder.setContent(remoteView);
        }

        Notification notification = builder.build();
        notification.sound = null;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.vibrate = null;

        if (Build.VERSION.SDK_INT < 10) {
            notification.contentView = remoteView;
        }
        mNotificationManager.notify(id, notification);
    }

    @Override
    public void showErrorNotification(Resource di, int err) {
        if (di == null) {
            return;
        }

        int id = di.getKey().hashCode();


        if (id < 0)
            id += Integer.MAX_VALUE;

        if (err == NetworkError.CANCEL) {
//            mNotificationManager.cancel(id);
            return;
        }

        if (err == WinkError.INSUFFICIENT_SPACE) {
            showInsufficientSpaceNotification();
        }

        Intent notificationIntent = new Intent(mContext, DownloadActivity.class);
        notificationIntent.setAction(BrowserContract.DOWNLOAD_ITEM_ACTION);
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_KEY, di.getKey());
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_STATE, DownloadActivity.DOWNLOADING);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        int icon = R.mipmap.ic_launcher_browser;
        if (Build.VERSION.SDK_INT > 20) {
            icon = R.drawable.ic_browser_menu_download_incognito;
        }
        String title = di.getTitle();

        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_progress);
        String contentText = mRes.getString(R.string.download_failed);
        int progress = 0;
        remoteView.setImageViewResource(R.id.action, R.drawable.ic_browser_download_reload);
        remoteView.setTextViewText(R.id.title, title);
        remoteView.setTextViewText(R.id.message, contentText);
        remoteView.setViewVisibility(R.id.progress, View.GONE);
        remoteView.setOnClickPendingIntent(R.id.action, getPendingIntent(di.getKey(), ResourceStatus.DOWNLOAD_FAILED));
        remoteView.setProgressBar(R.id.progress, 100, progress, false);
        remoteView.setImageViewResource(R.id.icon, R.mipmap.ic_launcher_browser);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= 16) {
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        if (Build.VERSION.SDK_INT >= 10) {
            builder.setContent(remoteView);
        }

        Notification notification = builder.build();
        notification.sound = null;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.vibrate = null;

        if (Build.VERSION.SDK_INT < 10) {
            notification.contentView = remoteView;
        }

        mNotificationManager.notify(id, notification);

        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings.ID_DOWNLOADFAIL);
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADFAIL_EVENTS, AnalyticsSettings.ID_URL, di.getUrl());
    }

    @Override
    public void showDeletedNotification(Resource resource) {
        int id = resource.getKey().hashCode();
        if (id < 0)
            id += Integer.MAX_VALUE;

        mNotificationManager.cancel(id);
    }

    @Override
    public void showProgressNotification(Resource resource, int progress) {
        Intent notificationIntent = new Intent(mContext, DownloadActivity.class);
        notificationIntent.setAction(BrowserContract.DOWNLOAD_ITEM_ACTION);
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_KEY, resource.getKey());
        notificationIntent.putExtra(BrowserContract.DOWNLOAD_STATE, DownloadActivity.DOWNLOADING);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        int icon = R.mipmap.ic_launcher_browser;
        if (Build.VERSION.SDK_INT > 20) {
            icon = R.drawable.ic_browser_menu_download_incognito;
        }
        String title = resource.getTitle();

        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_progress);
        String contentText = mRes.getString(R.string.downloading);
        if (resource instanceof DownloadInfo) {
            DownloadInfo info = (DownloadInfo) resource;
            String dsizeText = Utils.convertFileSize(info.getDownloadedSizeInBytes());
            String tsizeText = Utils.convertFileSize(info.getTotalSizeInBytes());
            contentText = mRes.getString(R.string.download_size_text_format, dsizeText, tsizeText);
            if (info.getTotalSizeInBytes() <= 0 && info.getDownloadedSizeInBytes() > 0) {
                contentText = dsizeText;
            }

            updateActionState(info, remoteView);
        }
        remoteView.setTextViewText(R.id.title, title);
        remoteView.setTextViewText(R.id.message, contentText);
        remoteView.setViewVisibility(R.id.progress, View.VISIBLE);

        remoteView.setProgressBar(R.id.progress, 100, progress, false);
        remoteView.setImageViewResource(R.id.icon, R.mipmap.ic_launcher_browser);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= 16) {
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        if (Build.VERSION.SDK_INT >= 10) {
            builder.setContent(remoteView);
        }

        Notification notification = builder.build();
        notification.sound = null;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.vibrate = null;

        if (Build.VERSION.SDK_INT < 10) {
            notification.contentView = remoteView;
        }

        int id = resource.getKey().hashCode();
        if (id < 0)
            id += Integer.MAX_VALUE;

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    @Override
    public void showPauseNotification(Resource resource) {
        if (resource instanceof DownloadInfo) {
            DownloadInfo info = (DownloadInfo) resource;
            showProgressNotification(resource, info.getDownloadProgress());
        }
    }

    private void updateActionState(DownloadInfo data, RemoteViews remoteViews) {
        int state = data.getDownloadState();

        switch (state) {
            case ResourceStatus.WAIT:
            case ResourceStatus.DOWNLOADING:
                remoteViews.setImageViewResource(R.id.action, R.drawable.ic_browser_download_pause);
                break;
            case ResourceStatus.DOWNLOAD_FAILED:
                remoteViews.setImageViewResource(R.id.action, R.drawable.ic_browser_download_reload);
                break;
            case ResourceStatus.PAUSE:
            case ResourceStatus.INIT:
                remoteViews.setImageViewResource(R.id.action, R.drawable.ic_browser_download_play);
                break;
        }
        remoteViews.setOnClickPendingIntent(R.id.action, getPendingIntent(data.getKey(), state));
    }

    private PendingIntent getPendingIntent(String key, int state) {
        Intent onClickIntent = new Intent(mContext, BrowserReceiver.class);
        onClickIntent.setAction(BrowserContract.DOWNLOAD_VIEW_ACTION);
        onClickIntent.putExtra(BrowserContract.DOWNLOAD_KEY, key);
        onClickIntent.putExtra(BrowserContract.DOWNLOAD_STATE, state);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, onClickIntent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

}
