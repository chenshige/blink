package com.blink.browser.download;

import android.content.Context;

import com.blink.browser.BrowserSettings;
import com.blink.browser.download.support.DownloadStat;
import com.blink.browser.download.support.NotificationControllerImpl;
import com.blink.browser.download.support.SimpleWinkCallback;
import com.blink.browser.download.support.WinkNetworkSensor;
import com.wcc.wink.Wink;
import com.wcc.wink.WinkBuilder;
import com.wcc.wink.module.WinkModule;

public class BrowserWinkModule implements WinkModule {

    @Override
    public void applyOptions(Context context, WinkBuilder builder) {
        String userAgent = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 6P Build/MTC19T; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.117 " +
                "Mobile Safari/537.36";
        builder.setUserAgent(userAgent);
        builder.setNeedCheckLengthBeforeDownload(true);
        builder.setNetworkSensor(new WinkNetworkSensor());
        builder.setSimpleResourceStoragePath(BrowserSettings.getInstance().getDownloadPath());
    }

    @Override
    public void registerComponents(Context context, Wink wink) {
        wink.setNotificationController(new NotificationControllerImpl(context));
        wink.setCallback(new SimpleWinkCallback());
        wink.setStatHandler(new DownloadStat());
    }
}
