package com.proz.projectiot.Devices;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.proz.projectiot.R;

public class AddDeviceActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView back,scan_img;
    TextView title;
    LinearLayout start_scan,connect;
    EditText ip_address;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        initView();
    }
    public void  initView(){
        scan_img = findViewById(R.id.scan_img);
        start_scan = findViewById(R.id.start_scan);
        connect = findViewById(R.id.connect);
        ip_address = findViewById(R.id.ip_address);
        start_scan.setOnClickListener(this);
        connect.setOnClickListener(this);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        title.setText("Add Device");
        back.setOnClickListener(this);
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
         if(R.id.back == id){
             finish();
         }else if(R.id.connect == id){

         }else if(R.id.start_scan == id){
             rotateImage();
             new Handler().postDelayed(() -> {
                 Intent intent = new Intent(AddDeviceActivity.this, DeviceListActivity.class);
                 intent.putExtra("scan","sacn");
                 startActivity(intent);
                 finish(); // Optional: close this activity
             }, 3000);
         }

    }
    private void rotateImage() {
        // Rotate 0 → 360 degrees continuously
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(1000); // 1 second per rotation
        rotate.setRepeatCount(Animation.INFINITE);
        scan_img.startAnimation(rotate);
    }
}
