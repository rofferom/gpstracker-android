package fr.rtwo.gpstracker;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.location.Location;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import fr.rtwo.gpstracker.acquisition.AcquisitionService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private class AcqListener implements AcquisitionService.Listener {
        @Override
        public void onNewLocation(Location location) {
            Resources resources = getResources();

            // Update position count
            TextView tvPosCount = (TextView) findViewById(R.id.positionsCount);
            int count = mAcqService.getLocationCount();

            Log.i(TAG, count + " locations");
            tvPosCount.setText(resources.getString(R.string.textViewPosition, count));

            // Update last position
            SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            TextView tvLastPos = (TextView) findViewById(R.id.lastPosition);
            tvLastPos.setText(resources.getString(
                    R.string.textViewLastPosition,
                    dateFormater.format(new Date(location.getTime()))));
        }
    }

    // AcquisitionService variables
    private boolean mAcqServiceBound = false;
    private AcquisitionService mAcqService;
    private AcqListener mAcqServiceListerner = new AcqListener();

    private ServiceConnection mAcqServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            AcquisitionService.LocalBinder binder = (AcquisitionService.LocalBinder) service;
            mAcqService = binder.getService();
            mAcqService.registerListener(mAcqServiceListerner);
            mAcqServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mAcqService = null;
            mAcqServiceBound = false;
        }
    };

    public void startGpsRecording(View v) {
        Log.i(TAG, "Start GPS recording");

        // Start acquisition service
        Intent startIntent = new Intent(this, AcquisitionService.class);
        startService(startIntent);

        // Bind to acquisition service
        Intent bindItent = new Intent(this, AcquisitionService.class);
        bindService(bindItent, mAcqServiceConnection, 0);

        TextView tv = (TextView) findViewById(R.id.state);
        tv.setText(R.string.textViewStateStarted);
    }

    public void stopGpsRecording(View v) {
        Log.i(TAG, "Stop GPS recording");

        // Stop GPS acquisition
        unbindService(mAcqServiceConnection);

        Intent intent = new Intent(this, AcquisitionService.class);
        stopService(intent);

        TextView tvState = (TextView) findViewById(R.id.state);
        tvState.setText(R.string.textViewStateStopped);

        Log.i(TAG, "0 locations");
        TextView tvCount = (TextView) findViewById(R.id.positionsCount);
        tvCount.setText(R.string.textViewPositionNone);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean ret;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
