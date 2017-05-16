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

    public void open(File parent) {
        if (mFile != null)
            return;

        mFile = new File(parent, "locations.txt");

        try {
            mWriter = new PrintWriter(mFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mFile = null;
            return;
        }
    }

    public void close() {
        if (mFile == null)
            return;

        Log.i(TAG, "Close file: " + mFile.getName());

        mWriter.close();
        mWriter = null;
        mFile = null;
    }

    public void recordLocation(Location location) {
        if (mWriter == null) {
            Log.w(TAG, "null writer");
            return;
        }

        mWriter.printf("ts:%d;lat:%f;long:%f;accuracy:%f;speed:%f\n",
                location.getTime(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed());

        mWriter.flush();
    }
}
