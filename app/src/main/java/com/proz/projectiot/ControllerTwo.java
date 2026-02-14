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

public class ControllerTwo extends Activity implements
        RecognitionListener {
    TextView wifiStatusText,ip_address,connection_status,connected_text;
    private boolean commandSent = false; // add this as a class variable

    private final String ESP32_IP = "192.168.4.1"; // ESP32 IP
    private AlertDialog voiceDialog;
     LinearLayout btnVoice,lforward,lstop,lbackward,rforward,rstop,rbackward;
    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;
    LinearLayout info_layout;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private TextView resultView,strength,battery_value,title;
    ImageView back,filter,settings,stopBtn,layout_home;

    CommonClass commonClass = new CommonClass();

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
            requestPermission();  // call your method
            handler.postDelayed(this, 60 * 1000); // repeat every 1 minute
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        handler.post(permissionRunnable); // start loop
    }
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(permissionRunnable); // stop loop
    }
    @Override
    public void onBackPressed() {
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_two);
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
                Toast.makeText(getApplicationContext(),"Permission Not Enabled",Toast.LENGTH_SHORT).show();

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initView(){
        settings = findViewById(R.id.settings);
        back = findViewById(R.id.back);
        back.setVisibility(GONE);
        filter = findViewById(R.id.filter);
        title = findViewById(R.id.title);
        title.setText("Mode 2");
        layout_home = findViewById(R.id.layout_home);
        strength= findViewById(R.id.strength);
        stopBtn = findViewById(R.id.stopBtn);
        info_layout = findViewById(R.id.info_layout);
        battery_value = findViewById(R.id.battery_value);
        battery_value.setText(commonClass.getBatteryPercentage(ControllerTwo.this)+"%");
        connected_text = findViewById(R.id.connected_text);
        connection_status = findViewById(R.id.connection_status);
        ip_address = findViewById(R.id.ip_address);
        wifiStatusText = findViewById(R.id.ssidText);
        lforward = findViewById(R.id.lforward);
        resultView = findViewById(R.id.resultView);
        lstop = findViewById(R.id.lstop);
        lbackward = findViewById(R.id.lbackward);
        rforward = findViewById(R.id.rforward);
        rbackward = findViewById(R.id.rbackward);
        rstop = findViewById(R.id.rstop);
        btnVoice = findViewById(R.id.btnVoice);
        checkWifiConnection();
        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showVoiceDialog();
                //recognizeMicrophone();
            }
        });
        layout_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callSplash();
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callSplash();
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* sendCommand("rstop");
                sendCommand("lstop"); */
                if(account!=null){
                    signOut();
                }else{
                    Toast.makeText(getApplicationContext(),"You are not Logged in..",Toast.LENGTH_SHORT).show();
                }

            }
        });
        info_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomDialog();
            }
        });
        lforward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("lfront");
            }
        });
        lstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("lstop");
            }
        });
        lbackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("lback");
            }
        });
        rforward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("rfront");
            }
        });
        rstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("rstop");
            }
        });
        rbackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("rback");
            }
        });
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), DeviceListActivity.class);
                intent.putExtra("mode","two");
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
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void callSplash() {
        Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Toast.makeText(ControllerTwo.this, "Signed out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
        });
    }
    private void showCustomDialog() {
        Dialog dialog = new Dialog(ControllerTwo.this);
        dialog.setContentView(R.layout.mode_two_pop);

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

            // Optional: add animation and dim effect
            window.getAttributes().windowAnimations = R.style.DialogSlideAnimation;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
        }

        dialog.show();

    }


    private void initModel() {
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
        Log.d("receiving_text"," on final resu;lt "+hypothesis);
        try {
            JSONObject json = new JSONObject(hypothesis);
            String command = json.optString("text", "").trim().toLowerCase();
            command =command.replace(" ","");

            if (!command.isEmpty()) {
               // Toast.makeText(getApplicationContext(),"Command "+command,Toast.LENGTH_SHORT).show();
                Log.d("receiving_text", "Final result command: " + command);
                String[] words = command.trim().toLowerCase().split("\\s+");
                String first = words[0];
                if (command.matches(".*(lfront|l\\s*fr|left\\s*(a\\s*)?fr).*") || command.contains("lfront") || command.contains("lf")) {
                    sendCommand("lfront");
                } else if (command.matches(".*(lback|l\\s*ba|left\\s*(a\\s*)?ba).*") || command.contains("lback")|| command.contains("lb")) {
                    sendCommand("lback");
                } else if (command.matches(".*(lstop|l\\s*st|left\\s*(a\\s*)?st).*") || command.contains("lstop")|| command.contains("ls")) {
                    sendCommand("lstop");
                } else if (command.matches(".*(rfront|r\\s*fr|right\\s*(a\\s*)?fr).*") || command.contains("rfront")||
                        command.contains("rf") || (first.contains("r") && command.matches(".*fr.*"))) {
                    sendCommand("rfront");
                } else if (command.matches(".*(rback|r\\s*ba|right\\s*(a\\s*)?ba).*") || command.contains("rback")||
                        command.contains("rb") || (first.contains("r") && command.matches(".*ba.*"))) {
                    sendCommand("rback");
                } else if (command.matches(".*(rstop|r\\s*st|right\\s*(a\\s*)?st).*") || command.contains("rstop")||
                        command.contains("rs") || (first.contains("r") && command.matches(".*st.*"))) {
                    sendCommand("rstop");
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
        Log.d("receiving_text"," onpartially as "+hypothesis);
        try {
            JSONObject json = new JSONObject(hypothesis);
            String command = json.optString("partial", "").trim().toLowerCase();
            command =command.replace(" ","");
            if (!command.isEmpty()) {
                Log.d("receiving_text", "Partial command: " + command);
                String[] words = command.trim().toLowerCase().split("\\s+");
                String first = words[0];
                //Toast.makeText(getApplicationContext(),"Command "+command,Toast.LENGTH_SHORT).show();
                if (command.matches(".*(lfront|l\\s*fr|left\\s*(a\\s*)?fr).*") || command.contains("lfront")
                        || command.contains("lf") || (first.startsWith("l") && (command.matches(".*fr.*") || command.matches(".*for.*")
                || command.matches(".*f.*")) )) {
                    sendCommand("lfront");
                    commandSent = true;
                } else if (command.matches(".*(lback|l\\s*ba|left\\s*(a\\s*)?ba).*") || command.contains("lback")
                || command.contains("lb") || command.contains("l b")|| command.contains("lb")||
                        (first.startsWith("l") && command.matches(".*ba.*"))) {
                    sendCommand("lback");
                    commandSent = true;
                } else if (command.matches(".*(lstop|l\\s*st|left\\s*(a\\s*)?st).*") || command.contains("lstop")
                        || command.contains("ls")|| (first.startsWith("l") && command.matches(".*st.*"))) {
                    sendCommand("lstop");
                    commandSent = true;
                } else if (command.matches(".*(rfront|r\\s*fr|right\\s*(a\\s*)?fr).*")|| command.contains("rfront")
                        || command.contains("rf") || (first.startsWith("r") && (command.matches(".*fr.*") || command.matches(".*for.*")
                        || command.matches(".*f.*")) )) {
                    sendCommand("rfront");
                    commandSent = true;
                } else if (command.matches(".*(rback|r\\s*ba|right\\s*(a\\s*)?ba).*")|| command.contains("rback")
                        || command.contains("rb") || (first.startsWith("r") && command.matches(".*ba.*"))) {
                    sendCommand("rback");
                    commandSent = true;
                } else if (command.matches(".*(rstop|r\\s*st|right\\s*(a\\s*)?st).*")|| command.contains("rstop")
                        || command.contains("rs") || (first.startsWith("r") && command.matches(".*st.*"))) {
                    sendCommand("rstop");
                    commandSent = true;
                } else {
                    callWordSplitUp(command);
                    Log.d("receiving_text", "Unrecognized command: " + command);
                }




            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callWordSplitUp(String command) {
        Log.d("receiving_text"," comman "+command.trim().toLowerCase());
      /*  String[] words = command.trim().toLowerCase().split("\\s+");

        if (words.length > 1) {
            String side = words[0];              // "left" or "right"
            char next = words[1].charAt(0);      // first letter of 2nd word

            if (side.startsWith("l")) {
                if (next == 'f') {
                    sendCommand("lfront");
                } else if (next == 'b') {
                    sendCommand("lback");
                } else if (next == 's') {
                    sendCommand("lstop");
                }
                commandSent =true;
            } else if (side.startsWith("r")) {
                if (next == 'f') {
                    sendCommand("rfront");
                } else if (next == 'b') {
                    sendCommand("rback");
                } else if (next == 's') {
                    sendCommand("rstop");
                }
                commandSent =true;
            } else {
                Log.d("receiving_text", "Unrecognized command: " + command);
            }
        } else {
            Log.d("receiving_text", "Unrecognized command: " + command);
        } */

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
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

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
            wifiStatusText.setText("Not connected to Wi-Fi");
            connected_text.setText("Not connected");
            connection_status.setBackgroundTintList(getApplicationContext().getColorStateList(R.color.red));
          //  showWifiDisconnectedAlert();
        }
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
       // Toast.makeText(getApplicationContext(),"command "+command,Toast.LENGTH_SHORT).show();
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
        Log.d("receiving_text"," url "+url);
        //Toast.makeText(getApplicationContext()," url "+url,Toast.LENGTH_SHORT).show();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ESP32_ERROR", "Failed to connect", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                runOnUiThread(() -> {
                    new AlertDialog.Builder(ControllerTwo.this)
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
