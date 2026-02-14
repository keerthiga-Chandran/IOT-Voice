package com.proz.projectiot.Devices.Adapter;

import static android.view.View.GONE;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.proz.projectiot.Devices.DeviceControlActivity;
import com.proz.projectiot.Devices.Modal.Device;
import com.proz.projectiot.Devices.WifiActivity;
import com.proz.projectiot.Devices.WifiConnector;
import com.proz.projectiot.R;
import com.proz.projectiot.SQLIte.DeviceDatabaseHelper;

import java.util.Collections;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {


    Context context;

    private final List<Device> deviceList;
    WifiManager wifiManager;
    WifiConnector wifiConnector;

    public DeviceAdapter(Context context,List<Device> deviceList ) {
        this.deviceList = deviceList;
        this.context =context;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiConnector = new WifiConnector(context,(Activity) context);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        String displayName = device.getCustom_name() != null ? device.getCustom_name() : device.getName();
        Log.d("deviceadapter","custom name "+device.getCustom_name()+" name "+device.getName());
        holder.name.setText(
                !TextUtils.isEmpty(device.getCustom_name()) ? device.getCustom_name() : device.getName()
        );
        holder.ip.setText(device.getIp());

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String connectedSSID = wifiInfo.getSSID();
        if (connectedSSID != null && connectedSSID.startsWith("\"") && connectedSSID.endsWith("\"")) {
            connectedSSID = connectedSSID.substring(1, connectedSSID.length() - 1);
        }
         Log.d("deviceadapter"," ssid "+connectedSSID+" device "+device.getName()+" IP "+device.getIp()
        +" "+wifiInfo.getIpAddress()+" bssid "+wifiInfo.getMacAddress()+" new name "+device.getCustom_name());
        if (connectedSSID.equals(device.getName())) {
            holder.status.setText("Connected");
            holder.status.setTextColor(context.getColor(R.color.green)); // Green
            holder.status.setBackgroundTintList(context.getColorStateList(R.color.green_tint));
        } else {
            holder.status.setText("Offline");
            holder.status.setTextColor(context.getColor(R.color.red)); // Green
            holder.status.setBackgroundTintList(context.getColorStateList(R.color.red_tint));        }

         holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeviceLongPress(device,context);
            }
        });



    }
    private void forgetWifiNetwork(String ssid) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ cannot remove networks directly
                // But we can disable auto-join and trigger a disconnect
                WifiNetworkSuggestion suggestion =
                        new WifiNetworkSuggestion.Builder()
                                .setSsid(ssid)
                                .setIsAppInteractionRequired(true)
                                .build();

                wifiManager.removeNetworkSuggestions(
                        Collections.singletonList(suggestion)
                );
            } else {
                // Android 9 and below – direct removal
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                for (WifiConfiguration i : list) {
                    if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                        wifiManager.removeNetwork(i.networkId);
                        wifiManager.saveConfiguration();
                    }
                }
            }

            wifiManager.disconnect();
        } catch (Exception e) {
            Log.e("forgetWifi", "Error forgetting WiFi: " + e.getMessage());
        }
    }



    private void onDeviceLongPress(Device device, Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.device_menu);
        TextView device_name = dialog.findViewById(R.id.device_name);
        device_name.setText(device.getName());
        RelativeLayout rename_device = dialog.findViewById(R.id.rename_device);
         RelativeLayout remove_device = dialog.findViewById(R.id.remove_device);
         RelativeLayout connect_device = dialog.findViewById(R.id.connect_device);
        LinearLayout cancel = dialog.findViewById(R.id.cancel);
        if (isDeviceConnected(device)) {
            connect_device.setVisibility(GONE);
        } else {
            connect_device.setVisibility(View.VISIBLE);
        }
