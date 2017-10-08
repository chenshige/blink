package com.blink.browser.util;

import android.text.TextUtils;

import com.blink.browser.Browser;
import com.blink.browser.R;

public class ChannelUtil {

    private static final String CHANNEL_NONE = "00000000";
    private static final String CHANNEL_GOOGLE_PREFIX = "2000";

    private final static String CHANNEL_CHINA_PREFIX = "8000";//国内渠道的首数字

    private static final String sUpdateList[] = {
            CHANNEL_NONE,
    };

    public static boolean isUpdateEnabled() {
        String channelId = getChannelId();
        if (TextUtils.isEmpty(channelId)) return false;
        for (String str : sUpdateList) {
            if (channelId.equals(str) || channelId.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGoogleChannel() {
        return true;
//        String channelId = getChannelId();
//        return !TextUtils.isEmpty(channelId) && channelId.startsWith(CHANNEL_GOOGLE_PREFIX);
    }

    public static String getChannelId() {
        return Browser.getInstance().getApplicationContext().getString(R.string.channel);
    }

    public static boolean isOpenPlatform() {
        return CHANNEL_NONE.equals(getChannelId());
    }

    /**
     * 是否是国内渠道
     *
     * @return
     */
    public static boolean isChinaChannel() {
        String channelId = getChannelId();
        return !TextUtils.isEmpty(channelId) && channelId.startsWith(CHANNEL_CHINA_PREFIX);
    }
}
