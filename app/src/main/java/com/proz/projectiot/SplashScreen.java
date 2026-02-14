package com.proz.projectiot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.proz.projectiot.Common.CommonClass;
import com.proz.projectiot.Devices.DeviceListActivity;
import com.proz.projectiot.GoogleOauth.GoogleOauth;

public class SplashScreen extends Activity {
    TextView wifiStatusText ;
    Button controller1Button,controller2Button,deviceList;
    CommonClass commonClass = new CommonClass();
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;
    GoogleSignInAccount account;
    private boolean doubleBackToExitPressedOnce = false;
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_CODE = 100;
    @Override
    protected void onStart() {
        super.onStart();
         account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.d("GOOGLE_AUTH_spl", "Already signed in: " + account.getEmail());
        }
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        wifiStatusText = findViewById(R.id.wifiStatusText);
        controller1Button = findViewById(R.id.controller1Button);
        controller2Button = findViewById(R.id.controller2Button);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1044956637806-1el9i4pn4o1i7cdp6f6l787jg3f2lt3t.apps.googleusercontent.com")   // IMPORTANT
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        /* if(!TextUtils.isEmpty(commonClass.getSharedPref(getApplicationContext(),"mode"))){
            if(commonClass.getSharedPref(getApplicationContext(),"mode").equals("mode1")){
                intentPage("mode1");
            }else{
                intentPage("mode2");
            }
        }else{
            intentPage("mode1");
        }
 */






        requestPermissions();

        // Check Wi-Fi periodically
        checkWifiConnection();
    }
    private String getNetworkType() {
        String result = "Unknown";

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return result;

        NetworkCapabilities nc = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        }
        if (nc == null) return result;

        boolean isWifi = nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean isMobile = nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        if (isMobile) {
            return "MOBILE_DATA";
        }

        if (isWifi) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                String ssid = wifiInfo.getSSID().replace("\"", "");

                // Customize your IoT kit SSID patterns here
                if (ssid.startsWith("IOT_") ||
                        ssid.startsWith("ESP_") ||
                        ssid.contains("KIT") ||
                        ssid.contains("DEVICE"))
                {
                    return "IOT_WIFI";   // Your IoT Kit hotspot
                }
                return "NORMAL_WIFI"; // WiFi but not IoT kit
            }
        }

        return result;
    }
    @Override
    public void onBackPressed() {
        finishAffinity();  // closes the app
        System.exit(0);
    }
    private void intentPage(String mode1) {
         Intent intent =null;
        if(mode1 .equals("mode1")){
            intent = new Intent(getApplicationContext(),ControllerOne.class);
        }else if(mode1.equals("signin")){
           intent = new Intent(getApplicationContext(), GoogleOauth.class);
           // intent = new Intent(getApplicationContext(),ControllerTwo.class);
        }else{
            intent = new Intent(getApplicationContext(),ControllerTwo.class);
        }
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.RECORD_AUDIO
                        },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void checkWifiConnection() {
        String networkType = getNetworkType();    // IOT_WIFI, NORMAL_WIFI, MOBILE_DATA, NONE
        String savedMode = commonClass.getSharedPref(getApplicationContext(),"mode");

        // default mode
        if (TextUtils.isEmpty(savedMode)) {
            savedMode = "mode1";
        }

         if (networkType.equals("IOT_WIFI")) {
            // NO GOOGLE CHECK for IoT
            intentPage(savedMode);
            return;
        }

         if (networkType.equals("NORMAL_WIFI") || networkType.equals("MOBILE_DATA")) {

            account = GoogleSignIn.getLastSignedInAccount(this);

             if (account == null) {
                Log.d("GOOGLE_AUTH", "No login found, redirecting to SIGNIN");
                intentPage("signin");
                return;
            }

            // 👉 Already Signed-in → go to saved mode
            Log.d("GOOGLE_AUTH", "Signed in, redirecting to " + savedMode);
            intentPage(savedMode);
            return;
        }

        // =============== 3️⃣ NO INTERNET OR NO WIFI ======================
        wifiStatusText.setText("Not connected to Wi-Fi or Mobile Data");
    }

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkWifiConnection();   // Re-check every time network changes
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }


    private void showWifiDisconnectedAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Wi-Fi Disconnected")
                .setMessage("Your Wi-Fi connection was lost. Please reconnect.")
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission required to access Wi-Fi info", Toast.LENGTH_SHORT).show();
            } else {
                checkWifiConnection();
            }
        }
    }
}
