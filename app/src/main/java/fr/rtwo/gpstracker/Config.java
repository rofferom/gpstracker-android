package fr.rtwo.gpstracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    private static final String TAG = "Config";

    private static final String KEY_PREF_GPS_ACCURACY = "pref_gps_accuracy";
    private static final String KEY_PREF_GPS_PERIOD = "pref_gps_period";
    private static final String KEY_PREF_GPS_TIMEOUT = "pref_gps_timeout";

    private static final List<String> mKeyList = new ArrayList<String>() {{
        add(KEY_PREF_GPS_ACCURACY);
        add(KEY_PREF_GPS_PERIOD);
        add(KEY_PREF_GPS_TIMEOUT);
    }};

    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    private Config() {
    }

    public void loadValues(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (String key: mKeyList) {
            String value = sharedPreferences.getString(key, "");

            // Keep default value if the preference is not set
            if (value.isEmpty())
                continue;

            updateValue(key, value);
        }
    }

    public void updateValue(String key, String value) {
        try {
            if (key.equals(KEY_PREF_GPS_ACCURACY)) {
                mGpsAccuracy = Float.parseFloat(value);
                Log.i(TAG, "GPS accuracy changed to " + mGpsAccuracy);
            } else if (key.equals(KEY_PREF_GPS_PERIOD)) {
                mGpsAcqPeriod = Long.parseLong(value);
                Log.i(TAG, "GPS period changed to " + mGpsAcqPeriod);
            } else if (key.equals(KEY_PREF_GPS_TIMEOUT)) {
                mGpsAcqTimeout = Long.parseLong(value);
                Log.i(TAG, "GPS timeout changed to " + mGpsAcqTimeout);
            } else {
                Log.e(TAG, "Unknown preference '" + key + "'");
            }
        } catch (ClassCastException e) {
            Log.e(TAG, e.getMessage());
        } catch (NumberFormatException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    // GPS configuration
    private float mGpsAccuracy = 6.0f; // meters
    private long mGpsAcqPeriod = 60; // seconds
    private long mGpsAcqTimeout = 45; // seconds

    public float getGpsAccuracy() {
        return mGpsAccuracy;
    }

    public long getGpsAcqPeriod() {
        return mGpsAcqPeriod;
    }

    public long getGpsAcqTimeout() {
        return mGpsAcqTimeout;
    }

    // Battery configuration
    public final long mBatteryAcqPeriod = 120; // seconds
}
