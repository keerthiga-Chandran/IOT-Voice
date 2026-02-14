package com.proz.projectiot.Common;

import static androidx.core.content.ContextCompat.registerReceiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import com.proz.projectiot.SplashScreen;

public class CommonClass {
    public int getBatteryPercentage(Activity activity) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float) scale;
        return (int) batteryPct;
    }
    public void sendBack(Activity activity) {
        Intent intent = new Intent(activity, SplashScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
    public void putSharedPref(Context context, String tag, String value){
        SharedPreferences sp=context.getSharedPreferences(tag,0);
        SharedPreferences.Editor editor=sp.edit();
        editor.putString(tag,value);
        editor.apply();
        editor.commit();
        Log.d("getEmployeeList"," putted ");
    }
    public String getSharedPref(Context context,String tag){
        SharedPreferences sp=context.getSharedPreferences(tag,0);
        return  sp.getString(tag,null);
    }
}
