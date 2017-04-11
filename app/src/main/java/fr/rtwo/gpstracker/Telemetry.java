package fr.rtwo.gpstracker;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

public class Telemetry {
    static public String APP = "APP";
    static public String GPS = "GPS";
    static public String BATTERY = "Battery";

    private static Telemetry ourInstance = new Telemetry();

    private File mFile;
    private PrintWriter mWriter;

    public static Telemetry getInstance() {
        return ourInstance;
    }

    private Telemetry() {
    }

    public boolean open() {
        if (mFile != null)
            return false;

        Calendar c = Calendar.getInstance();

        String folderName = String.format(
                "%04d%02d%02d-%02d%02d%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND));


        File rootFolder = Environment.getExternalStorageDirectory();
        File parent = new File(rootFolder, "GpsTracker/" + folderName);
        parent.mkdirs();

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
