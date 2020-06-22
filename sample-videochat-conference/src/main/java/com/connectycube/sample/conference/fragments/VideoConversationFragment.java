package com.connectycube.sample.conference.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.adapters.OpponentsFromCallAdapter;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;
import com.connectycube.videochat.conference.ConferenceSession;
import com.connectycube.videochat.conference.view.ConferenceSurfaceView;
import com.connectycube.videochat.view.RTCVideoTrack;

import org.webrtc.CameraVideoCapturer;

import java.io.Serializable;


public class VideoConversationFragment extends BaseConversationFragment implements Serializable,
        RTCSessionStateCallback<ConferenceSession>, OpponentsFromCallAdapter.OnAdapterEventListener {

    private String TAG = VideoConversationFragment.class.getSimpleName();

    private ToggleButton cameraToggle;
    private CameraState cameraState = CameraState.DISABLED_FROM_USER;

    private RTCVideoTrack localVideoTrack;
    protected boolean isCurrentCameraFront;


    @Override
    protected void initViews(View view) {
        isCurrentCameraFront = true;
        cameraToggle = view.findViewById(R.id.toggle_camera);
        cameraToggle.setVisibility(View.VISIBLE);
        super.initViews(view);
    }

    @Override
    protected void actionButtonsEnabled(boolean inability) {
        super.actionButtonsEnabled(inability);
        cameraToggle.setEnabled(inability);
        // inactivate toggle buttons
        cameraToggle.setActivated(inability);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // If user changed camera state few times and last state was CameraState.ENABLED_FROM_USER
        // than we turn on cam, else we nothing change
        if (cameraState != VideoConversationFragment.CameraState.DISABLED_FROM_USER) {
            cameraToggle.setChecked(true);
            toggleCamera(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onPause() {
        // If camera state is CameraState.ENABLED_FROM_USER or CameraState.NONE
        // than we turn off cam
        if (cameraState != CameraState.DISABLED_FROM_USER) {
            toggleCamera(false);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }


    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    @Override
    protected void setActionButtonsInvisible() {
        super.setActionButtonsInvisible();
        cameraToggle.setVisibility(View.INVISIBLE);
    }


    protected void initButtonsListener() {
        super.initButtonsListener();

        cameraToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (cameraState != CameraState.DISABLED_FROM_USER) {
                toggleCamera(isChecked);
            }
        });
    }

    private void switchCamera(final MenuItem item) {
        if (cameraState == CameraState.DISABLED_FROM_USER) {
            return;
        }
        cameraToggle.setEnabled(false);
        conversationFragmentCallbackListener.onSwitchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean b) {
                Log.d(TAG, "camera switched, bool = " + b);
                isCurrentCameraFront = b;
                updateSwitchCameraIcon(item);
                toggleCameraInternal();
            }

            @Override
            public void onCameraSwitchError(String s) {
                Log.d(TAG, "camera switch error " + s);
                Toast.makeText(getContext(), getString(R.string.camera_swicth_failed) + s, Toast.LENGTH_SHORT).show();
                cameraToggle.setEnabled(true);
            }
        });
    }

    private void updateSwitchCameraIcon(final MenuItem item) {
        if (isCurrentCameraFront) {
            Log.d(TAG, "CameraFront now!");
            item.setIcon(R.drawable.ic_camera_front);
        } else {
            Log.d(TAG, "CameraRear now!");
            item.setIcon(R.drawable.ic_camera_rear);
        }
    }

    private void toggleCameraInternal() {
        Log.d(TAG, "Camera was switched!");
        updateVideoView(localVideoView, isCurrentCameraFront);
        toggleCamera(true);
    }

    private void toggleCamera(boolean isNeedEnableCam) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            conversationFragmentCallbackListener.onSetVideoEnabled(isNeedEnableCam);
        }
        if (!cameraToggle.isEnabled()) {
            cameraToggle.setEnabled(true);
        }
    }

    @Override
    protected void fillVideoView(ConferenceSurfaceView videoView, RTCVideoTrack videoTrack,
                                 boolean remoteRenderer) {
        super.fillVideoView(videoView, videoTrack, remoteRenderer);
        if (!remoteRenderer) {
            updateVideoView(videoView, isCurrentCameraFront);
        }
    }

    ////////////////////////////  callbacks from RTCClientVideoTracksCallback ///////////////////
    @Override
    public void onLocalVideoTrackReceive(ConferenceSession rtcSession, final RTCVideoTrack videoTrack) {
        Log.d(TAG, "onLocalVideoTrackReceive() run");
        localVideoTrack = videoTrack;
        cameraState = CameraState.NONE;
        actionButtonsEnabled(true);

        if (localVideoView != null) {
            fillVideoView(localVideoView, localVideoTrack, false);
        }
    }

    /////////////////////////////////////////    end    ////////////////////////////////////////////

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera_switch:
                Log.d(TAG, "camera_switch");
                switchCamera(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private enum CameraState {
        NONE,
        DISABLED_FROM_USER,
        ENABLED_FROM_USER
    }
}