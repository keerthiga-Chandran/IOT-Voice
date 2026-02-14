package com.proz.projectiot.Devices;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.proz.projectiot.R;

import java.util.ArrayList;
import java.util.List;

public class WifiActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    String ssid;
    EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            ssid = b.getString("ssid");
        }

        password = findViewById(R.id.password);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Button connectBtn = findViewById(R.id.btnConnect);
        Button disconnectBtn = findViewById(R.id.btnDisconnect);
        Button suggestBtn = findViewById(R.id.btnSuggest);

        connectBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (checkPermissions()) {
                    connectToWifi(ssid, password.getText().toString());
                } else {
                    requestPermissions();
                }
            } else {
                Toast.makeText(this, "Requires Android 10+", Toast.LENGTH_SHORT).show();
            }
        });

        disconnectBtn.setOnClickListener(v -> disconnectWifi());

        suggestBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                suggestWifi(ssid, password.getText().toString());
            }
        });
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToWifi(String ssid, String password) {
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                connectivityManager.bindProcessToNetwork(network);
                runOnUiThread(() -> Toast.makeText(WifiActivity.this,
                        "Connected to " + ssid, Toast.LENGTH_SHORT).show());
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);
    }

    private void disconnectWifi() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            connectivityManager.bindProcessToNetwork(null);
            Toast.makeText(this, "Disconnected Wi-Fi", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void suggestWifi(String ssid, String password) {
        WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsAppInteractionRequired(true) // user must approve
                .build();

        List<WifiNetworkSuggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int status = wifiManager.addNetworkSuggestions(suggestions);

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(this, "Suggestion added. User must approve.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add suggestion", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission required to connect Wi-Fi", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
