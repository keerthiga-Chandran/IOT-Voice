package com.proz.projectiot.Devices;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkChangeListener {
    public interface WifiStatusListener {
        void onWifiConnected(String ssid);

        void onWifiDisconnected();
    }

    private WifiStatusListener listener;
    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkChangeListener(Context context, WifiStatusListener listener) {
        this.listener = listener;

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                String ssid = wifiInfo != null ? wifiInfo.getSSID() : null;
                if (ssid != null && !ssid.equals("<unknown ssid>")) {
                    listener.onWifiConnected(ssid.replace("\"", ""));
                }
            }

            @Override
            public void onLost(Network network) {
                listener.onWifiDisconnected();
            }
        };

        cm.registerDefaultNetworkCallback(networkCallback);
    }
    public void unregister(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        } catch (Exception e) {
            Log.e("NetworkChangeListener", "Failed to unregister", e);
        }
    }
}
