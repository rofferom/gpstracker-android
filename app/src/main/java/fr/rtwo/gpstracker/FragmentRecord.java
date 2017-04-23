package fr.rtwo.gpstracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import fr.rtwo.gpstracker.acquisition.AcquisitionService;

public class FragmentRecord extends Fragment {
    private static final String TAG = "FragmentRecord";

    private class AcqListener implements AcquisitionService.Listener {
        @Override
        public void onNewLocation(Location location) {
            Resources resources = getResources();

            // Update position count
            TextView tvPosCount = (TextView) getView().findViewById(R.id.positionsCount);
            int count = mAcqService.getLocationCount();

            Log.i(TAG, count + " locations");
            tvPosCount.setText(resources.getString(R.string.textViewPosition, count));

            // Update last position
            SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            TextView tvLastPos = (TextView) getView().findViewById(R.id.lastPosition);
            tvLastPos.setText(resources.getString(
                    R.string.textViewLastPosition,
                    dateFormater.format(new Date(location.getTime()))));
        }
    }

    private Activity mActivity;
    private View mView;

    // AcquisitionService variables
    private boolean mAcqServiceBound = false;
    private AcquisitionService mAcqService;
    private AcqListener mAcqServiceListerner = new AcqListener();

    private ServiceConnection mAcqServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,  IBinder service) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_record, container, false);
        mActivity = getActivity();

        Button startButton = (Button) mView.findViewById(R.id.startRecording);
        startButton.setOnClickListener(mStartGpsRecording);

        Button stopButton = (Button) mView.findViewById(R.id.stopRecording);
        stopButton.setOnClickListener(mStopGpsRecording);

        return mView;
    }

    private View.OnClickListener mStartGpsRecording = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mAcqServiceBound)
                return;
            Log.i(TAG, "Start GPS recording");
            // Start acquisition service
            Intent startIntent = new Intent(mActivity, AcquisitionService.class);
            mActivity.startService(startIntent);
            // Bind to acquisition service
            Intent bindItent = new Intent(mActivity, AcquisitionService.class);
            mActivity.bindService(bindItent, mAcqServiceConnection, 0);
            TextView tv = (TextView) mView.findViewById(R.id.state);
            tv.setText(R.string.textViewStateStarted);
        }
    };

    private View.OnClickListener mStopGpsRecording = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mAcqServiceBound)
                return;

            Log.i(TAG, "Stop GPS recording");

            // Stop GPS acquisition
            mActivity.unbindService(mAcqServiceConnection);
            mAcqServiceBound = false;

            Intent intent = new Intent(mActivity, AcquisitionService.class);
            mActivity.stopService(intent);

            TextView tvState = (TextView) mView.findViewById(R.id.state);
            tvState.setText(R.string.textViewStateStopped);

            Log.i(TAG, "0 locations");
            TextView tvCount = (TextView) mView.findViewById(R.id.positionsCount);
            tvCount.setText(R.string.textViewPositionNone);
        }
    };
}
