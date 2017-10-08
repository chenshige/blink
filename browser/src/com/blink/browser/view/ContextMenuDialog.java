package com.blink.browser.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.ContextMenuClickListener;
import com.blink.browser.Controller;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.ViewUtils;

import java.util.HashMap;


public class ContextMenuDialog {

    private Context mContext;
    private Resources mResource;
    private Dialog mMenuDialog;
    private int mType;
    private View mContentView;
    private LinearLayout mContentParent;
    private int[][] mMenuData;
    private MenuConstants mMenuConstants;
    private Controller mController;
    private int mViewHeight;
    private WebView.HitTestResult mResult;
    private int mMaxWidth = 0;
    public ContextMenuDialog(Context context, Controller controller, WebView.HitTestResult result) {
        mContext = context;
        mResource = mContext.getResources();
        mController = controller;
        mResult = result;
    }

    public ContextMenuDialog(Context context, int themeResId) {
    }

    public void show(int type, float touchX, float touchY) {
        mType = type;
        getData(mType);
        if (mMenuData == null) {
            return;
        }
        if (mMenuDialog == null) {
            mMenuDialog = new Dialog(mContext, R.style.dialog);
        }
        initContentView();
        mMenuDialog.setContentView(mContentView);
        Window dialogWindow = mMenuDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP);

