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
    private Config mConfig = Config.getInstance();
    private State mState = State.stopped;
    private Listener mListener;
    private Telemetry mTelemetry;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation = null;
    private Timer mTimeoutTimer = new Timer();

    public interface Listener {
        void onNewLocation(Location location);
        void onTimeout();
    }

    GpsRecorder(Context context, Listener listener, Telemetry telemetry) {
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
            new Timer.Listener() {
                @Override
                public void onExpired() {
                    onLocationTimeout();
                }
            },
            mConfig.mGpsAcqTimeout * 1000);
    }

    public void stop() {
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
    }

    private void onNewLocation(Location location) {
        float accuracy = location.getAccuracy();

        mTelemetry.write(
                Telemetry.GPS,
                String.format(
                        "ts:%d;lat:%f;long:%f;accuracy:%f;speed:%f",
                        location.getTime(),
                        location.getLatitude(),
                        location.getLongitude(),
                        accuracy,
                        location.getSpeed()));

        Log.d(TAG, "New position: " + location.getLatitude() + ", " + location.getLongitude());
        if (accuracy <= mConfig.mGpsAccuracy) {
            Log.d(TAG, "Position stored (accuracy : " + accuracy + ")");

            mTelemetry.write(Telemetry.GPS, "ValidPoint");

            if (mListener != null)
                mListener.onNewLocation(location);

            stop();
        } else {
            Log.d(TAG, "Better accuracy required (accuracy : " + accuracy + ")");
            mLastLocation = location;
        }
    }

    private void onLocationTimeout() {
        float accuracy = mLastLocation != null ? mLastLocation.getAccuracy() : Float.NaN;

        Log.i(TAG, "Location acquisition timeout (better accuracy : " + accuracy + ")");

        mTelemetry.write(Telemetry.GPS, "Timeout");

        if (mListener != null)
            mListener.onTimeout();

        stop();
    }
}
