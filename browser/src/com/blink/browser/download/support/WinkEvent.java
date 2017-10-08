package com.blink.browser.download.support;

import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.DownloadState;

public class WinkEvent {
    public static final int EVENT_STATUS_CHANGE = 1;
    public static final int EVENT_PROGRESS = 2;

    public final DownloadInfo entity;
    public final int event;
    /**
     * download state
     */
    public DownloadState state;

    WinkEvent(int event, DownloadInfo entity) {
        this.event = event;
        this.entity = entity;
    }

    /**
     * create wink event
     * @param entity
     * @param state
     * @return
     */
    public static WinkEvent statusChanged(DownloadInfo entity, DownloadState state) {
        WinkEvent winkEvent = new WinkEvent(EVENT_STATUS_CHANGE, entity);
        winkEvent.state = state;
        return winkEvent;
    }

    static WinkEvent progress(DownloadInfo entity) {
        return new WinkEvent(EVENT_PROGRESS, entity);
    }
}
