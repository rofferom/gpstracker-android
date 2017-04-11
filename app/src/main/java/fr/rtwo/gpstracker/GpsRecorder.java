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

    private enum State {
        stopped,
        started
    }

    // General attributes
    private Config mConfig = Config.getInstance();
    private Telemetry mTelemetry = Telemetry.getInstance();
    private State mState = State.stopped;

    // GPS acquisition
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation = null;
    private Timer mTimeoutTimer = new Timer();
    private Timer mNextAcqTimer = new Timer();

    // Output attributes
    private LocationDatabase mLocationDb = LocationDatabase.getInstance();
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

        // Open record file
        mEventLogger = new EventLogger();
        mEventLogger.open();

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                onNewLocation(location);
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

        stopAcquisition();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        startAcquisition();

        return START_STICKY;
    }

    private void startAcquisition() {
        Log.i(TAG, "Start acquisition");
        mTelemetry.write(Telemetry.GPS, "StartAcq");

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener);
        } catch (SecurityException e) {
            Log.i(TAG, "mLocationManager.requestLocationUpdates() rejected : " + e.getMessage());
            return;
        }

        mState = State.started;

        // Arm timeout timer
        mTimeoutTimer.set(
            mTimeoutTimer.new Listener() {
                @Override
                void onExpired() {
                    onLocationTimeout();
                }
            },
            mConfig.mGpsAcqTimeout * 1000);
    }

    private void stopAcquisition() {
        Log.i(TAG, "Stop acquisition");
        mTelemetry.write(Telemetry.GPS, "StopAcq");

        // Clear current acquisition context
        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (SecurityException e) {
            Log.i(TAG, "mLocationManager.removeUpdates() rejected : " + e.getMessage());
        }

        mState = State.stopped;
        mLastLocation = null;

        mTimeoutTimer.clear();
        mNextAcqTimer.clear();
    }

    private void stopAndQueueNewAcquisition() {
        stopAcquisition();

        mNextAcqTimer.set(
            mNextAcqTimer.new Listener() {
                @Override
                void onExpired() {
                    startAcquisition();
                }
            },
            mConfig.mGpsAcqPeriod * 1000);
    }

    private void onNewLocation(Location location) {
        float accuracy = location.getAccuracy();

        Log.d(TAG, "New position: " + location.getLatitude() + ", " + location.getLongitude());

        mTelemetry.write(
                Telemetry.GPS,
                String.format(
                        "ts:%d;lat:%f;long:%f;accuracy:%f;speed:%f",
                        location.getTime(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAccuracy(),
                        location.getSpeed()));

        if (accuracy <= mConfig.mGpsAccuracy) {
            Log.d(TAG, "Position stored (accuracy : " + accuracy + ")");

            mTelemetry.write(Telemetry.GPS, "ValidPoint");
            mLocationDb.addLocation(location);
            mEventLogger.recordLocation(location);

            stopAndQueueNewAcquisition();
        } else {
            Log.d(TAG, "Better accuracy required (accuracy : " + accuracy + ")");
            mLastLocation = location;
        }
    }

    private void onLocationTimeout() {
        float accuracy = mLastLocation != null ? mLastLocation.getAccuracy() : Float.NaN;

        Log.i(TAG, "Location acquisition timeout (better accuracy : " + accuracy + ")");
        mTelemetry.write(Telemetry.GPS, "Timeout");

        stopAndQueueNewAcquisition();
    }
}
