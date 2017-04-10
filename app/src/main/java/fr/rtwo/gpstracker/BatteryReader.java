package fr.rtwo.gpstracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryReader {
    private Context mContext;
    private Config mConfig = Config.getInstance();
    private Timer mTimer = new Timer();

    public BatteryReader(Context context) {
        mContext = context;
    }

    private void recordBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = (level / (float) scale) * 100;
    }

    public boolean start() {
        recordBatteryLevel();

        mTimer.setPeriodic(mTimer.new Listener() {
            @Override
            void onExpired() {
                recordBatteryLevel();
            }
        },
        mConfig.mBatteryAcqPeriod * 1000);

        return true;
    }

    public boolean stop() {
        mTimer.clear();

        return true;
    }
}
