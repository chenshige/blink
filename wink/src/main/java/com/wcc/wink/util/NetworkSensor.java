package com.wcc.wink.util;

import android.content.Context;

/**
 * Created by wenbiao.xie on 2016/9/18.
 */
public interface NetworkSensor {

    enum NetworkStatus {
        NetworkNotReachable,
        NetworkReachableViaWWAN,
        NetworkReachableViaWiFi,
        NetworkReachableViaBlueTooth;

        public String getShortName() {
            String s = this.toString();
            if (this.equals(NetworkNotReachable))
                s = s.replace("Network", "");
            else
                s = s.replace("NetworkReachableVia", "");

            return s;
        }
    }

    interface Callback {
        void onNetworkChanged(NetworkStatus status);
    }

    void register(Context context, Callback callback);
    void unregister(Context context);
    NetworkStatus getNetworkStatus();
}
