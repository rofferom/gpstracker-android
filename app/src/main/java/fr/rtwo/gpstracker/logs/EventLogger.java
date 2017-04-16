package fr.rtwo.gpstracker.logs;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;

public class EventLogger {
    private static final String TAG = "EventLogger";

    private File mFile;
    private PrintWriter mWriter;

    public EventLogger() {
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    public void open() {
        Calendar c = Calendar.getInstance();

        if (!isExternalStorageWritable()) {
            Log.e(TAG, "External storage not writable");
            return;
        }

        String filename = String.format(
            "GpsTracker-%04d%02d%02d-%02d%02d%02d.bin",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            c.get(Calendar.SECOND));

        Log.i(TAG, "Open file: " + filename);

        File parent = Environment.getExternalStorageDirectory();
        mFile = new File(parent, filename);

        try {
            mWriter = new PrintWriter(mFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
    }

    public void close() {
        Log.i(TAG, "Close file: " + mFile.getName());

        mWriter.close();
        mWriter = null;
        mFile = null;
    }

    public void recordLocation(Location location) {
        mWriter.printf("ts:%d;lat:%f;long:%f;accuracy:%f;speed:%f\n",
                location.getTime(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed());
    }
}
