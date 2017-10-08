package com.blink.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.BrowserDownloadManager;
import com.blink.browser.provider.BrowserContract;

public class BrowserReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (BrowserContract.DOWNLOAD_VIEW_ACTION.equals(action)) {
            String key = intent.getStringExtra(BrowserContract.DOWNLOAD_KEY);
            int state = intent.getIntExtra(BrowserContract.DOWNLOAD_STATE, 0);
            BrowserDownloadManager.getInstance().downloadAction(key, state);
        }
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            BrowserAnalytics.createAnalytics(context);
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.NETWORKSWITCH_EVENTS, AnalyticsSettings.ID_NETWORKSWITCH);
        }
    }
}
