package com.blink.browser.download.ui;

import android.content.Context;
import android.view.View;

import com.blink.browser.R;
import com.wcc.wink.Resource;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.request.SimpleURLResource;
import com.wcc.wink.util.Utils;

import java.util.Locale;

public class DownloadedItemHolder extends DownloadedAdapter.ViewHolder {

    public final static int SECONDS_PER_HOUR = 3600;
    public final static int SECONDS_PER_MINUTE = 60;

    public DownloadedItemHolder(Context context, View itemView) {
        super(context, itemView);
    }

    public static String formatTimeInSecs(int secs) {
        int hour = secs / SECONDS_PER_HOUR;
        secs = secs % SECONDS_PER_HOUR;
        int minute = secs / SECONDS_PER_MINUTE;
        secs = secs % SECONDS_PER_MINUTE;

        if (hour > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hour, minute, secs);
        } else {
            return String.format(Locale.US, "%02d:%02d", minute, secs);
        }
    }

    @Override
    public void bindView(DownloadInfo data) {
        setItemText(R.id.item_title, data.getTitle());

        String sizeText = Utils.convertFileSize(data.getTotalSizeInBytes());
        if (data.getDownloadState() == ResourceStatus.DELETED) {
            sizeText = getContext().getResources().getString(R.string.file_not_exist);
        }

        setItemText(R.id.item_size, sizeText);
        setViewTag(R.id.item_action, data);
        setViewTag(R.id.item_container, data);
        Resource res = data.getResource();
        if (res instanceof SimpleURLResource) {
            String mime = ((SimpleURLResource) res).getMimeType();
            if ("application/vnd.android.package-archive".equalsIgnoreCase(mime) && data.getApkIcon() != null) {
                setItemIcon(R.id.item_icon, data.getApkIcon());
                setItemText(R.id.item_title, data.getApkName());
            } else {
                setItemIcon(R.id.item_icon, R.drawable.icon_downloaded_file);
            }
        }
    }

    @Override
    protected void attach(DownloadedAdapter adapter) {
        super.attach(adapter);
        setViewOnClickListener(R.id.item_action, adapter);
        setViewOnClickListener(R.id.item_container, adapter);
    }
}
