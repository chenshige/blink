package com.blink.browser.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.blink.browser.Browser;
import com.blink.browser.BrowserActivity;

import java.io.FileInputStream;

/**
 * Created by eric on 16-10-13.
 */
public class BroadcastUtils {

    private static Intent createShortCut(String title, String url) {
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");

        // 快捷名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        // 快捷图标是允许重复
        shortcut.putExtra("duplicate", false);

        Intent respondIntent = new Intent();
        respondIntent.setAction("android.intent.action.VIEW");
        Uri uri = Uri.parse(url);
        respondIntent.setData(uri);
        respondIntent.setClassName(Browser.getInstance().getPackageName(), BrowserActivity.class.getName());
        respondIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, respondIntent);
        return shortcut;
    }

    public static void sendShortCutToDesktop(Context context, String name, String url, String fullPath) {
        Intent shortcut = createShortCut(name, url);

        // 快捷图标
        try {
            Bitmap bm = BitmapFactory.decodeStream(new FileInputStream(fullPath));
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, bm);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // 发送广播
        context.sendBroadcast(shortcut);
    }

    public static void sendShortCutToDesktop(Context context, String name, String url, int resId) {
        Intent shortcut = createShortCut(name, url);
        // 快捷图标
        Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(context, resId);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
        // 发送广播
        context.sendBroadcast(shortcut);
    }

    public static void sendShortCutToDesktop(Context context, String name, String url, Bitmap bitmap) {
        Intent shortcut = createShortCut(name, url);
        // 快捷图标
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        // 发送广播
        context.sendBroadcast(shortcut);
    }
}
