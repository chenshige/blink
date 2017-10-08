package com.blink.browser.homepages;

import android.support.v4.content.ContextCompat;

import com.blink.browser.PhoneUi;
import com.blink.browser.R;
import com.blink.browser.TabControl;
import com.blink.browser.UI;
import com.blink.browser.util.SystemTintBarUtils;

/**
 * 主界面的状态较多，且多种状态以及对应的View所呈现的沉浸式颜色状态不同
 * 介于此独立类用于控制不同状态下的颜色对应
 */
public class ImmersiveController {
    public static final int CHILD_DEFAULT = -1;
    public static final int CHILD_TABMODE_INCOGNITO = 0;
    public static final int CHILD_TABMODE_NORMAL = 1;


    private static ImmersiveController sInstance;
    private PhoneUi mUi;
    private TabControl mTabControl;
    private int mWebViewStatusBarColor;

    private ImmersiveController() {
    }

    public static void init(PhoneUi ui, TabControl tabControl) {
        if (sInstance == null) {
            sInstance = new ImmersiveController();
        }
        sInstance.bind(ui, tabControl);
    }

    /**
     * 用于刷新PhoneUi和TabControl，防止进程没有销毁的情况下，browserActivity执行onDestroy后，
     * 重现打开浏览器PhoneUi和TabControl没有重新赋值
     */
    private void bind(PhoneUi ui, TabControl tabControl) {
        mUi = ui;
        mTabControl = tabControl;
        mWebViewStatusBarColor = ContextCompat.getColor(mUi.getActivity(), R.color.status_bar_webview);
    }

    public static final ImmersiveController getInstance() {
        if (sInstance == null) {
            throw new RuntimeException(" ImmersiveController uninitialized .");
        }
        return sInstance;
    }

    public void changeStatus() {
        // This is used to prevent changing the navigation bar style when app switch to video fullscreen mode
        if (mUi.getVideoFullScreenState()) {
            return;
        }
        this.changeStatus(CHILD_DEFAULT);
    }

    public void changeStatus(int childStatus) {
        UI.ComboHomeViews status = mUi.getComboStatus();
        switch (status) {
            case VIEW_HIDE_NATIVE_PAGER:
                SystemTintBarUtils.setSystemBarColor(mUi.getActivity(), R.color.status_bar_homepage);
                break;
            case VIEW_NAV_SCREEN:
                SystemTintBarUtils.setSystemBarColor(mUi.getActivity(), R.color.navscreen_backgroud_color);
                break;
            case VIEW_NATIVE_PAGER:
                SystemTintBarUtils.setSystemBarColor(mUi.getActivity(), R.color.status_bar_homepage);
                mWebViewStatusBarColor = ContextCompat.getColor(mUi.getActivity(), R.color.status_bar_webview);
                break;
            case VIEW_WEBVIEW:
                SystemTintBarUtils.setSystemBarColorByValue(mUi.getActivity(), mWebViewStatusBarColor);
                break;
            default:
        }
    }

    public void setWebViewStatusBarColor(int color) {
        mWebViewStatusBarColor = color;
        changeStatus();
    }

}
