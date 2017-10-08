package com.wcc.wink.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.wcc.wink.Resource;
import com.wcc.wink.Wink;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.SimpleURLResource;

import java.io.File;

/**
 * Created by Wenhao on 2016/12/16.
 */

public class FileUtils {


    public static boolean fileIsExists(String path) {
        try {
            File f = new File(path);
            return f.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void checkFile(Context context, String apkName, int id, File cacheFile, DownloadInfo entity) {
        int loc = apkName.lastIndexOf(".");
        StringBuilder builder = new StringBuilder(apkName);
        builder.insert(loc, "(" + id + ")");
        String newName = builder.toString();
        Uri apkpath = Uri.withAppendedPath(Uri.fromFile(new File(Wink.get().getSetting()
                .getSimpleResourceStorageDirectory().getAbsolutePath())), newName);
        if (FileUtils.fileIsExists(apkpath.getPath())) {
            checkFile(context, apkName, id + 1, cacheFile, entity);
        } else {
            boolean ret = cacheFile.renameTo(new File(apkpath.getPath()));
            entity.setLocalFilePath(apkpath.getPath());
            Resource res = entity.getResource();
            if (res instanceof SimpleURLResource && context != null) {
                String mime = ((SimpleURLResource) res).getMimeType();
                if ("application/vnd.android.package-archive".equalsIgnoreCase(mime)) {
                    FileUtils.apkInfo(context, entity);
                }
            }
            entity.setTitle(newName);
            WLog.i("", "cache file renameTo %s, result:%b, title:%s", entity.getLocalFilePath(), ret, apkName);
        }
    }

    public static void apkInfo(Context context, DownloadInfo data) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(data.getLocalFilePath(), PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            /* 必须加这两句，不然下面icon获取是default icon而不是应用包的icon */
            appInfo.sourceDir = data.getLocalFilePath();
            appInfo.publicSourceDir = data.getLocalFilePath();
            String appName = pm.getApplicationLabel(appInfo).toString();// 得到应用名
            Drawable icon1 = pm.getApplicationIcon(appInfo);// 得到图标信息
            Drawable icon2 = appInfo.loadIcon(pm);

            if (icon1 != null) {
                data.setApkIcon(icon1);
            } else if (icon2 != null) {
                data.setApkIcon(icon2);
            }

            if (!TextUtils.isEmpty(appName)) {
                data.setApkName(appName);
            }
        }
    }

}
