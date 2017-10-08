package com.blink.browser.handler;

import android.os.Handler;

public class BrowserHandler {
    public static BrowserHandler sInstance;
    private Handler mHandler;
    public static BrowserHandler getInstance(){
        if (sInstance == null){
            sInstance = new BrowserHandler();
        }
        return sInstance;
    }

    public void handlerPostDelayed(Runnable runnable ,long time) {
        if (mHandler == null){
            mHandler = new Handler();
        }
        mHandler.postDelayed(runnable, time);
    }
}
