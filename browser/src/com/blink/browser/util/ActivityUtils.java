package com.blink.browser.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.blink.browser.BrowserActivity;
import com.blink.browser.ComboViewActivity;
import com.blink.browser.R;
import com.blink.browser.download.DownloadActivity;
import com.blink.browser.provider.BrowserContract;

/**
 * Created by eric on 16/3/31.
 */
public class ActivityUtils {

    public static void startActivity(Context context, Class<? extends Activity> cls) {
        Intent intent = new Intent(context, cls);
        context.startActivity(intent);
    }

    public static void startDownloadActivity(Activity activity, int state) {
        Intent intent = new Intent(activity, DownloadActivity.class);
        intent.putExtra(BrowserContract.DOWNLOAD_STATE, state);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
    }

    public static void startComboViewActivity(Activity activity, int code, int menuId) {
        Intent intent = new Intent(activity, ComboViewActivity.class);
        intent.putExtra(BrowserContract.MENU_ID, menuId);
        activity.startActivityForResult(intent, code);
        activity.overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
    }

    public static void openUrl(Activity srcActivity, String url) {
        Intent intent = srcActivity.getPackageManager().getLaunchIntentForPackage(srcActivity.getPackageName());
        intent.setClass(srcActivity, BrowserActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        srcActivity.startActivity(intent);
    }

}
