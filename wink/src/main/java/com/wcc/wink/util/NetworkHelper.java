package com.wcc.wink.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.wcc.wink.util.NetworkSensor.NetworkStatus;

/**
 * 网络帮助模块
 *
 * @author wenbiao.xie
 */
public class NetworkHelper implements NetworkSensor.Callback{
    private static String TAG = "NetworkHelper";
    public interface NetworkInductor {
        void onNetworkChanged(NetworkSensor.NetworkStatus status);
    }

    private static class HelperHolder {
        private static final NetworkHelper helper = new NetworkHelper();
    }

    public static NetworkHelper sharedHelper() {
        return HelperHolder.helper;
    }

    List<WeakReference<NetworkInductor>> mInductors;
    NetworkSensor mSensor;

    private NetworkHelper() {
        mInductors = new ArrayList<>();
    }

    public void registerNetworkSensor(Context context) {
        registerNetworkSensor(context, null);
    }

    public synchronized void registerNetworkSensor(Context context, NetworkSensor ns) {
        WLog.v(TAG, "registerNetworkSensor");
        if (mSensor == ns)
            return;

        if (mSensor != null) {
            unregisterNetworkSensor(context);
        }

        final NetworkSensor sensor = (ns == null) ? new DefaultNetworkSenor(): ns;
        sensor.register(context, this);
        mSensor = sensor;
    }

    public synchronized void unregisterNetworkSensor(Context context) {
        final NetworkSensor sensor = mSensor;
        if (sensor != null) {
            mSensor = null;
            sensor.unregister(context);
        }
    }

    public NetworkStatus getNetworkStatus() {
        if (mSensor == null)
            throw new IllegalStateException("should register valid sensor first!");
        return mSensor.getNetworkStatus();
    }

    public boolean isWifiActive() {
        return NetworkStatus.NetworkReachableViaWiFi.equals(getNetworkStatus());
    }

    public boolean isMobileActive() {
        return NetworkStatus.NetworkReachableViaWWAN.equals(getNetworkStatus());
    }

    public boolean isBluetoothActive() {
        return NetworkStatus.NetworkReachableViaBlueTooth.equals(getNetworkStatus());
    }

    public boolean isNetworkAvailable() {
        return !NetworkStatus.NetworkNotReachable.equals(getNetworkStatus());
    }

    public void addNetworkInductor(NetworkInductor inductor) {
        final List<WeakReference<NetworkInductor>> list = new ArrayList<WeakReference<NetworkInductor>>(mInductors);
        for (int i = 0; i < list.size(); i++) {
            WeakReference<NetworkInductor> inductorRef = list.get(i);
            NetworkInductor ind = inductorRef.get();
            if (ind == inductor)
                return;
            else if (ind == null) {
                mInductors.remove(inductorRef);
            }
        }

        mInductors.add(new WeakReference<>(inductor));
    }

    public void removeNetworkInductor(NetworkInductor inductor) {
        final List<WeakReference<NetworkInductor>> list = new ArrayList<WeakReference<NetworkInductor>>(mInductors);
        for (int i = 0; i < list.size(); i++) {
            WeakReference<NetworkInductor> inductorRef = list.get(i);
            NetworkInductor ind = inductorRef.get();
            if (ind == inductor) {
                mInductors.remove(inductorRef);
                return;
            } else if (ind == null) {
                mInductors.remove(inductorRef);
            }
        }
    }

    @Override
    public void onNetworkChanged(NetworkStatus status) {
        if (mInductors.size() == 0)
            return;

        final List<WeakReference<NetworkInductor>> list = new ArrayList<WeakReference<NetworkInductor>>(mInductors);
        for (int i = 0; i < list.size(); i++) {
            WeakReference<NetworkInductor> inductorRef = list.get(i);
            NetworkInductor inductor = inductorRef.get();
            if (inductor != null)
                inductor.onNetworkChanged(status);
            else
                mInductors.remove(inductorRef);
        }
    }

    protected static class NetworkBroadcastReceiver extends BroadcastReceiver {

        WeakReference<DefaultNetworkSenor> ref;
        public NetworkBroadcastReceiver(DefaultNetworkSenor sensor) {
            this.ref = new WeakReference<>(sensor);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            WLog.v("NetworkBroadcastReceiver", "onReceive");
            if (intent == null)
                return;

            final DefaultNetworkSenor sensor = ref.get();
            if (sensor == null)
                return;

            final ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                NetworkInfo info = manager.getActiveNetworkInfo();
                NetworkStatus ns = NetworkStatus.NetworkNotReachable;
                if (info == null || !info.isAvailable()) {
                    WLog.i("NetworkBroadcastReceiver", "network not reachable");
                    ns = NetworkStatus.NetworkNotReachable;
                } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    WLog.i("NetworkBroadcastReceiver", "network reachable via wwan");
                    ns = NetworkStatus.NetworkReachableViaWWAN;

                } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    WLog.i("NetworkBroadcastReceiver", "network reachable via wifi");
                    ns = NetworkStatus.NetworkReachableViaWiFi;
                } else if (info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
                    WLog.i("NetworkBroadcastReceiver", "network reachable via bluetooth");
                    ns = NetworkStatus.NetworkReachableViaBlueTooth;
                }

                sensor.networkChanged(ns);
            }
        }
    }

    private static class DefaultNetworkSenor implements NetworkSensor {

        final NetworkBroadcastReceiver mReceiver;
        Callback mCallback;
        NetworkSensor.NetworkStatus mStatus = NetworkStatus.NetworkNotReachable;

        DefaultNetworkSenor() {
            mReceiver = new NetworkBroadcastReceiver(this);
        }

        @Override
        public void register(Context context, Callback callback) {

            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info == null || !info.isAvailable()) {
                WLog.i(TAG, "network not reachable");
                mStatus = NetworkStatus.NetworkNotReachable;
            } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                WLog.i(TAG, "network reachable via wwan");
                mStatus = NetworkStatus.NetworkReachableViaWWAN;

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                WLog.i(TAG, "network reachable via wifi");
                mStatus = NetworkStatus.NetworkReachableViaWiFi;
            } else if (info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
                WLog.i("NetworkBroadcastReceiver", "network reachable via bluetooth");
                mStatus = NetworkStatus.NetworkReachableViaBlueTooth;
            }


            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(mReceiver, intentFilter);
            this.mCallback = callback;
        }

        @Override
        public void unregister(Context context) {
            this.mCallback = null;
            context.unregisterReceiver(mReceiver);
        }

        @Override
        public NetworkStatus getNetworkStatus() {
            return mStatus;
        }

        void networkChanged(NetworkStatus status) {
            if (status.equals(mStatus))
                return;

            mStatus = status;
            final Callback callback = mCallback;
            if (callback != null)
                callback.onNetworkChanged(status);
        }
    }
}
