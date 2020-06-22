package com.connectycube.sample.conference.fragments;

import com.connectycube.sample.conference.activities.CallActivity;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;
import com.connectycube.videochat.conference.ConferenceSession;

import org.webrtc.CameraVideoCapturer;


public interface ConversationFragmentCallbackListener {

    void addClientConnectionCallback(RTCSessionStateCallback<ConferenceSession> clientConnectionCallbacks);
    void removeClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks);

    void addCurrentCallStateCallback (CallActivity.CurrentCallStateCallback currentCallStateCallback);
    void removeCurrentCallStateCallback (CallActivity.CurrentCallStateCallback currentCallStateCallback);

    void onSetAudioEnabled(boolean isAudioEnabled);

    void onSetVideoEnabled(boolean isNeedEnableCam);

    void onSwitchAudio();

    void onLeaveCurrentSession();

    void onSwitchCamera(CameraVideoCapturer.CameraSwitchHandler cameraSwitchHandler);

    void onStartJoinConference();
}
