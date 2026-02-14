package com.proz.projectiot.SQLIte;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.proz.projectiot.Devices.Modal.Device;

import java.util.ArrayList;
import java.util.List;

public class DeviceDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "devices.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "device_table";
    private static final String COLUMN_ID = "id";
    private  static final String COLUMN_Pass ="password";
    private static final String COLUMN_SSID = "ssid";
    private static final String COLUMN_BSSID = "bssid";
    private static final String COLUMN_CUSTOM_NAME = "custom_name";
    private static final String COLUMN_IS_REMOVED = "is_removed";

    public DeviceDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SSID + " TEXT, " +
                COLUMN_BSSID + " TEXT UNIQUE, " +
                COLUMN_CUSTOM_NAME + " TEXT, " +
                COLUMN_Pass + " TEXT, " +
                COLUMN_IS_REMOVED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 🔹 Insert or Update device
    public boolean insertOrUpdateDevice(String ssid, String bssid) {
        Log.d("inertupdate"," ssid name "+ssid+" bssid ip "+bssid);
        if (isRemoved(bssid)) return false;

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_NAME + " WHERE " + COLUMN_BSSID + "=?",
                new String[]{bssid}
        );

        if (cursor.moveToFirst()) {
            cursor.close();
            return true; // already exists
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_SSID, ssid);
        values.put(COLUMN_BSSID, bssid);
        values.put(COLUMN_IS_REMOVED, 0);

        db.insert(TABLE_NAME, null, values);

        cursor.close();
        return true;
    }

    public void updatePasswordNull(String ssid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", (String) null);

        db.update(TABLE_NAME, cv, "ssid = ?", new String[]{ssid});
    }
    public void updateNewPass(String ssid,String pass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", pass);

        db.update(TABLE_NAME, cv, "ssid = ?", new String[]{ssid});
    }

    // 🔹 Rename device
    public void renameDevice(String bssid,String name, String newName) {
        Log.d("inertupdate"," bssid "+bssid+" name "+name+" update name "+newName);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUSTOM_NAME, newName);
        int row =db.update(TABLE_NAME, values, COLUMN_BSSID + "=? AND " + COLUMN_SSID + "=?", new String[]{bssid,name});
        Log.d("inertupdate"," update stuatus "+row);
    }

    // 🔹 Mark device as removed
    public void removeDevice(String bssid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_REMOVED, 1);
        db.update(TABLE_NAME, values, COLUMN_BSSID + "=?", new String[]{bssid});
    }

    // 🔹 Fetch active (non-removed) devices
    public List<Device> getAllActiveDevices() {
        List<Device> devices = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_IS_REMOVED + "=0",
                null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String ssid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SSID));
                String bssid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BSSID));
                String customName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOM_NAME));
                String password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Pass));

                String displayName = customName != null ? customName : ssid;
                devices.add(new Device(ssid, bssid, false,customName,password));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return devices;
    }

    // 🔹 Check if device is marked removed
    public boolean isRemoved(String bssid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_IS_REMOVED},
                COLUMN_BSSID + "=?", new String[]{bssid}, null, null, null);
        boolean removed = false;
        if (cursor.moveToFirst()) {
            removed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_REMOVED)) == 1;
        }
        cursor.close();
        return removed;
    }
}
