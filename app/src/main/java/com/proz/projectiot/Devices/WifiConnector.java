package com.proz.projectiot.Devices;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.proz.projectiot.SQLIte.DeviceDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class WifiConnector {

    private static final String TAG = "WifiConnector";
    private final Context context;
    private final Activity activity;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;

    // Store IoT network specifier callback & request
    private ConnectivityManager.NetworkCallback iotCallback = null;
    private NetworkRequest iotRequest = null;

    private final String[] IOT_PREFIXES = new String[]{
            "node", "nodemcu", "esp", "esp32", "iot", "192.168."
    };

    public WifiConnector(@NonNull Context ctx, Activity act) {
        context = ctx.getApplicationContext();
        activity = act;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // Public entry point
    public void connectToWifi(String ssid, String password) {

        if (ssid == null || ssid.isEmpty()) {
            Toast.makeText(context, "Invalid SSID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isHotspot(ssid)) {
            Toast.makeText(context, "Skipping mobile hotspot: " + ssid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasLocationPermission()) {
            requestLocationPermission();
            Toast.makeText(context, "Grant location permission and try again", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isIot = isIotSsid(ssid);

        Log.d(TAG, "connectToWifi → SSID=" + ssid + " | IoT=" + isIot);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            connectLegacy(ssid, password);
            return;
        }

        if (isIot) {
            connectIotWithSpecifier(ssid, password);
        } else {
            connectRouterWithSuggestion(ssid, password);
        }
    }

    private boolean isIotSsid(String ssid) {
        ssid = ssid.toLowerCase();
        for (String p : IOT_PREFIXES) {
            if (ssid.startsWith(p) || ssid.contains(p)) return true;
        }
        return false;
    }

    private boolean isHotspot(String ssid) {
        ssid = ssid.toLowerCase();
        return ssid.contains("iphone") ||
                ssid.contains("hotspot") ||
                ssid.contains("androidap") ||
                ssid.contains("tether") ||
                ssid.contains("mobileap") ||
                ssid.startsWith("realme") ||
                ssid.startsWith("vivo") ||
                ssid.startsWith("oppo") ||
                ssid.startsWith("redmi");
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (activity == null) return;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
    }

    // ===== Legacy (Android < 10)
    private void connectLegacy(String ssid, String pass) {
        Toast.makeText(context, "Legacy Wi-Fi. Not reliable on new Android.", Toast.LENGTH_SHORT).show();
        Log.w(TAG, "Legacy connection requested for " + ssid);
    }

    // ==============================================================
    // ROUTER (Normal Wi-Fi)
    // ==============================================================
    @RequiresApi(Build.VERSION_CODES.Q)
    private void connectRouterWithSuggestion(String ssid, String password) {

        if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);

        try {
            List<WifiNetworkSuggestion> old = wifiManager.getNetworkSuggestions();
            if (old != null) wifiManager.removeNetworkSuggestions(old);
        } catch (Exception ignored) {}

        WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsAppInteractionRequired(true) // forces popup
                .build();

        List<WifiNetworkSuggestion> list = new ArrayList<>();
        list.add(suggestion);

        int status = wifiManager.addNetworkSuggestions(list);

        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(context, "Suggestion failed: " + status, Toast.LENGTH_SHORT).show();
            openWifiPanel();
            return;
        }

        Toast.makeText(context, "Added. System will prompt.", Toast.LENGTH_SHORT).show();
        openWifiSettings();

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION.equals(intent.getAction())) {
                    Toast.makeText(context, "Connected to " + ssid, Toast.LENGTH_SHORT).show();
                }
            }
        }, new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION));
    }

    // ==============================================================
    // IoT (ESP32 / NodeMCU) - NetworkSpecifier
    // ==============================================================
    @RequiresApi(Build.VERSION_CODES.Q)
    private void connectIotWithSpecifier(String ssid, String password) {

        if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);

        try {
            wifiManager.disconnect();
        } catch (Exception ignored) {}

        WifiNetworkSpecifier spec = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(spec);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        iotRequest = builder.build();

        iotCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                connectivityManager.bindProcessToNetwork(network);
                Log.d(TAG, "IoT Connected: " + ssid);
                openWifiSettings();
                Toast.makeText(context, "Connected to IoT: " + ssid, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnavailable() {
                Log.e(TAG, "IoT unavailable: " + ssid);
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "IoT Lost: " + ssid);
            }
        };

        connectivityManager.requestNetwork(iotRequest, iotCallback, 30000);

        Toast.makeText(context, "Connecting IoT: " + ssid, Toast.LENGTH_SHORT).show();
    }

    // ==============================================================
    // FORGET DEVICE (Router + IoT)
    // ==============================================================
    public void forgetDevice(String ssid) {

        Log.d(TAG, "Forgetting device: " + ssid);

        try {
            connectivityManager.bindProcessToNetwork(null);
        } catch (Exception ignored) {}

        try {
            if (iotCallback != null) {
                connectivityManager.unregisterNetworkCallback(iotCallback);
            }
        } catch (Exception ignored) {}

        iotCallback = null;
        iotRequest = null;

        try {
            wifiManager.disconnect();
        } catch (Exception ignored) {}

        try {
            List<WifiNetworkSuggestion> list = wifiManager.getNetworkSuggestions();
            if (list != null) {
                List<WifiNetworkSuggestion> remove = new ArrayList<>();
                for (WifiNetworkSuggestion s : list) {
                    if (s.getSsid().equals(ssid)) remove.add(s);
                }
                wifiManager.removeNetworkSuggestions(remove);

                DeviceDatabaseHelper databaseHelper = new DeviceDatabaseHelper(context);
                databaseHelper.getWritableDatabase();
                databaseHelper.updatePasswordNull(ssid);


            }
        } catch (Exception ignored) {}

        openWifiSettings();
        Toast.makeText(context, "Device forgotten: " + ssid, Toast.LENGTH_SHORT).show();
    }

    // ==============================================================
    // HELPERS
    // ==============================================================
    private void openWifiPanel() {
        if (activity == null) return;
        try {
            Intent i = new Intent(Settings.Panel.ACTION_WIFI);
            activity.startActivityForResult(i, 2001);
        } catch (Exception ignored) {}
    }

    private void openWifiSettings() {
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            i = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
        } else {
            i = new Intent(Settings.ACTION_WIFI_SETTINGS);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
