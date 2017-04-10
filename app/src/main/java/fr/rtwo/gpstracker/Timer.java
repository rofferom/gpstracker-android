package fr.rtwo.gpstracker;

import android.os.Handler;
import android.util.Log;

public class Timer {
    private static final String TAG = "Timer";

    public abstract class Listener {
        abstract void onExpired();
    }

    private Handler mHandler;
    private Runnable mRunnable = null;

    public Timer() {
        mHandler = new Handler();
    }

    public boolean set(final Listener cb, long delay) {
        boolean ret;

        if (mRunnable != null)
            return false;

        mRunnable = new Runnable() {
            @Override
            public void run() {
                cb.onExpired();
            }
        };

        ret = mHandler.postDelayed(mRunnable, delay);
        if (!ret) {
            mRunnable = null;
            return false;
        }

        return true;
    }

    public boolean setPeriodic(final Listener cb, final long period) {
        boolean ret;

        if (mRunnable != null)
            return false;

        mRunnable = new Runnable() {
            @Override
            public void run() {
                boolean ret;

                cb.onExpired();
                ret = mHandler.postDelayed(mRunnable, period);
                if (!ret) {
                    Log.e(TAG, "postDelayed() failed");
                }
            }
        };

        ret = mHandler.postDelayed(mRunnable, period);
        if (!ret) {
            mRunnable = null;
            return false;
        }

        return true;
    }

    public boolean clear() {
        if (mRunnable == null)
            return false;

        mHandler.removeCallbacks(mRunnable);
        mRunnable = null;

        return true;
    }
}