        lp.x = (int) touchX; // 新位置X坐标
        lp.y = (int) touchY; // 新位置Y坐标
        int max = mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_maxwidth);
        int realWidth = mMaxWidth  + mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_margin_left_right) * 2;
        lp.width = realWidth > max? max: realWidth;

        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mViewHeight = ViewUtils.getHeightOfView(mContentView);
        int screenHeight = DisplayUtil.getScreenHeight(mContext);
        int statusBarHeight = DisplayUtil.getStatusBarHeight(mContext);
        if (touchY + mViewHeight > screenHeight - mResource.getDimension(R.dimen.toolbar_height)) {
            lp.y = (int)(screenHeight - mViewHeight - mResource.getDimension(R.dimen.toolbar_height));
        }
        if (BrowserSettings.getInstance().getShowStatusBar()) {
            lp.y -= statusBarHeight;
        }
        lp.y = lp.y < statusBarHeight ? statusBarHeight : lp.y;
        dialogWindow.setAttributes(lp);

        mMenuDialog.show();
    }

    private void initContentView() {
        mContentView = null;
        mContentView = ((Activity)mContext).getLayoutInflater().inflate(R.layout.contextmenu_dialog_layout, null);
        mContentParent = (LinearLayout) mContentView.findViewById(R.id.menu_item_parent);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        int dividerHeight = mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_divider_height);
        for (int i = 0; i < mMenuData[0].length; i++) {
            final TextView textView = new TextView(mContext);
            textView.setId(mMenuData[0][i]);
            textView.setText(mMenuData[1][i]);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
            textView.setGravity(Gravity.START);
            textView.setPadding(mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_margin_left_right),
                    mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_margin_top_btm),
                    mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_margin_left_right),
                    mResource.getDimensionPixelOffset(R.dimen.context_menu_dialog_margin_top_btm));
            mContentParent.addView(textView, params);
            textView.setOnClickListener(new ContextMenuClickListener(mContext, mMenuData[0][i], mMenuData[1][i], mResult, mController, mMenuDialog));
            TextPaint paint = textView.getPaint();
            float len = paint.measureText(mResource.getString(mMenuData[1][i]));
            if (mMaxWidth < len) {
                mMaxWidth = (int)len;
            }

            if (i != mMenuData[0].length - 1) {
                View divider = new View(mContext);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dividerHeight);
                divider.setBackgroundColor(mResource.getColor(R.color.longclick_menu_divider_bg));
                mContentParent.addView(divider, dividerParams);
            }
        }
    }

    private void getData(int type) {
        if (mMenuConstants == null) {
            mMenuConstants = new MenuConstants();
        }
        mMenuData = mMenuConstants.getMenu(type);
    }

    public void dismiss() {
        mContentView = null;
        mMenuDialog.dismiss();
        mMenuDialog = null;
    }

    private static class MenuConstants {
        private HashMap<String, int[][]> mKeyMap;
        private final String[] GROUP_ID = {
                "PHONE_MENU",
                "EMAIL_MENU",
                "GEO_MENU",
                "ANCHOR_MENU",
                "IMAGE_MENU",
                "SELECT_TEXT_MENU",
                "SELECT_IMAGE_ANCHOR_MENU"
        };

        private final int[][] PHONE_MENU = {
                {
                        R.id.dial_context_menu_id,
                        R.id.add_contact_context_menu_id,
                        R.id.copy_phone_context_menu_id
                },
                {
                        R.string.contextmenu_dial_dot,
                        R.string.contextmenu_add_contact,
                        R.string.contextmenu_copy
                }
        };

        private final int[][] EMAIL_MENU = {
                {
                        R.id.email_context_menu_id,
                        R.id.copy_mail_context_menu_id
                },
                {
                        R.string.contextmenu_send_mail,
                        R.string.contextmenu_copy
                }
        };

        private final int[][] GEO_MENU = {
                {
                        R.id.map_context_menu_id,
                        R.id.copy_geo_context_menu_id
                },
                {
                        R.string.contextmenu_map,
                        R.string.contextmenu_copy
                }
        };


        private final int[][] ANCHOR_MENU = {
                {
                        R.id.open_newtab_context_menu_id,
                        R.id.open_newtab_background_context_menu_id,
                        R.id.save_link_context_menu_id,
                        R.id.copy_link_context_menu_id,
                },
                {
                        R.string.contextmenu_openlink_newwindow,
                        R.string.contextmenu_openlink_newwindow_background,
                        R.string.contextmenu_savelink,
                        R.string.contextmenu_copylink,
                }
        };

        private final int[][] IMAGE_MENU = {
                {
                        R.id.download_context_menu_id,
                        R.id.view_image_context_menu_id,
                        R.id.set_wallpaper_context_menu_id,
                        R.id.share_link_context_menu_id,
                        R.id.image_ad_mark
                },
                {
                        R.string.contextmenu_download_image,
                        R.string.contextmenu_view_image,
                        R.string.contextmenu_set_wallpaper,
                        R.string.contextmenu_sharelink,
                        R.string.mark_image_ad
                }
        };

        private final int[][] SELECT_TEXT_MENU = {
                {
                        R.id.select_text_menu_id
                },
                {
                        R.string.select_dot
                }
        };

        private final int[][] SELECT_IMAGE_ANCHOR_MENU = {
                {
                        R.id.open_newtab_context_menu_id,
                        R.id.open_newtab_background_context_menu_id,
                        R.id.save_link_context_menu_id,
                        R.id.copy_link_context_menu_id,
                        R.id.download_context_menu_id,
                        R.id.view_image_context_menu_id,
                        R.id.set_wallpaper_context_menu_id,
                },
                {
                        R.string.contextmenu_openlink_newwindow,
                        R.string.contextmenu_openlink_newwindow_background,
                        R.string.contextmenu_savelink,
                        R.string.contextmenu_copylink,
                        R.string.contextmenu_download_image,
                        R.string.contextmenu_view_image,
                        R.string.contextmenu_set_wallpaper,
                }
        };
        public MenuConstants() {
            if (mKeyMap == null) {
                mKeyMap = new HashMap<>();
            }
            mKeyMap.clear();
            mKeyMap.put(GROUP_ID[0], PHONE_MENU);
            mKeyMap.put(GROUP_ID[1], EMAIL_MENU);
            mKeyMap.put(GROUP_ID[2], GEO_MENU);
            mKeyMap.put(GROUP_ID[3], ANCHOR_MENU);
            mKeyMap.put(GROUP_ID[4], IMAGE_MENU);
            mKeyMap.put(GROUP_ID[5], SELECT_TEXT_MENU);
            mKeyMap.put(GROUP_ID[6], SELECT_IMAGE_ANCHOR_MENU);
        }

        public int[][] getMenu(int type) {
            switch (type) {
                case WebView.HitTestResult.PHONE_TYPE:
                    return PHONE_MENU;
                case WebView.HitTestResult.EMAIL_TYPE:
                    return EMAIL_MENU;
                case WebView.HitTestResult.GEO_TYPE:
                    return GEO_MENU;
                case WebView.HitTestResult.IMAGE_TYPE:
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PAGE_EVENTS, AnalyticsSettings.ID_PICTURE);
                    return IMAGE_MENU;
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PAGE_EVENTS, AnalyticsSettings.ID_PICTURE);
                    if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.M) {
                        return SELECT_IMAGE_ANCHOR_MENU;
                    } else {
                        return IMAGE_MENU;
                    }
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.PAGE_EVENTS, AnalyticsSettings.ID_LINKS);
                    return ANCHOR_MENU;
                default:
                    return null;
            }
        }
    }
 }
