package com.blink.browser.video;

import android.webkit.JavascriptInterface;

/**
 * This class provide methods which should be inject before webview loadUrl
 */
public class JsInterfaceInject {
    private VideoPlayerLayer.MediaInfoListener mListener;

    @JavascriptInterface
    public void getMediaDuration(String duration) {
        if (mListener != null) {
            mListener.getMediaDuration(duration);
        }
    }

    @JavascriptInterface
    public void getCurrentPlayTime(String time) {
        if (mListener != null) {
            mListener.getCurrentPlayTime(time);
        }
    }

    @JavascriptInterface
    public void canGetVideoElement(boolean success) {
        if (mListener != null) {
            mListener.canControlVideoPlay(success);
        }
    }

    @JavascriptInterface
    public void isPaused(boolean isPaused) {
        if (mListener != null) {
            mListener.isPaused(isPaused);
        }
    }

    @JavascriptInterface
    public void initMediaControlsProgressBar() {
        if (mListener != null) {
            mListener.initMediaControlsProgressBar();
        }
    }

    @JavascriptInterface
    public void played() {
        if (mListener != null) {
            mListener.played();
        }
    }

    @JavascriptInterface
    public void paused() {
        if (mListener != null) {
            mListener.paused();
        }
    }

    public void setListener(VideoPlayerLayer.MediaInfoListener listener) {
        mListener = listener;
    }

}
