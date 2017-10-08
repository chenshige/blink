// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.webkit.WebView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.ToastUtil;

import java.io.File;
import java.net.URLEncoder;
import java.util.Map;

public class ContextMenuClickListener implements View.OnClickListener {
    public static final String LOGTAG = "ContextMenu";
    public static final int FILD_ID = 0;
    public static final int FILD_TITLE = 1;
    private int[] mMenuItemData;
    private Context mContext;
    private WebView.HitTestResult mResult;
    private String mExtra;
    private Controller mController;
    private Dialog mDialog;

    // Only view images using these schemes
    private static final String[] IMAGE_VIEWABLE_SCHEMES = {
            "http",
            "https",
            "file"
    };

    public ContextMenuClickListener(Context context, int id, int titleResId, WebView.HitTestResult result, Controller
            controller, Dialog dialog) {
        mMenuItemData = new int[2];
        mMenuItemData[FILD_ID] = id;
        mMenuItemData[FILD_TITLE] = titleResId;
        mContext = context;
        mResult = result;
        mExtra = result.getExtra();
        mController = controller;
        mDialog = dialog;
    }

    @Override
    public void onClick(View v) {
        if (mExtra == null && mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
            return;
        }

        switch (mMenuItemData[FILD_ID]) {
            case R.id.download_context_menu_id: //下载图片
                if (isImageViewableUri(Uri.parse(mExtra))) {
                    String mimetype = null;
                    if (mController.getCurrentTab() != null && mController.getCurrentTab().getOriginalUrl() != null
                            && mController.getCurrentTab().getOriginalUrl().equals
                            (mExtra) && mController.getParentTab() != null && mController.getParentTab()
                            .getOriginalUrl() != null) {
                        DownloadHandler.onDownloadStart((Activity) mContext, mExtra, null,
                                null, mimetype, mController.getParentTab().getOriginalUrl(), false, mController);
                    } else {
                        if (mController.getCurrentTab() != null) {
                            DownloadHandler.onDownloadStart((Activity) mContext, mExtra, null,
                                    null, mimetype, mController.getCurrentTab().getOriginalUrl(), false, mController);
                        }
                    }
                } else if (mExtra.startsWith("data:image/jpeg;base64")) {
                    saveBase64ImgToFile(mExtra);
                }
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PICTURE_EVENTS, AnalyticsSettings.ID_SAVEIMAGE);

                break;
            case R.id.view_image_context_menu_id: //查看图片
                mController.openTab(mExtra, mController.getCurrentTab(), true, true);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PICTURE_EVENTS, AnalyticsSettings.ID_VIEWIMAGE);
                break;
            case R.id.set_wallpaper_context_menu_id: //设为壁纸
                new WallpaperHandler(mContext, mExtra).onMenuItemClick();
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PICTURE_EVENTS, AnalyticsSettings.ID_SETWALLPAPER);
                break;
            case R.id.share_link_context_menu_id: //分享图片链接
                mController.sharePage(mContext, null, mExtra, null, null);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PICTURE_EVENTS, AnalyticsSettings.ID_SHAREPICTURE);
                break;
            case R.id.dial_context_menu_id:
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(WebView.SCHEME_TEL + mExtra)));
                break;
            case R.id.add_contact_context_menu_id:
                Intent addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                addIntent.putExtra(ContactsContract.Intents.Insert.PHONE, Uri.decode(mExtra));
                addIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                mContext.startActivity(addIntent);
                break;
            case R.id.copy_phone_context_menu_id:
                new Copy(mExtra).onMenuItemClick();
                break;
            case R.id.email_context_menu_id:
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(WebView.SCHEME_MAILTO + mExtra)));
                break;
            case R.id.copy_mail_context_menu_id:
                new Copy(mExtra).onMenuItemClick();
                break;
            case R.id.map_context_menu_id:
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(WebView.SCHEME_GEO
                                + URLEncoder.encode(mExtra))));
                break;
            case R.id.copy_geo_context_menu_id:
                new Copy(mExtra).onMenuItemClick();
                break;
            case R.id.open_context_menu_id:
                mController.onContextItemSelected(R.id.open_context_menu_id);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.LINKS_EVENTS, AnalyticsSettings.ID_OPEN);
                break;
            case R.id.open_newtab_context_menu_id:
            case R.id.open_newtab_background_context_menu_id:
                mController.onContextItemSelected(mMenuItemData[FILD_ID]);
            /*    final Tab parent = mController.getCurrentTab();
                mController.openTab(mExtra, parent, true, true);*/
                break;
            case R.id.save_link_context_menu_id:
                mController.onContextItemSelected(R.id.save_link_context_menu_id);
                break;
            case R.id.copy_link_context_menu_id:
                mController.onContextItemSelected(R.id.copy_link_context_menu_id);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.LINKS_EVENTS, AnalyticsSettings.ID_PASTE);
                break;
            case R.id.image_ad_mark:
                //广告标记
                mController.onContextItemSelected(R.id.image_ad_mark);
                break;
            default:
                break;
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private static boolean isImageViewableUri(Uri uri) {
        String scheme = uri.getScheme();
        for (String allowed : IMAGE_VIEWABLE_SCHEMES) {
            if (allowed.equals(scheme)) {
                return true;
            }
        }
        return false;
    }

    private void saveBase64ImgToFile(String imgStr) {
        imgStr = imgStr.substring(imgStr.indexOf(","));

        final Uri apkpath = Uri.withAppendedPath(Uri.fromFile(new File(BrowserSettings.getInstance().getDownloadPath
                ())), "download_image" + System.currentTimeMillis() + ".jpg");

        if (FileUtils.GenerateImage(imgStr, apkpath.getPath())) {
            ToastUtil.showShortToastByString(mContext, apkpath.getPath() + "     " + mContext.getResources()
                    .getString(R.string.downloaded));
        } else {
            ToastUtil.showShortToast(mContext, R.string.download_failed);
        }
    }


    private class Copy {
        private CharSequence mText;

        public boolean onMenuItemClick() {
            copy(mText);
            return true;
        }

        public Copy(CharSequence toCopy) {
            mText = toCopy;
        }
    }


    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mContext
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }
}
