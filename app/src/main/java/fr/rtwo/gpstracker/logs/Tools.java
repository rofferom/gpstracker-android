package fr.rtwo.gpstracker.logs;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Calendar;

public class Tools {
    private static final String TAG = "LogsTools";

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }


    public static File createOutputFolder() {
        boolean ret;

        if (!isExternalStorageWritable()) {
            Log.e(TAG, "External storage not writable");
            return null;
        }

        Calendar c = Calendar.getInstance();

        String folderName = String.format(
                "%04d%02d%02d-%02d%02d%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND));

        File rootFolder = Environment.getExternalStorageDirectory();

        File folder = new File(rootFolder, "GpsTracker/" + folderName);

        ret = folder.mkdirs();
        if (!ret)
            Log.e(TAG, "Failed to create folder " + folder.getAbsolutePath());

        return folder;
    }
}
