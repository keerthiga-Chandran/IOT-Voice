package com.proz.projectiot.Devices;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.proz.projectiot.Common.CommonClass;
import com.proz.projectiot.ControllerOne;
import com.proz.projectiot.ControllerTwo;
import com.proz.projectiot.Devices.Adapter.DeviceAdapter;
import com.proz.projectiot.Devices.Modal.Device;
import com.proz.projectiot.R;
import com.proz.projectiot.SQLIte.DeviceDatabaseHelper;
import com.proz.projectiot.Settings.SettingActivity;

import java.util.*;

public class DeviceListActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;
    private List<Device> deviceList = new ArrayList<>();
    private DeviceAdapter adapter;
    private static final int PERMISSIONS_REQUEST_CODE = 123;
    TextView title;
    private NetworkChangeListener networkChangeListener;
    ImageView settings;

    private DeviceDatabaseHelper dbHelper;

    ImageView back;
    String scan_str,mode;
    CommonClass commonClass = new CommonClass();

    ProgressDialog progressDialog;

    private Handler handler = new Handler();

    private Runnable permissionRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("DeviceListActivity","permission");
            checkPermissions();  // call your method
            handler.postDelayed(this, 60 * 1000); // repeat every 1 minute
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("DeviceListActivity","onResume");
        handler.post(permissionRunnable); // start loop
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("DeviceListActivity","onPause");
        handler.removeCallbacks(permissionRunnable); // stop loop
    }
    @Override
    public void finish() {
        super.finish();
        Intent intent =null;
        if(TextUtils.isEmpty(mode)){
            intent = new Intent(getApplicationContext(), ControllerOne.class);
        }else{
            if(mode.startsWith("one")){
                intent = new Intent(getApplicationContext(), ControllerOne.class);
            }else{
                intent = new Intent(getApplicationContext(), ControllerTwo.class);
            }
        }
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showLoader() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("WiFi list fetching.Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoader() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        Log.d("DeviceListActivity","oncreate ");
        showLoader();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        settings = findViewById(R.id.settings);

        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
        title.setText("Device List");
        dbHelper = new DeviceDatabaseHelper(this);
        dbHelper.getWritableDatabase();
        Bundle b = getIntent().getExtras();
        if(b!=null){
            mode = b.getString("mode");
            scan_str = b.getString("scan");
            if(!TextUtils.isEmpty(scan_str)){
                Toast.makeText(getApplicationContext(),"Wifi scan completed",Toast.LENGTH_SHORT).show();
                checkPermissions();
            }
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                1001
        );

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commonClass.sendBack(DeviceListActivity.this);
            }
        });
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DeviceAdapter(DeviceListActivity.this,deviceList);

         recyclerView.setAdapter(adapter);

        FloatingActionButton refreshBtn = findViewById(R.id.refreshButton);
        FloatingActionButton addBtn = findViewById(R.id.addButton);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        refreshBtn.setOnClickListener(v -> startWifiScan());
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(getApplicationContext(),AddDeviceActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }
        });

        checkPermissions();

        networkChangeListener = new NetworkChangeListener(this, new NetworkChangeListener.WifiStatusListener() {
            @Override
            public void onWifiConnected(String ssid) {
                Log.d("WIFI", "Connected → " + ssid);
                reloadAdapter();
            }

            @Override
            public void onWifiDisconnected() {
                Log.d("WIFI", "Disconnected");
                reloadAdapter();
            }
        });
    }
    private void reloadAdapter() {
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    protected void onDestroy() {
        if (wifiScanReceiver != null) unregisterReceiver(wifiScanReceiver);

        if (networkChangeListener != null) {
            networkChangeListener.unregister(this);
        }
        super.onDestroy();
    }

    private void checkPermissions() {
        Log.d("DeviceListActivity","checkpermission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
            } else {
                startWifiScan();
            }
        } else {
            startWifiScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWifiScan();
        } else {
          //  Toast.makeText(this, "Location permission is required for scanning Wi-Fi", Toast.LENGTH_LONG).show();
        }
    }

    private void startWifiScan() {
        Log.d("DeviceListActivity","start wifi scan ");
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) scanSuccess();
                else scanFailure();
            }
        };

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success = wifiManager.startScan();
        if (!success) scanFailure();
    }

/*
    private void scanSuccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String connectedSSID = wifiInfo.getSSID();
        if (connectedSSID != null && connectedSSID.startsWith("\"") && connectedSSID.endsWith("\"")) {
            connectedSSID = connectedSSID.substring(1, connectedSSID.length() - 1);
        }

        List<ScanResult> results = wifiManager.getScanResults();
        deviceList.clear();

        for (ScanResult result : results) {
            if (result.SSID != null && !result.SSID.isEmpty()) {
                boolean isConnected = result.SSID.equals(connectedSSID);
                deviceList.add(new Device(result.SSID, result.BSSID, isConnected));

                if (dbHelper.isRemoved(result.BSSID)) continue;

                // Add to DB if not exist
                dbHelper.insertOrUpdateDevice(result.SSID, result.BSSID);
            }
        }

        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Scan complete: " + deviceList.size() + " devices found", Toast.LENGTH_SHORT).show();
    }
*/
private void scanSuccess() {
    hideLoader();
    Log.d("DeviceListActivity","scan success");
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
        return;
    }

    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    String connectedSSID = wifiInfo.getSSID();
    if (connectedSSID != null && connectedSSID.startsWith("\"") && connectedSSID.endsWith("\"")) {
        connectedSSID = connectedSSID.substring(1, connectedSSID.length() - 1);
    }

    List<ScanResult> results = wifiManager.getScanResults();
    HashSet<String> ssidSet = new HashSet<>();
    Log.d("DeviceListActivity","as "+results.size());
    for (ScanResult result : results) {
        if (result.SSID == null || result.SSID.isEmpty()) continue;

         if (result.SSID.toLowerCase().contains("iphone")) continue;

         String s = result.SSID.toLowerCase();
        if (s.contains("hotspot") || s.contains("mobileap") || s.contains("androidap") ||
                s.contains("galaxy") || s.contains("vivo") || s.contains("oppo") ||
                s.contains("redmi") || s.contains("realme") || s.contains("oneplus")) {
            continue;
        }

         String bssid = result.BSSID.toUpperCase();
        if (bssid.startsWith("A4:5E:60") ||  // Apple
                bssid.startsWith("D0:23:DB") ||  // Apple
                bssid.startsWith("28:F0:76") ||  // Apple
                bssid.startsWith("70:11:24")) {  // Apple
            continue;
        }

         if (dbHelper.isRemoved(result.BSSID)) continue;

         if (ssidSet.contains(result.SSID)) continue;
        ssidSet.add(result.SSID);

         dbHelper.insertOrUpdateDevice(result.SSID, result.BSSID);
    }

    deviceList.clear();
    deviceList.addAll(dbHelper.getAllActiveDevices());

    // Mark connected device
    for (Device d : deviceList) {
        d.setConnected(d.getName().equals(connectedSSID));
    }

    adapter.notifyDataSetChanged();
}

    private void scanFailure() {
     }






}
