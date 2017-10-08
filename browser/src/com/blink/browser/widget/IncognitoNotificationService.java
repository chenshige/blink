package com.blink.browser.widget;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.blink.browser.ActivityController;
import com.blink.browser.Browser;
import com.blink.browser.BrowserActivity;
import com.blink.browser.Controller;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.ThreadUtils;

import java.lang.ref.WeakReference;

public class IncognitoNotificationService extends IntentService {

    private static final String TAG = "incognito_notification";

    private static final String ACTION_CLOSE_ALL_INCOGNITO ="blink.action.CLOSE_ALL_INCOGNITO";

    public static PendingIntent getRemoveAllIncognitoTabsIntent(Context context) {
        Intent intent = new Intent(context, IncognitoNotificationService.class);
        intent.setAction(ACTION_CLOSE_ALL_INCOGNITO);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public IncognitoNotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ThreadUtils.runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.NOTIFCLOSEINCOGNITO_EVENTS, AnalyticsSettings.ID_CLICK);
                WeakReference<ActivityController> activityWeakReference = Browser.getInstance().getController();
                if (activityWeakReference != null) {
                    ActivityController controller = activityWeakReference.get();
                    if (controller instanceof Controller) {
                        ((Controller) controller).closeAllTabs(true);
                    }
                }
            }
        });
    }
}
