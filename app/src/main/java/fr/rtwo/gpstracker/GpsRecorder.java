package fr.rtwo.gpstracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class GpsRecorder extends Service {
    private static final String TAG = "GpsRecorder";

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private LocationDatabase mLocationDb;
    private EventLogger mEventLogger;

    public GpsRecorder() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        // Get reference to location database
        mLocationDb = LocationDatabase.getInstance();

        // Open record file
        mEventLogger = new EventLogger();
        mEventLogger.open();

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.d(TAG, "New position: " + location.getLatitude() + ", " + location.getLongitude());

                mLocationDb.addLocation(location);
                mEventLogger.recordLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");

        // Clear location database
        mLocationDb.clear();

        // Close record file
        mEventLogger.close();
        mEventLogger = null;

        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (SecurityException e) {
            Log.i(TAG, "mLocationManager.removeUpdates() rejected : " + e.getMessage());
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 0, mLocationListener);
        } catch (SecurityException e) {
            Log.i(TAG, "mLocationManager.requestLocationUpdates() rejected : " + e.getMessage());
        }

        return START_STICKY;
    }
}
