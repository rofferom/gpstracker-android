package fr.rtwo.gpstracker;

import android.os.Handler;

public class Timer {
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

    public boolean clear() {
        if (mRunnable == null)
            return false;

        mHandler.removeCallbacks(mRunnable);
        mRunnable = null;

        return true;
    }
}
