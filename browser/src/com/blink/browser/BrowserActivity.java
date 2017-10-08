/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blink.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.blink.browser.adblock.AdBlock;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.BrowserDownloadManager;
import com.blink.browser.download.BrowserNetworkStateNotifier;
import com.blink.browser.stub.NullController;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.DeviceInfoUtils;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.Logger;
import com.blink.browser.util.RecommendUrlUtil;
import com.blink.browser.util.SystemTintBarUtils;

public class BrowserActivity extends FragmentActivity {
    private final static int REQUEST_READ_PHONE_STATE_CODE = 0x1001;

    public static final String ACTION_SHOW_BOOKMARKS = "show_bookmarks";
    public static final String ACTION_SHOW_BROWSER = "show_browser";
    public static final String ACTION_OPEN_URL = "open_url";
    public static final String ACTION_RESTART = "--restart--";
    private static final String EXTRA_STATE = "state";
    public static final String EXTRA_DISABLE_URL_OVERRIDE = "disable_url_override";

    private final static String LOGTAG = "browser";

    private ActivityController mController = NullController.INSTANCE;

    private HandlerThread mHandlerThread;
    private BroadcastReceiver mReceiver;
    private float mTouchX, mTouchY;
    private View mRootView;
    private int mCurrHeight = -1;
    private boolean mIsActive = false;


    private ViewTreeObserver.OnGlobalLayoutListener mGlobalListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (mRootView == null) return;
            Rect r = new Rect();
            mRootView.getWindowVisibleDisplayFrame(r);
            int visitHeight = mRootView.getBottom() - r.bottom;
            if (mCurrHeight == visitHeight) {
                return;
            }
            mCurrHeight = visitHeight;
            if (mController != null && mController instanceof Controller) {
                ((Controller) mController).onInputKeyChanged(mCurrHeight);
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        Logger.verbose(LOGTAG, this + " onStart, has state: "
                + (icicle == null ? "false" : "true"));
        super.onCreate(icicle);

        // SystemTintBarUtils.setSystemBarColor(this);
        if (shouldIgnoreIntents()) {
            finish();
            return;
        }

        mController = createController();
        Browser.getInstance().setController(mController);
        Intent intent = (icicle == null) ? getIntent() : null;
        mController.start(intent);
        DisplayUtil.changeScreenBrightnessIfNightMode(this);

        SystemTintBarUtils.setSystemBarColor(this, R.color.status_bar_homepage);

        // Init AdBlock.
        AdBlock.getInstance().initAdBlock(Browser.getInstance());

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                    Logger.debug(LOGTAG, " finish activity !! ");
                    RecommendUrlUtil.resetDbForChangeLanguage(context);
                    getController().exit();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        this.registerReceiver(mReceiver, filter);
        requestPhonePermission();
        mRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalListener);
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    private Controller createController() {
        Controller controller = new Controller(this);
        boolean xlarge = isTablet(this);
        UI ui = null;
        if (xlarge) {
            ui = new XLargeUi(this, controller);
        } else {
            ui = new PhoneUi(this, controller);
        }
        controller.setUi(ui);
        return controller;
    }

    Controller getController() {
        return (Controller) mController;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (shouldIgnoreIntents()) return;

        if (ACTION_RESTART.equals(intent.getAction())) {
            Bundle outState = new Bundle();
            mController.onSaveInstanceState(outState);
            finish();
            getApplicationContext().startActivity(
                    new Intent(getApplicationContext(), BrowserActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(EXTRA_STATE, outState));
            return;
        }
        mController.handleNewIntent(intent);
    }

    private PowerManager mPowerManager;

    private boolean shouldIgnoreIntents() {
        // Only process intents if the screen is on and the device is unlocked
        // aka, if we will be user-visible
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        boolean ignore = !mPowerManager.isScreenOn();
        Logger.verbose(LOGTAG, "ignore intents: " + ignore);
        return ignore;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.verbose(LOGTAG, "BrowserActivity.onResume: this=" + this);

        mController.onResume();
        BrowserNetworkStateNotifier.getInstance().registerReveiver(this);
        BrowserAnalytics.onResume(this);

        if (DeviceInfoUtils.isAppOnForeground(this) && !mIsActive) {
            mIsActive = true;
        }
    }


    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (Window.FEATURE_OPTIONS_PANEL == featureId) {
            mController.onMenuOpened(featureId, menu);
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mController.onOptionsMenuClosed(menu);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mController.onContextMenuClosed(menu);
    }

    /**
     * onSaveInstanceState(Bundle map)
     * onSaveInstanceState is called right before onStop(). The map contains
     * the saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Logger.verbose(LOGTAG, "BrowserActivity.onSaveInstanceState: this=" + this);

        mController.onSaveInstanceState(outState);
    }

    /**
     * 请求获取读取手机状态权限
     *
     * @return
     */
    private boolean requestPhonePermission() {
        if (Build.VERSION.SDK_INT < BuildUtil.VERSION_CODES.M) {
            return false;
        }
        int hasPermission = this.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE);
        Logger.debug(LOGTAG, "android.Manifest.permission.READ_PHONE_STATE " + hasPermission);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE},
                    REQUEST_READ_PHONE_STATE_CODE);
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        try {
            mController.onPause();
            super.onPause();

            BrowserNetworkStateNotifier.getInstance().unRegisterReceiver(this);
            BrowserAnalytics.onPause(this);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStop() {
        mController.onStop();
        super.onStop();

        if (!DeviceInfoUtils.isAppOnForeground(this) && mIsActive) {
            mIsActive = false;
        }
    }

    @Override
    protected void onDestroy() {
        Logger.verbose(LOGTAG, "BrowserActivity.onDestroy: this=" + this);

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        super.onDestroy();
        if (mController != null) {
            mController.onDestroy();
        }
        mController = NullController.INSTANCE;

        // Destroy AdBlock.
        AdBlock.getInstance().destroyAdBlock();

        if (mReceiver != null) {
            this.unregisterReceiver(mReceiver);
        }

        BrowserDownloadManager.getInstance().onDestroy();
    }

    public HandlerThread getHandlerThread() {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("search");
            mHandlerThread.start();
        }
        return mHandlerThread;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mController.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mController.onLowMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mController.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mController.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mController.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        mController.onCreateContextMenu(menu, v, menuInfo, mTouchX, mTouchY);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return mController.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event) ||
                super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mController.onKeyLongPress(keyCode, event) ||
                super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mController.onKeyUp(keyCode, event) ||
                super.onKeyUp(keyCode, event);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        mController.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mController.onActionModeFinished(mode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        mController.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onSearchRequested() {
        return mController.onSearchRequested();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mController.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mController.dispatchKeyShortcutEvent(event)
                || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mTouchX = ev.getRawX();
        mTouchY = ev.getRawY();
        return mController.dispatchTouchEvent(ev)
                || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mController.dispatchTrackballEvent(ev)
                || super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mController.dispatchGenericMotionEvent(ev) ||
                super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
        mController.invalidateOptionsMenu();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //auto checkupdate close
        checkUpdate();
    }

    private void checkUpdate() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 2000);
    }
}
