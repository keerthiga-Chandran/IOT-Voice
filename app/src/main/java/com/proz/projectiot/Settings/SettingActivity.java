package com.proz.projectiot.Settings;

import static android.view.View.GONE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.proz.projectiot.Common.CommonClass;
import com.proz.projectiot.ControllerOne;
import com.proz.projectiot.ControllerTwo;
import com.proz.projectiot.Devices.DeviceListActivity;
import com.proz.projectiot.GoogleOauth.GoogleOauth;
import com.proz.projectiot.R;
import com.proz.projectiot.SplashScreen;

public class SettingActivity extends Activity implements View.OnClickListener {
    ImageView back,settings,filter,logout;
    TextView title;
    RadioButton mode_one,mode_two;
    LinearLayout make_default;
    CommonClass commonClass = new CommonClass();
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;
    GoogleSignInAccount account;
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
        setContentView(R.layout.activity_settings);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1044956637806-1el9i4pn4o1i7cdp6f6l787jg3f2lt3t.apps.googleusercontent.com")   // IMPORTANT
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        initView();
    }
    public void  initView(){
        logout= findViewById(R.id.logout);
         back = findViewById(R.id.back);
        settings = findViewById(R.id.settings);
        settings.setVisibility(GONE);

        account = GoogleSignIn.getLastSignedInAccount(this);
        if(account==null){
            logout.setVisibility(GONE);
        }else{
            logout.setVisibility(View.VISIBLE);
        }
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        filter = findViewById(R.id.filter);
        title = findViewById(R.id.title);
        title.setText("Settings");
        mode_one = findViewById(R.id.mode_one);
        mode_two = findViewById(R.id.mode_two);
        make_default = findViewById(R.id.make_default);
        back.setOnClickListener(this);
        filter.setOnClickListener(this);
        make_default.setOnClickListener(this);
        mode_two.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mode_two.setChecked(true);
                    mode_one.setChecked(false);
                    commonClass.putSharedPref(getApplicationContext(), "mode", "mode2");
                }
            }
        });

        mode_one.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mode_one.setChecked(true);
                    mode_two.setChecked(false);
                }
            }
        });
        String savedMode = commonClass.getSharedPref(getApplicationContext(), "mode");

        if (!TextUtils.isEmpty(savedMode)) {
            if (savedMode.equals("mode1")) {
                mode_one.setChecked(true);
                mode_two.setChecked(false);
            } else {
                mode_one.setChecked(false);
                mode_two.setChecked(true);
            }
        } else {
            mode_one.setChecked(true);
            mode_two.setChecked(false);
        }

    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Toast.makeText(SettingActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.make_default){
            String txt ="Mode 1 Activated" ;
            Intent intent = null;
            if(mode_one.isChecked()){
                txt ="Mode 1 Activated" ;
                commonClass.putSharedPref(getApplicationContext(), "mode", "mode1");
                intent = new Intent(getApplicationContext(), ControllerOne.class);
            }else{
                txt ="Mode 2 Activated" ;
                commonClass.putSharedPref(getApplicationContext(), "mode", "mode2");
                intent = new Intent(getApplicationContext(), ControllerTwo.class);
            }
            Toast.makeText(getApplicationContext(),txt,Toast.LENGTH_SHORT).show();
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }else if(id==R.id.back){
            finish();
        }else if(id==R.id.filter){
            startActivity(new Intent(getApplicationContext(), DeviceListActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }
}
