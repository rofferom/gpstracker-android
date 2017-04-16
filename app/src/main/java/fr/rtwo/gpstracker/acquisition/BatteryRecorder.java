package fr.rtwo.gpstracker.acquisition;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

import fr.rtwo.gpstracker.Config;
import fr.rtwo.gpstracker.logs.Telemetry;

public class BatteryRecorder {
    private static final String TAG = "BatteryRecorder";

    private static final String ALARM_TIMER_ACTION = "fr.rtwo.gpstracker.batterytimeraction";

    private Context mContext;
    private AlarmManager mAlarmManager;
    private Config mConfig = Config.getInstance();
    private Telemetry mTelemetry;
    private TimerReceiver mTimerReceiver = new TimerReceiver();

    private PendingIntent mPendingIntent;

    public BatteryRecorder(Context context, Telemetry telemetry) {
        mContext = context;
        mTelemetry = telemetry;
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    private void recordBatteryLevel() {
        Log.i(TAG, "Record battery level");

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = (level / (float) scale) * 100;
        mTelemetry.write(Telemetry.BATTERY, String.valueOf(batteryPct));
    }

    public boolean start() {
        recordBatteryLevel();

        // Set alarm handler
        IntentFilter filter = new IntentFilter();
        filter.addAction(ALARM_TIMER_ACTION);
        mContext.registerReceiver(mTimerReceiver, filter);

        // Set alarm
        Intent intent = new Intent(ALARM_TIMER_ACTION);

        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + mConfig.mBatteryAcqPeriod * 1000,
                mConfig.mBatteryAcqPeriod * 1000,
                mPendingIntent);

        return true;
    }

    public boolean stop() {
        mAlarmManager.cancel(mPendingIntent);
        mPendingIntent = null;

        mContext.unregisterReceiver(mTimerReceiver);

        return true;
    }

    private class TimerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            recordBatteryLevel();
        }
    }
}
