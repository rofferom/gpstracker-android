package fr.rtwo.gpstracker.acquisition;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import fr.rtwo.gpstracker.Config;
import fr.rtwo.gpstracker.R;
import fr.rtwo.gpstracker.logs.EventLogger;
import fr.rtwo.gpstracker.logs.Telemetry;
import fr.rtwo.gpstracker.logs.Tools;

public class AcquisitionService extends Service {
    private static final String TAG = "AcqService";

    private static final String GPS_TIMER_ACTION = "fr.rtwo.gpstracker.gpstimeraction";

    public interface Listener {
        void onNewLocation(Location location);
    }

    // Lifecycle
    private static boolean sStarted  = false;

    public static boolean isStarted() {
        return sStarted;
    }

    // Service API
    private final IBinder mBinder = new LocalBinder();
    private List<Listener> mListeners = new LinkedList<Listener>();

    // Globals
    private Telemetry mTelemetry;
    private EventLogger mEventLogger;
    private AlarmManager mAlarmManager;

    // Notifications variables
    private static final int SERVICE_NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;

    // Battery varaibles
    private BatteryRecorder mBatteryRecorder;

    // Gps variables
    private long mGpsAcqPeriod;
    private GpsRecorder mGpsRecorder;
    private GpsListener mGpsRecorderListener;
    private GpsTimerReceiver mGpsTimerReceiver = new GpsTimerReceiver();
    private PendingIntent mGpsPendingIntent;

    private int mRecordedLocations = 0;
    private Location mLastLocation = null;

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

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        mNotificationBuilder = new Notification.Builder(this);

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

        Log.i(TAG, "AcquisitionService destroyed");

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

        // Service is not foreground anymore
        unsetForeground();

        sStarted = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Config config = Config.getInstance();

        Log.i(TAG, "AcquisitionService started");
        mGpsAcqPeriod = config.getGpsAcqPeriod();

        // Write config in telemetry file.
        // Required to understand generated stats
        mTelemetry.write(Telemetry.APP_TAG,
                String.format(
                    Telemetry.APP_CONFIG,
                    config.getGpsAccuracy(),
                    mGpsAcqPeriod,
                    config.getGpsAcqTimeout())
                );

        // Start subcomponents
        startGpsAcq();
        mBatteryRecorder.start();

        // Make service foreground
        setForeground();

        sStarted = true;

        return START_STICKY;
    }

    // Foreground management
    private void setForeground() {
        mNotificationBuilder.setContentTitle(getString(R.string.notifAcqServiceTitle));
        mNotificationBuilder.setContentText(getString(R.string.notifAcqServiceMessage, 0));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_notif_acq_service);

        startForeground(SERVICE_NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void unsetForeground() {
        stopForeground(true);
    }

    private void updateForeground() {
        String msg = getString(R.string.notifAcqServiceMessage, mRecordedLocations);
        mNotificationBuilder.setContentText(msg);

        mNotificationManager.notify(SERVICE_NOTIFICATION_ID , mNotificationBuilder.build());
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

    public Location getLastLocation() {
        return mLastLocation;
    }

    public void manualAcq() {
        mTelemetry.write(Telemetry.GPS_TAG, Telemetry.GPS_MANUAL_ACQ);
        clearGpsAcqTimer();
        mGpsRecorder.manualAcq();
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
                SystemClock.elapsedRealtime() + mGpsAcqPeriod * 1000,
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
        public void onNewLocation(Location location, boolean isManualAcq) {
            if (isManualAcq)
                Log.i(TAG, "GPS: New manual location");
            else
                Log.i(TAG, "GPS: New location");

            mRecordedLocations++;
            mLastLocation = location;
            updateForeground();

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
