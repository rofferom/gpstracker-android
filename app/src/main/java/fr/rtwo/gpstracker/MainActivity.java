package fr.rtwo.gpstracker;

import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BatteryReader mBatteryReader;

    private LocationDatabase mLocationDb;
    private LocationDatabase.Listener mLocationListener;

    public void startGpsRecording(View v) {
        Log.i(TAG, "Start GPS recording");

        // Start GPS acquisition
        Intent intent = new Intent(this, GpsRecorder.class);
        startService(intent);

        TextView tv = (TextView) findViewById(R.id.state);
        tv.setText(R.string.textViewStateStarted);

        // Start battery acquisition
        mBatteryReader.start();
    }

    public void stopGpsRecording(View v) {
        Log.i(TAG, "Stop GPS recording");

        // Stop GPS acquisition
        Intent intent = new Intent(this, GpsRecorder.class);
        stopService(intent);

        TextView tv = (TextView) findViewById(R.id.state);
        tv.setText(R.string.textViewStateStopped);

        // Stop battery acquisition
        mBatteryReader.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean ret;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create battery reader
        mBatteryReader = new BatteryReader(this);

        // Get reference to location database
        mLocationDb = LocationDatabase.getInstance();

        mLocationListener = mLocationDb.new Listener() {
            @Override
            void onNewLocation(Location location) {
                Resources resources = getResources();

                // Update position count
                TextView tvPosCount = (TextView) findViewById(R.id.positionsCount);
                int count = mLocationDb.getCount();

                Log.i(TAG, count + " locations");
                tvPosCount.setText(resources.getString(R.string.textViewPosition, count));

                // Update last position
                SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd HH:mm");

                TextView tvLastPos = (TextView) findViewById(R.id.lastPosition);
                tvLastPos.setText(resources.getString(
                        R.string.textViewLastPosition,
                        dateFormater.format(new Date(location.getTime()))));
            }

            @Override
            void onLocationsCleared() {
                Log.i(TAG, "0 locations");

                TextView tv = (TextView) findViewById(R.id.positionsCount);
                tv.setText(R.string.textViewPositionNone);
            }
        };

        ret = mLocationDb.registerListener(mLocationListener);
        if (ret == false) {
            Log.e(TAG, "Fail to register LocationListener");
        }
    }
}