// Make background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            // Align bottom
            Window window = dialog.getWindow();
            window.setGravity(Gravity.BOTTOM);

            // Optional: add animation and dim effectSpla
            window.getAttributes().windowAnimations = R.style.DialogSlideAnimation;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
        }

        dialog.show();
        rename_device.setVisibility(GONE);
        rename_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showRenameDialog(device);
            }
        });
        remove_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                forgetWifiNetwork(device.getName());
                deviceList.remove(device);
                notifyDataSetChanged();
                wifiConnector.forgetDevice(device.getName());
                //openWifiSettings();
                //showRemoveDialog(device);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        connect_device.setOnClickListener(v -> {
            dialog.dismiss();
            if(!TextUtils.isEmpty(device.getPassword())){
                wifiConnector.connectToWifi(device.getName(),device.getPassword());

            }else {
                showWifiPasswordDialog(device.getName(),device.getCustom_name());

            }
           /* if(device.getName().startsWith("node")){
                wifiConnector.connectToWifi(device.getName(),"12345678");
            }else{
                wifiConnector.connectToWifi(device.getName(),"Ramz1234");

            }*/

           //  openWifiSettings();
            /*Intent intent = new Intent(context, WifiActivity.class);
            intent.putExtra("ssid",device.getName());
            intent.putExtra("ip",device.getIp());
            context.startActivity(intent);*/

            // showConnectDialog(device);
        });

    }
    private boolean isDeviceConnected(Device device) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replace("\"", "");
        return ssid.equals(device.getName());
    }
    private void openWifiSettings() {
      ///  Toast.makeText(context, "Select WiFi manually (Android requirement)", Toast.LENGTH_LONG).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
            context.startActivity(panelIntent);
        } else {
            context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
    }



    private void showRemoveDialog(Device device) {
        new AlertDialog.Builder(context)
                .setTitle("Remove Device")
                .setMessage("Are you sure you want to reset this device?")
                .setPositiveButton("Yes", (d, w) -> {
                    /*DeviceDatabaseHelper db = new DeviceDatabaseHelper(context);
                    db.removeDevice(device.getIp());
                    deviceList.remove(device);
                    notifyDataSetChanged(); */
                    forgetWifiNetwork(device.getName());
                    deviceList.remove(device);
                    notifyDataSetChanged();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showWifiPasswordDialog(String ssid,String rename) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.activity_wifi_connect, null);  // ⬅ replace name

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Get views
        TextView deviceName = dialogView.findViewById(R.id.device_name);
        EditText edtPassword = dialogView.findViewById(R.id.password);
        LinearLayout btnConnect = dialogView.findViewById(R.id.btnConnect);

        // Set device name
        if(!TextUtils.isEmpty(rename)){
            deviceName.setText(rename);

        }else{
            deviceName.setText(ssid);

        }

        // Handle connect click
        btnConnect.setOnClickListener(v -> {
            String pwd = edtPassword.getText().toString().trim();

            if (pwd.isEmpty()) {
                edtPassword.setError("Enter password");
                return;
            }


            DeviceDatabaseHelper databaseHelper = new DeviceDatabaseHelper(context);
            databaseHelper.getWritableDatabase();

            databaseHelper.updateNewPass(ssid,pwd);

            // Call your method → connect to WiFi
            wifiConnector.connectToWifi(ssid,pwd);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void showRenameDialog(Device device) {
        EditText input = new EditText(context);
        String name = device.getName();
        if(!TextUtils.isEmpty(device.getCustom_name())){
            name = device.getCustom_name();
        }

        input.setText(name);
        new AlertDialog.Builder(context)
                .setTitle("Rename Device")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        DeviceDatabaseHelper db = new DeviceDatabaseHelper(context);
                        db.getWritableDatabase();
                        db.renameDevice(device.getIp(),device.getName(), newName);
                        device.setCustom_name(newName);

                        notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void onDeviceClick(Device device) {
        Intent intent = new Intent(context, DeviceControlActivity.class);
        intent.putExtra("DEVICE_NAME", device.getName());
        intent.putExtra("DEVICE_MAC", device.getIp());
        context.startActivity(intent);
    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

     static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView name, ip, status;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.deviceName);
            ip = itemView.findViewById(R.id.deviceIp);
            status = itemView.findViewById(R.id.deviceStatus);
        }
    }

}
