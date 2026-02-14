package com.proz.projectiot;

import static android.view.View.GONE;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.InputStream;
import android.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.proz.projectiot.Common.CommonClass;
import com.proz.projectiot.Devices.DeviceListActivity;
import com.proz.projectiot.Settings.SettingActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ControllerOne extends Activity implements
        RecognitionListener {
    TextView wifiStatusText;
    private boolean commandSent = false; // add this as a class variable

    private final String ESP32_IP = "192.168.4.1"; // ESP32 IP
    private AlertDialog voiceDialog;
    ImageView btnForward,btnRight,btnBackward,btnLeft,filter,settings;
    Button btnStop;
    LinearLayout btnVoice,info_layout;
    ImageView layout_home,stopBtn;
    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private TextView resultView,ip_address,connection_status,connected_text,title,battery_value,strength;
    CommonClass commonClass = new CommonClass();
    ImageView back;

    private boolean doubleBackToExitPressedOnce = false;
    private Handler handler = new Handler();
    GoogleSignInAccount account;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onStart() {
        super.onStart();
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.d("GOOGLE_AUTH_spl", "Already signed in: " + account.getEmail());
        }
    }
    private Runnable permissionRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("permissionRunnable","called runnable class ");
            requestPermission();  // call your method
            handler.postDelayed(this, 60 * 1000); // repeat every 1 minute
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("permissionRunnable","onresume ");
        handler.post(permissionRunnable); // start loop
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("permissionRunnable","onpause");
        handler.removeCallbacks(permissionRunnable); // stop loop
    }
    @Override
    public void onBackPressed() {
        Log.d("permissionRunnable","back pressed ");
        if (doubleBackToExitPressedOnce) {
            finishAffinity();  // closes the app
            System.exit(0);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000); // 2 seconds timeout
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("permissionRunnable","called runnable class ");
        setContentView(R.layout.controller_one);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1044956637806-1el9i4pn4o1i7cdp6f6l787jg3f2lt3t.apps.googleusercontent.com")   // IMPORTANT
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        initView();
        requestPermission();

        setUiState(STATE_START);
        LibVosk.setLogLevel(LogLevel.INFO);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initModel();
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("permissionRunnable","request permisison");

                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 100);
                return;
            }else{
                if(checkAndRequestLocation()){
                    checkWifiConnection();
                }

            }
        }else{
            if(checkAndRequestLocation()){
                checkWifiConnection();
            }
        }

    }

    public void initView(){
        Log.d("permissionRunnable","initVire");
        settings = findViewById(R.id.settings);
        filter = findViewById(R.id.filter);
        strength= findViewById(R.id.strength);
        title = findViewById(R.id.title);
        battery_value = findViewById(R.id.battery_value);
        battery_value.setText(commonClass.getBatteryPercentage(ControllerOne.this)+"%");
        title.setText("Mode 1");
        back = findViewById(R.id.back);
        back.setVisibility(GONE);
        layout_home = findViewById(R.id.layout_home);
        stopBtn = findViewById(R.id.stopBtn);
        connected_text = findViewById(R.id.connected_text);
        connection_status = findViewById(R.id.connection_status);
        ip_address = findViewById(R.id.ip_address);
        info_layout = findViewById(R.id.info_layout);
        wifiStatusText = findViewById(R.id.ssidText);
        btnStop = findViewById(R.id.btnStop);
        resultView = findViewById(R.id.resultView);
        btnBackward = findViewById(R.id.btnBackward);
        btnForward = findViewById(R.id.btnForward);
        btnRight = findViewById(R.id.btnRight);
        btnLeft = findViewById(R.id.btnLeft);
        btnVoice = findViewById(R.id.btnVoice);
        checkWifiConnection();
        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showVoiceDialog();
                //recognizeMicrophone();
            }
        });
        btnBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("back");
            }
        });
        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("front");
            }
        });
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("left");
            }
        });
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("right");
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("stop");
            }
        });
        info_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomDialog();
            }
        });
        layout_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               sendBack();
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(account!=null){
                    signOut();
                }else{
                    Toast.makeText(getApplicationContext(),"You are not Logged in..",Toast.LENGTH_SHORT).show();
                }
              //  sendCommand("stop");
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBack();
            }
        });
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DeviceListActivity.class);
                intent.putExtra("mode","one");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
     }

    private void sendBack() {
        Log.d("permissionRunnable","send back");
        Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

    }

    private void showCustomDialog() {
        Log.d("permissionRunnable","show custom dialog");
        Dialog dialog = new Dialog(ControllerOne.this);
        dialog.setContentView(R.layout.popup_info);

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

    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    private void initModel() {
        Log.d("permissionRunnable"," initModal ");
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                initModel();
            } else {
                finish();
            }
        }else    if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                checkWifiConnection();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission required for WiFi SSID", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onResult(String hypothesis) {
        //resultView.append(hypothesis + "\n");
    }

    @Override
    public void onFinalResult(String hypothesis) {
        if (commandSent) return;
       // resultView.append(hypothesis + "\n");
        Log.d("permissionRunnable"," on final resu;lt "+hypothesis);
        try {
            JSONObject json = new JSONObject(hypothesis);
            String command = json.optString("text", "").trim().toLowerCase();

            if (!command.isEmpty()) {
                Log.d("receiving_text", "Final result command: " + command);

                if (command.contains("front") || command.startsWith("fr")) {
                    sendCommand("front");
                } else if (command.contains("back") || command.startsWith("ba")) {
                    sendCommand("back");
                } else if (command.contains("left")  || command.startsWith("le")) {
                    sendCommand("left");
                } else if (command.contains("right") || command.startsWith("ri")) {
                    sendCommand("right");
                } else if (command.contains("stop") || command.startsWith("st")) {
                    sendCommand("stop");
                } else {
                    Log.d("receiving_text", "Unrecognized command: " + command);
                }

                // Stop recognizer
                if (speechService != null) {
                    speechService.stop();
                    speechService.shutdown();
                    speechService = null;
                }
                if (voiceDialog != null && voiceDialog.isShowing()) {
                    voiceDialog.dismiss();
                }
                setUiState(STATE_DONE);
                commandSent = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        if (commandSent) return;
       // resultView.append(hypothesis + "\n");
        Log.d("permissionRunnable"," onpartially as "+hypothesis);
        try {
            JSONObject json = new JSONObject(hypothesis);
            String partial = json.optString("partial", "").trim().toLowerCase();

            if (!partial.isEmpty()) {
                Log.d("receiving_text", "Partial command: " + partial);


                if (partial.contains("front")|| partial.startsWith("fr")) {
                    sendCommand("front");
                    commandSent = true;
                } else if (partial.contains("back") || partial.startsWith("ba")) {
                    sendCommand("back");
                    commandSent = true;
                } else if (partial.contains("left") || partial.startsWith("le")) {
                    sendCommand("left");
                    commandSent = true;
                } else if (partial.contains("right")|| partial.startsWith("ri")) {
                    sendCommand("right");
                    commandSent = true;
                } else if (partial.contains("stop")|| partial.startsWith("st")) {
                    sendCommand("stop");
                    commandSent = true;
                } else {
                    Log.d("receiving_text", "Unrecognized command: " + partial);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }
    private void showVoiceDialog() {
        Log.d("permissionRunnable","show voice dialog");
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.alert_dialog, null);
        builder.setView(dialogView);
        builder.setCancelable(true); // user cannot dismiss manually

        voiceDialog = builder.create();
        voiceDialog.show();

        TextView tvListeningStatus = dialogView.findViewById(R.id.tvListeningStatus);
        TextView tvRecognizedText = dialogView.findViewById(R.id.tvRecognizedText);
        ImageButton btnMic = dialogView.findViewById(R.id.btnMic);

         this.resultView = tvRecognizedText;

        commandSent = false;
        recognizeMicrophone();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkWifiConnection() {
        Log.d("permissionRunnable","connection calling");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.d("permissionRunnable"," info "+wifiInfo);
        if (wifiInfo != null && wifiInfo.getSSID() != null && !wifiInfo.getSSID().equals("<unknown ssid>")) {
            String ssid = wifiInfo.getSSID().replace("\"", "");
            int ipAddress = wifiInfo.getIpAddress();
            String ip = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
            int rssi = wifiInfo.getRssi(); // typically between -100 and 0
            String signalQuality = getSignalQuality(rssi);
            strength.setText(signalQuality);
            wifiStatusText.setText("Connected to: " + ssid  );
            ip_address.setText(ip);
            connection_status.setBackgroundTintList(getApplicationContext().getColorStateList(R.color.green));

        } else {
            Log.d("permissionRunnable","wifi not connected");
            wifiStatusText.setText("Not connected to Wi-Fi");
            connected_text.setText("Not connected");
            connection_status.setBackgroundTintList(getApplicationContext().getColorStateList(R.color.red));
           // showWifiDisconnectedAlert();
        }
    }
    private boolean checkAndRequestLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gpsEnabled) {
            showEnableLocationDialog();
            return false;
        }

        return true;
    }

    private void showEnableLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("To get Wi-Fi SSID, please turn ON Location (GPS).")
                .setCancelable(false)
                .setPositiveButton("Turn On", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getSignalQuality(int rssi) {
        if (rssi >= -50) {
            return "Excellent";
        } else if (rssi >= -60) {
            return "Good";
        } else if (rssi >= -70) {
            return "Moderate";
        } else if (rssi >= -80) {
            return "Weak";
        } else {
            return "Very Weak";
        }
    }
    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Toast.makeText(ControllerOne.this, "Signed out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
        });
    }

    private void showWifiDisconnectedAlert() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
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
    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
               // resultView.setText(R.string.preparing);
                resultView.setMovementMethod(new ScrollingMovementMethod());
                //findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.btnVoice).setEnabled(false);
               // findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_READY:
              //  resultView.setText(R.string.ready);
                //((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
              //  findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.btnVoice).setEnabled(true);
               // findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_DONE:
               // ((Button) findViewById(R.id.recognize_file)).setText(R.string.recognize_file);
              //  ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
               // findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.btnVoice).setEnabled(true);
                //findViewById(R.id.pause).setEnabled((false));
                //((ToggleButton) findViewById(R.id.pause)).setChecked(false);
                break;
            case STATE_FILE:
                //((Button) findViewById(R.id.recognize_file)).setText(R.string.stop_file);
              //  resultView.setText(getString(R.string.starting));
               // findViewById(R.id.recognize_mic).setEnabled(false);
                findViewById(R.id.recognize_file).setEnabled(true);
                //findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_MIC:
             //   ((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
              //  resultView.setText(getString(R.string.say_something));
               // findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.btnVoice).setEnabled(true);
               // findViewById(R.id.pause).setEnabled((true));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }
    private void sendCommand(String command) {
        Log.d("permissionRunnable","sending command "+command);

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        if (voiceDialog != null && voiceDialog.isShowing()) {
            voiceDialog.dismiss();
        }
        setUiState(STATE_DONE);


        Log.d("receiving_text"," command "+command);

        OkHttpClient client = new OkHttpClient();

        String url = "http://" + ESP32_IP + "/" + command;
        //Toast.makeText(getApplicationContext()," url "+url,Toast.LENGTH_SHORT).show();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Log.d("receiving_text"," url "+url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("receiving_text", "Failed to connect", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                runOnUiThread(() -> {
                    new AlertDialog.Builder(ControllerOne.this)
                            .setTitle("Device Response")
                            .setMessage("Command: " + command + "\nResponse: " + responseText)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });


    }
    private void setErrorState(String message) {
        resultView.setText(message);
       // ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        //findViewById(R.id.recognize_file).setEnabled(false);
      //  findViewById(R.id.recognize_mic).setEnabled(false);
    }

    private void recognizeFile() {
        if (speechStreamService != null) {
            setUiState(STATE_DONE);
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            setUiState(STATE_FILE);
            try {
                Recognizer rec = new Recognizer(model, 16000.f, "[\"one zero zero zero one\", " +
                        "\"oh zero one two three four five six seven eight nine\", \"[unk]\"]");

                InputStream ais = getAssets().open(
                        "10001-90210-01803.wav");
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 16000);
                speechStreamService.start(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    private void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        } else {
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }


    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }
}
