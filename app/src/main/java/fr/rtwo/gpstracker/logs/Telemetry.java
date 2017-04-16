package fr.rtwo.gpstracker.logs;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

public class Telemetry {
    static public final String APP_TAG = "APP";

    // GPS values
    static public final String GPS_TAG = "GPS";

    static public final String GPS_START_ACQ       = "StartAcq";
    static public final String GPS_STOP_ACQ        = "StopAcq";
    static public final String GPS_TIMEOUT         = "Timeout";
    static public final String GPS_VALID_POINT     = "ValidPoint";
    static public final String GPS_LOCATION_FORMAT = "ts:%d;lat:%f;long:%f;accuracy:%f;speed:%f";

    // Battery tags
    static public final String BATTERY_TAG = "Battery";

    private File mFile;
    private PrintWriter mWriter;

    public Telemetry() {
    }

    public boolean open(File parent) {
        if (mFile != null)
            return false;

        mFile = new File(parent, "telemetry.txt");

        try {
            mWriter = new PrintWriter(mFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mFile = null;
            return false;
        }

        return true;
    }

    public boolean close() {
        if (mFile == null)
            return false;

        mWriter.close();
        mWriter = null;
        mFile = null;

        return true;
    }

    public boolean write(String channel, String msg) {
        Date date = new Date();

        if (mFile == null)
            return false;

        mWriter.printf("[%d]%s:%s\n", date.getTime(), channel, msg);
        return true;
    }
}
