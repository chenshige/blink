package com.blink.browser.download.support;

import com.tcl.framework.notification.NotificationCenter;
import com.wcc.wink.WinkCallback;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.DownloadState;

public class SimpleWinkCallback implements WinkCallback {
    @Override
    public void onAdd(DownloadInfo... entities) {
        NotificationCenter.defaultCenter().publish(
                new WinkManageEvent(WinkManageEvent.EVENT_ADD, entities));
    }

    @Override
    public void onRemove(DownloadInfo... entities) {
        NotificationCenter.defaultCenter().publish(
                new WinkManageEvent(WinkManageEvent.EVENT_DELETE, entities));
    }

    @Override
    public void onClear() {
        NotificationCenter.defaultCenter().publish(
                new WinkManageEvent(WinkManageEvent.EVENT_CLEAR, null));
    }

    @Override
    public void onProgressChanged(DownloadInfo entity) {
        NotificationCenter.defaultCenter().publish(WinkEvent.progress(entity));
    }

    @Override
    public void onStatusChanged(DownloadInfo entity, DownloadState state) {
        NotificationCenter.defaultCenter().publish(WinkEvent.statusChanged(entity, state));
    }
}
