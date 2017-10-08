package com.blink.browser.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * app维护一个独立的网络监听广播
 */
public class BrowserNetworkStateNotifier {

    public List<NetworkStateChangedListener> mNetworkListeners = new ArrayList<>();
    private static BrowserNetworkStateNotifier sInstance;

    private BrowserNetworkStateNotifier() {
    }

    private BroadcastReceiver mNetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                for (NetworkStateChangedListener listener : mNetworkListeners) {
                    listener.onNetworkStateChanged();
                }
            }
        }
    };

    public void registerReveiver(Context context) {
        if(context!=null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(this.mNetReceiver, filter);
        }
    }

    public void unRegisterReceiver(Context context) {
        if(context!=null) {
            context.unregisterReceiver(mNetReceiver);
        }
    }

    public static BrowserNetworkStateNotifier getInstance() {
        if (sInstance == null) {
            sInstance = new BrowserNetworkStateNotifier();
        }
        return sInstance;
    }

    public void addEventListener(NetworkStateChangedListener listener) {
        if (!mNetworkListeners.contains(listener)) {
            mNetworkListeners.add(listener);
        }
    }

    public void removeEventListener(NetworkStateChangedListener listener) {
        mNetworkListeners.remove(listener);
    }


    public interface NetworkStateChangedListener {
        void onNetworkStateChanged();
    }

}
