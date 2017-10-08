package com.blink.browser.util;

import com.blink.browser.provider.BrowserContract;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

/**
 * Utils to find system config or run some system option
 */
public class DefaultBrowserSetUtils {
    public static final String KEY_DEFAULT_BROWSER_SETTING = "key_default_browser_setting";

    public static final String BROWSABLE_CATEGORY = "android.intent.category.BROWSABLE";

    /**
     * created by simon on 16-8-15
     * finds system default browser info
     */
    public static ResolveInfo findDefaultBrowser(Context context) {
        Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(BrowserContract.EMPTY_WEB_URL));
        return context.getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    public static boolean isThereNoDefaultBrowser(Context context) {
        ResolveInfo defaultBrowser = findDefaultBrowser(context);
        return "android".equals(defaultBrowser.activityInfo.packageName);
    }

    public static void openOneUrlToSetDefaultBrowser(Context context, String url) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory(BROWSABLE_CATEGORY);
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        intent.setComponent(new ComponentName("android", "com.android.internal.app.ResolverActivity"));
        intent.putExtra(KEY_DEFAULT_BROWSER_SETTING, true);
        context.startActivity(intent);
    }

    public static void openAppInfoSettingView(Context context, String targetPackageName) {
        Intent i = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String pkg = "com.android.settings";
        String cls = "com.android.settings.applications.InstalledAppDetails";
        i.setComponent(new ComponentName(pkg, cls));
        i.setData(Uri.parse("package:" + targetPackageName));
        context.startActivity(i);
    }

    public static boolean isThisBrowserSetAsDefault(Context context) {
        ResolveInfo defaultBrowser = findDefaultBrowser(context);
        return context.getPackageName().equals(defaultBrowser.activityInfo.packageName);
    }

    public static boolean canSetDefaultBrowser(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ActivityInfo in = pm.getActivityInfo(new ComponentName("android", "com.android.internal.app.ResolverActivity"), PackageManager.MATCH_DEFAULT_ONLY);
            return in != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
