package com.blink.browser.video;

import android.view.KeyEvent;
import android.view.View;

public interface VideoPlayerLayer {
    void setPreView(View view);
    View getLayer();
    void setListener(Listener l);
    void beginFullScreen();
    void endFullScreen();
    boolean dispatchKey(int code, KeyEvent event);
    void multiWindow();

    interface Listener {
        void setLockerState(boolean state);
    }

    interface MediaInfoListener {
        void getMediaDuration(String duration);
        void getCurrentPlayTime(String time);
        void canControlVideoPlay(boolean success);
        void isPaused(boolean isPaused);
        void initMediaControlsProgressBar();
        void played();
        void paused();
    }
}
