package com.connectycube.sample.conference.fragments;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.adapters.OpponentsFromCallAdapter;
import com.connectycube.videochat.RTCAudioTrack;
import com.connectycube.videochat.callbacks.RTCClientAudioTracksCallback;
import com.connectycube.videochat.conference.ConferenceSession;

import java.io.Serializable;


public class AudioConversationFragment extends BaseConversationFragment implements Serializable, OpponentsFromCallAdapter.OnAdapterEventListener,
        RTCClientAudioTracksCallback<ConferenceSession> {
    private String TAG = getClass().getSimpleName();

    private ToggleButton audioSwitchToggleButton;

    @Override
    protected void initViews(View view) {
        TextView localName = view.findViewById(R.id.localName);
        localName.setVisibility(View.VISIBLE);
        audioSwitchToggleButton = view.findViewById(R.id.toggle_speaker);
        audioSwitchToggleButton.setVisibility(View.VISIBLE);
        super.initViews(view);
    }

    @Override
    public void onLocalAudioTrackReceive(ConferenceSession session, RTCAudioTrack audioTrack) {
        Log.d(TAG, "onLocalAudioTrackReceive() run");
        setStatusForCurrentUser(getString(R.string.text_status_connected));
        actionButtonsEnabled(true);
    }

    @Override
    public void onRemoteAudioTrackReceive(ConferenceSession session, RTCAudioTrack audioTrack, final Integer userID) {
        Log.d(TAG, "onRemoteAudioTrackReceive() run");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem cameraSwitchItem = menu.findItem(R.id.camera_switch);
        cameraSwitchItem.setVisible(false);
    }

    @Override
    protected void initButtonsListener() {
        super.initButtonsListener();

        audioSwitchToggleButton.setOnClickListener(v -> conversationFragmentCallbackListener.onSwitchAudio());
    }

    @Override
    protected void actionButtonsEnabled(boolean inability) {
        super.actionButtonsEnabled(inability);
        audioSwitchToggleButton.setActivated(inability);
    }

    @Override
    protected void initTrackListeners() {
        super.initTrackListeners();
        initAudioTracksListener();
    }

    @Override
    protected void removeTrackListeners() {
        removeAudioTracksListener();
    }

    private void initAudioTracksListener() {
        if (currentSession != null) {
            currentSession.addAudioTrackCallbacksListener(this);
        }
    }

    private void removeAudioTracksListener() {
        if (currentSession != null) {
            currentSession.removeAudioTrackCallbacksListener(this);
        }
    }
}