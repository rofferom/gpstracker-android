package fr.rtwo.gpstracker.acquisition;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import fr.rtwo.gpstracker.Config;
import fr.rtwo.gpstracker.logs.EventLogger;
import fr.rtwo.gpstracker.logs.Telemetry;
import fr.rtwo.gpstracker.logs.Tools;

public class AcquisitionService extends Service {
    private static final String TAG = "AcqService";

    private static final String GPS_TIMER_ACTION = "fr.rtwo.gpstracker.gpstimeraction";

    public interface Listener {
        void onNewLocation(Location location);
    }

    // Service API
    private final IBinder mBinder = new LocalBinder();
    private List<Listener> mListeners = new LinkedList<Listener>();

    // Globals
    private Config mConfig = Config.getInstance();
    private Telemetry mTelemetry;
    private EventLogger mEventLogger;
    private AlarmManager mAlarmManager;

    // Battery varaibles
    private BatteryRecorder mBatteryRecorder;

    // Gps variables
    private GpsRecorder mGpsRecorder;
    private GpsListener mGpsRecorderListener;
    private GpsTimerReceiver mGpsTimerReceiver = new GpsTimerReceiver();
    private PendingIntent mGpsPendingIntent;
    private int mRecordedLocations = 0;

    public class LocalBinder extends Binder {
        public AcquisitionService getService() {
            return AcquisitionService.this;
        }
    }

    public AcquisitionService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Open record file
        File folder = Tools.createOutputFolder();

        mEventLogger = new EventLogger();
        mEventLogger.open(folder);

        // Open telemetry file
        mTelemetry = new Telemetry();
        mTelemetry.open(folder);

        // Create GPS recorder
        mGpsRecorderListener = new GpsListener();
        mGpsRecorder = new GpsRecorder(this, mGpsRecorderListener, mTelemetry);

        // Register to GPS timer
        IntentFilter filter = new IntentFilter();
        filter.addAction(GPS_TIMER_ACTION);
        registerReceiver(mGpsTimerReceiver, filter);

        // Create Battery recorder
        mBatteryRecorder = new BatteryRecorder(this, mTelemetry);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop GPS
        unregisterReceiver(mGpsTimerReceiver);

        stopGpsAcq();

        // Stop battery
        mBatteryRecorder.stop();

        // Close files
        mTelemetry.close();
        mTelemetry = null;

        mEventLogger.close();
        mEventLogger = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        // Write config in telemetry file.
        // Required to understand generated stats
        mTelemetry.write(Telemetry.APP_TAG,
                String.format(
                    Telemetry.APP_CONFIG,
                    mConfig.mGpsAccuracy,
                    mConfig.mGpsAcqPeriod,
                    mConfig.mGpsAcqTimeout)
                );

        // Start subcomponents
        startGpsAcq();
        mBatteryRecorder.start();

        return START_STICKY;
    }

    // Listerner management
    public boolean registerListener(Listener listener) {
        mListeners.add(listener);
        return true;
    }

    public boolean unregisterListener(Listener listener) {
        return mListeners.remove(listener);
    }

    // GPS management
    public int getLocationCount() {
        return mRecordedLocations;
    }

    private void startGpsAcq() {
        mGpsRecorder.start();
    }

    private void stopGpsAcq() {
        clearGpsAcqTimer();
        mGpsRecorder.stop();
    }

    private void setGpsAcqTimer() {
        Intent intent = new Intent(GPS_TIMER_ACTION);

        mGpsPendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + mConfig.mGpsAcqPeriod * 1000,
                mGpsPendingIntent);
    }

    private void clearGpsAcqTimer() {
        // The intent can be null because it is set after the first location received,
        // to prepare the next acquisition.
        if (mGpsPendingIntent != null) {
            mAlarmManager.cancel(mGpsPendingIntent);
            mGpsPendingIntent = null;
        }
    }

    private class GpsListener implements GpsRecorder.Listener {
        @Override
        public void onNewLocation(Location location) {
            Log.i(TAG, "GPS: New location");

            mRecordedLocations++;
            for (Listener e : mListeners) {
                e.onNewLocation(location);
            }

            mEventLogger.recordLocation(location);
            setGpsAcqTimer();
        }

        @Override
        public void onTimeout() {
            Log.i(TAG, "GPS: Timeout");
            setGpsAcqTimer();
        }
    }

    private class GpsTimerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Start new GPS acquisition");
            startGpsAcq();
        }
    }
}
