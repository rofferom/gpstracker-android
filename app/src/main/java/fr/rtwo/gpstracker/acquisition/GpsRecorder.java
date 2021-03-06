package fr.rtwo.gpstracker.acquisition;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import fr.rtwo.gpstracker.Config;
import fr.rtwo.gpstracker.logs.Telemetry;
import fr.rtwo.gpstracker.utils.Timer;

public class GpsRecorder {
    private static final String TAG = "GpsRecorder";

    private enum State {
        stopped,
        started
    }

    // General attributes
    private State mState = State.stopped;
    private Listener mListener;
    private Telemetry mTelemetry;

    private float mGpsAccuracy;
    private long mGpsAcqTimeout;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation = null;
    private Timer mTimeoutTimer = new Timer();
    private boolean mManualAcq = false;

    public interface Listener {
        void onNewLocation(Location location, boolean isManualAcq);
        void onTimeout();
    }

    GpsRecorder(Context context, Listener listener, Telemetry telemetry) {
        Config config = Config.getInstance();

        mGpsAccuracy = config.getGpsAccuracy();
        mGpsAcqTimeout = config.getGpsAcqTimeout();

        mListener = listener;
        mTelemetry = telemetry;

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

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

    public void start() {
        Log.i(TAG, "Start acquisition");
        mTelemetry.write(Telemetry.GPS_TAG, Telemetry.GPS_START_ACQ);

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener);
        } catch (SecurityException e) {
            Log.i(TAG, "mLocationManager.requestLocationUpdates() rejected : " + e.getMessage());
            return;
        }

        mState = State.started;

        // Arm timeout timer
        mTimeoutTimer.set(
            new Timer.Listener() {
                @Override
                public void onExpired() {
                    onLocationTimeout();
                }
            },
            mGpsAcqTimeout * 1000);
    }

    public void stop() {
        Log.i(TAG, "Stop acquisition");
        mTelemetry.write(Telemetry.GPS_TAG, Telemetry.GPS_STOP_ACQ);

        // Clear current acquisition context
        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (SecurityException e) {
            Log.i(TAG, "mLocationManager.removeUpdates() rejected : " + e.getMessage());
        }

        mState = State.stopped;
        mManualAcq = false;
        mLastLocation = null;

        mTimeoutTimer.clear();
    }

    public void manualAcq() {
        Log.i(TAG, "Request manual acquisition");
        mManualAcq = true;
        if (mState == State.stopped)
            start();
        else
            Log.i(TAG, "Acquisition already in progress");
    }

    private void onNewLocation(Location location) {
        float accuracy = location.getAccuracy();

        mTelemetry.write(
                Telemetry.GPS_TAG,
                String.format(
                        Telemetry.GPS_LOCATION_FORMAT,
                        location.getTime(),
                        location.getLatitude(),
                        location.getLongitude(),
                        accuracy,
                        location.getSpeed()));

        Log.d(TAG, "New position: " + location.getLatitude() + ", " + location.getLongitude());
        if (accuracy <= mGpsAccuracy) {
            Log.d(TAG, "Position stored (accuracy : " + accuracy + ")");

            mTelemetry.write(Telemetry.GPS_TAG, Telemetry.GPS_VALID_POINT);

            if (mListener != null)
                mListener.onNewLocation(location, mManualAcq);

            stop();
        } else {
            Log.d(TAG, "Better accuracy required (accuracy : " + accuracy + ")");
            mLastLocation = location;
        }
    }

    private void onLocationTimeout() {
        float accuracy = mLastLocation != null ? mLastLocation.getAccuracy() : Float.NaN;

        Log.i(TAG, "Location acquisition timeout (better accuracy : " + accuracy + ")");

        mTelemetry.write(Telemetry.GPS_TAG, Telemetry.GPS_TIMEOUT);

        if (mListener != null)
            mListener.onTimeout();

        stop();
    }
}
