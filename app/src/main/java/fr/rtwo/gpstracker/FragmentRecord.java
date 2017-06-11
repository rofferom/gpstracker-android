package fr.rtwo.gpstracker;

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
            // Update position count
            AcquisitionService.Stats stats = mAcqService.getStats();

            Log.i(TAG, stats.mRecordedLocations + " locations");
            updateLocationCount(stats);
            updateLastLocation(location);
        }

        @Override
        public void onTimeout() {
            // Update position count
            AcquisitionService.Stats stats = mAcqService.getStats();

            updateLocationCount(stats);
        }
    }

    private MainActivity mActivity;
    private View mView;
    private Resources mResources;

    // Layout
    private TextView mTvState;
    private TextView mTvPositionCount;
    private TextView mTvPositionStats;
    private TextView mTvLastPosition;

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

            // Update UI
            mTvState.setText(R.string.textViewStateStarted);

            AcquisitionService.Stats stats = mAcqService.getStats();
            updateLocationCount(stats);

            Location lastLocation = stats.mLastLocation;
            if (lastLocation != null)
                updateLastLocation(lastLocation);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mAcqService.unregisterListener(mAcqServiceListerner);
            mAcqService = null;
            mAcqServiceBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_record, container, false);
        mActivity = (MainActivity) getActivity();
        mResources = getResources();

        Button startButton = (Button) mView.findViewById(R.id.startRecording);
        startButton.setOnClickListener(mStartGpsRecording);

        Button stopButton = (Button) mView.findViewById(R.id.stopRecording);
        stopButton.setOnClickListener(mStopGpsRecording);

        Button manualAcqButton = (Button) mView.findViewById(R.id.manualAcq);
        manualAcqButton.setOnClickListener(mManualGpsAcq);

        mTvState = (TextView) mView.findViewById(R.id.state);
        mTvPositionCount = (TextView) mView.findViewById(R.id.positionsCount);
        mTvPositionStats = (TextView) mView.findViewById(R.id.positionsStats);
        mTvLastPosition = (TextView) mView.findViewById(R.id.lastPosition);

        if (AcquisitionService.isStarted())
            bindToAcqService();

        return mView;
    }

    @Override
    public void onDestroyView() {
        if (mAcqServiceBound) {
            mAcqService.unregisterListener(mAcqServiceListerner);
            mActivity.unbindService(mAcqServiceConnection);
        }

        super.onDestroyView();
    }

    private void bindToAcqService() {
        Intent bindItent = new Intent(mActivity, AcquisitionService.class);
        mActivity.bindService(bindItent, mAcqServiceConnection, 0);
    }

    private View.OnClickListener mStartGpsRecording = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mActivity.getHasPermissions())
                return;

            if (mAcqServiceBound)
                return;

            Log.i(TAG, "Start GPS recording");
            // Start acquisition service
            Intent startIntent = new Intent(mActivity, AcquisitionService.class);
            mActivity.startService(startIntent);
            // Bind to acquisition service
            bindToAcqService();
        }
    };

    private View.OnClickListener mStopGpsRecording = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mActivity.getHasPermissions())
                return;

            if (!mAcqServiceBound)
                return;

            Log.i(TAG, "Stop GPS recording");

            // Stop GPS acquisition
            mActivity.unbindService(mAcqServiceConnection);
            mAcqServiceBound = false;

            Intent intent = new Intent(mActivity, AcquisitionService.class);
            mActivity.stopService(intent);

            // Update UI
            mTvState.setText(R.string.textViewStateStopped);

            Log.i(TAG, "0 locations");
            mTvPositionCount.setText(R.string.textViewPositionNone);
            mTvPositionStats.setText(R.string.textViewStatsNone);
        }
    };

    private View.OnClickListener mManualGpsAcq = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mActivity.getHasPermissions())
                return;

            if (!mAcqServiceBound)
                return;

            // Stop GPS acquisition
            mAcqService.manualAcq();
        }
    };

    private void updateLocationCount(AcquisitionService.Stats stats) {
        mTvPositionCount.setText(mResources.getString(R.string.textViewPosition,
                stats.mRecordedLocations, stats.mRecordedTimeouts));

        mTvPositionStats.setText(mResources.getString(R.string.textViewStats,
                stats.getLastSuccessCount(), stats.getLastTimeoutCount()));
    }

    private void updateLastLocation(Location location) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        mTvLastPosition.setText(mResources.getString(
                R.string.textViewLastPosition,
                dateFormater.format(new Date(location.getTime()))));
    }
}
