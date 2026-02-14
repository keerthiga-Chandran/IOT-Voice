package com.proz.projectiot.Devices;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.proz.projectiot.R;

public class DeviceControlActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        String name = getIntent().getStringExtra("DEVICE_NAME");
        String mac = getIntent().getStringExtra("DEVICE_MAC");

        TextView info = findViewById(R.id.deviceInfo);
        info.setText("Connected to: " + name + "\nMAC: " + mac);
    }
}