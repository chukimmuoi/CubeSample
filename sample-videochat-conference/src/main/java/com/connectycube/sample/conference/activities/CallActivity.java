package com.connectycube.sample.conference.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.fragments.AudioConversationFragment;
import com.connectycube.sample.conference.fragments.BaseConversationFragment;
import com.connectycube.sample.conference.fragments.ConversationFragmentCallbackListener;
import com.connectycube.sample.conference.fragments.VideoConversationFragment;
import com.connectycube.sample.conference.utils.Consts;
import com.connectycube.sample.conference.utils.NetworkConnectionChecker;
import com.connectycube.sample.conference.utils.SettingsUtil;
import com.connectycube.sample.conference.utils.WebRtcSessionManager;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.AppRTCAudioManager;
import com.connectycube.videochat.BaseSession;
import com.connectycube.videochat.RTCCameraVideoCapturer;
import com.connectycube.videochat.RTCConfig;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;
import com.connectycube.videochat.conference.ConferenceClient;
import com.connectycube.videochat.conference.ConferenceRole;
import com.connectycube.videochat.conference.ConferenceSession;
import com.connectycube.videochat.conference.WsException;
import com.connectycube.videochat.conference.WsHangUpException;
import com.connectycube.videochat.conference.WsNoResponseException;
import com.connectycube.videochat.conference.callbacks.ConferenceEntityCallback;
import com.connectycube.videochat.conference.callbacks.ConferenceSessionCallbacks;

import org.webrtc.CameraVideoCapturer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.connectycube.sample.conference.utils.Consts.EXTRA_OPPONENTS;


public class CallActivity extends BaseActivity implements RTCSessionStateCallback<ConferenceSession>, ConferenceSessionCallbacks<ConferenceSession>,
        ConversationFragmentCallbackListener, NetworkConnectionChecker.OnConnectivityChangedListener {

    private static final String TAG = CallActivity.class.getSimpleName();
    private static final String ICE_FAILED_REASON = "ICE failed";

    private ConferenceSession currentSession;
    private ConferenceClient rtcClient;
    private SharedPreferences sharedPref;
    private AppRTCAudioManager audioManager;
    private NetworkConnectionChecker networkConnectionChecker;
    private WebRtcSessionManager sessionManager;
    private boolean isVideoCall;
    private ArrayList<CurrentCallStateCallback> currentCallStateCallbackList = new ArrayList<>();
    private ArrayList<Integer> opponentsIdsList;
    private ArrayList<ConnectycubeUser> allOpponents;
    private boolean callStarted;
    private Set<Integer> subscribedPublishers = new CopyOnWriteArraySet<>();
    private volatile boolean connectedToJanus;
    private String dialogID;
    private boolean readyToSubscribe;
    private boolean asListenerRole;


    public static void start(Context context, String dialogID, List<Integer> occupants, ArrayList<ConnectycubeUser> allOpponents, boolean listenerRole) {

        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(Consts.EXTRA_DIALOG_ID, dialogID);
        intent.putExtra(Consts.EXTRA_DIALOG_OCCUPANTS, (Serializable) occupants);
        intent.putExtra(Consts.EXTRA_AS_LISTENER, listenerRole);
        intent.putExtra(Consts.EXTRA_OPPONENTS, allOpponents);

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        parseIntentExtras();

        sessionManager = WebRtcSessionManager.getInstance();
        if (!currentSessionExist()) {
//          currentSession == null, so there's no reason to do further initialization
            finish();
            Log.d(TAG, "finish CallActivity");
            return;
        }
        isVideoCall = RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO.equals(currentSession.getConferenceType());
        initCurrentSession(currentSession);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        initConferenceClient();
        initAudioManager();
        initWiFiManagerListener();

        startConversationFragment();
    }

    private boolean currentSessionExist() {
        currentSession = sessionManager.getCurrentSession();
        return currentSession != null;
    }

    @Override
    protected View getSnackbarAnchorView() {
        return null;
    }

    private void parseIntentExtras() {
        dialogID = getIntent().getExtras().getString(Consts.EXTRA_DIALOG_ID);
        opponentsIdsList = (ArrayList<Integer>) getIntent().getSerializableExtra(Consts.EXTRA_DIALOG_OCCUPANTS);
        asListenerRole = getIntent().getBooleanExtra(Consts.EXTRA_AS_LISTENER, false);
        allOpponents = (ArrayList<ConnectycubeUser>) getIntent().getSerializableExtra(EXTRA_OPPONENTS);
    }

    private void initAudioManager() {
        audioManager = AppRTCAudioManager.create(this);

        isVideoCall = RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO.equals(currentSession.getConferenceType());
        if (isVideoCall) {
            audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
            audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
            Log.d(TAG, "AppRTCAudioManager.AudioDevice.SPEAKER_PHONE");
        } else {
            audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
            audioManager.setManageSpeakerPhoneByProximity(true);
            Log.d(TAG, "AppRTCAudioManager.AudioDevice.EARPIECE");
        }

        audioManager.setOnWiredHeadsetStateListener((plugged, hasMicrophone) -> {
            if (callStarted) {
                showToast("Headset " + (plugged ? "plugged" : "unplugged"));
            }
        });

        audioManager.setBluetoothAudioDeviceStateListener(connected -> {
            if (callStarted) {
                showToast("Bluetooth " + (connected ? "connected" : "disconnected"));
            }
        });
    }

    private void initConferenceClient() {
        rtcClient = ConferenceClient.getInstance(this);

        rtcClient.setCameraErrorHandler(new CameraVideoCapturer.CameraEventsHandler() {
            @Override
            public void onCameraError(final String s) {

                showToast("Camera error: " + s);
            }

            @Override
            public void onCameraDisconnected() {
                showToast("Camera onCameraDisconnected: ");
            }

            @Override
            public void onCameraFreezed(String s) {
                showToast("Camera freezed: " + s);
                if (currentSession != null) {
                    leaveCurrentSession();
                }
            }

            @Override
            public void onCameraOpening(String s) {
                showToast("Camera aOpening: " + s);
            }

            @Override
            public void onFirstFrameAvailable() {
                showToast("onFirstFrameAvailable: ");
            }

            @Override
            public void onCameraClosed() {
            }
        });


        // Configure
        //
        SettingsUtil.setSettingsStrategy(opponentsIdsList, sharedPref, CallActivity.this);
        RTCConfig.setDebugEnabled(true);
    }

    @Override
    public void connectivityChanged(boolean availableNow) {
        if (callStarted) {
            showToast("Internet connection " + (availableNow ? "available" : " unavailable"));
        }
    }

    private void initWiFiManagerListener() {
        networkConnectionChecker = new NetworkConnectionChecker(getApplication());
    }

    public void leaveCurrentSession() {
        currentSession.leave();
    }

    private void setAudioEnabled(boolean isAudioEnabled) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            currentSession.getMediaStreamManager().getLocalAudioTrack().setEnabled(isAudioEnabled);
        }
    }

    private void setVideoEnabled(boolean isVideoEnabled) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            currentSession.getMediaStreamManager().getLocalVideoTrack().setEnabled(isVideoEnabled);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readyToSubscribe = true;
        subscribeToPublishersIfNeed();
        networkConnectionChecker.registerListener(this);
    }

    private void subscribeToPublishersIfNeed() {
        Set<Integer> notSubscribedPublishers = new CopyOnWriteArraySet<>(currentSession.getActivePublishers());
        notSubscribedPublishers.removeAll(subscribedPublishers);
        if (!notSubscribedPublishers.isEmpty()) {
            subscribeToPublishers(new ArrayList<>(notSubscribedPublishers));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        readyToSubscribe = false;
        networkConnectionChecker.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void initCurrentSession(ConferenceSession session) {
        if (session != null) {
            Log.d(TAG, "Init new ConferenceSession");
            this.currentSession = session;
            this.currentSession.addSessionCallbacksListener(CallActivity.this);
            this.currentSession.addConferenceSessionListener(CallActivity.this);
        }
    }

    public void releaseCurrentSession() {
        Log.d(TAG, "Release current session");
        if (currentSession != null) {
            leaveCurrentSession();
            this.currentSession.removeSessionCallbacksListener(CallActivity.this);
            this.currentSession.removeConferenceSessionListener(CallActivity.this);
            this.currentSession = null;
        }
    }

    // ---------------Chat callback methods implementation  ----------------------//


    @Override
    public void onConnectionClosedForUser(ConferenceSession session, Integer userID) {
        Log.d(TAG, "RTCSessionStateCallbackImpl onConnectionClosedForUser userID=" + userID);
    }

    @Override
    public void onConnectedToUser(ConferenceSession session, final Integer userID) {
        Log.d(TAG, "onConnectedToUser userID= " + userID + " sessionID= " + session.getSessionID());
        callStarted = true;
        notifyCallStateListenersCallStarted();

        Log.d(TAG, "onConnectedToUser() is started");
    }

    @Override
    public void onStateChanged(ConferenceSession session, BaseSession.RTCSessionState state) {
        if (BaseSession.RTCSessionState.RTC_SESSION_CONNECTED.equals(state)) {
            connectedToJanus = true;
            Log.d(TAG, "onStateChanged and begin subscribeToPublishersIfNeed");
            subscribeToPublishersIfNeed();
        }
    }

    @Override
    public void onDisconnectedFromUser(ConferenceSession session, Integer userID) {
        Log.d(TAG, "RTCSessionStateCallbackImpl onDisconnectedFromUser userID=" + userID);
    }

    private void showToast(final int message) {
        runOnUiThread(() -> Toast.makeText(CallActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(CallActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void startConversationFragment() {
        BaseConversationFragment conversationFragment = BaseConversationFragment.newInstance(
                isVideoCall
                        ? new VideoConversationFragment()
                        : new AudioConversationFragment(), opponentsIdsList, allOpponents, asListenerRole);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, conversationFragment, conversationFragment.getClass().getSimpleName()).commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
    }

    ////////////////////////////// ConversationFragmentCallbackListener ////////////////////////////

    @Override
    public void addClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks) {
        if (currentSession != null) {
            currentSession.addSessionCallbacksListener(clientConnectionCallbacks);
        }
    }

    @Override
    public void onSetAudioEnabled(boolean isAudioEnabled) {
        setAudioEnabled(isAudioEnabled);
    }

    @Override
    public void onLeaveCurrentSession() {
        leaveCurrentSession();
    }

    @Override
    public void onSwitchCamera(CameraVideoCapturer.CameraSwitchHandler cameraSwitchHandler) {
        ((RTCCameraVideoCapturer) (currentSession.getMediaStreamManager().getVideoCapturer()))
                .switchCamera(cameraSwitchHandler);
    }

    @Override
    public void onStartJoinConference() {
        int userID = currentSession.getCurrentUserID();
        ConferenceRole conferenceRole = asListenerRole ? ConferenceRole.LISTENER : ConferenceRole.PUBLISHER;
        currentSession.joinDialog(dialogID, conferenceRole, new JoinedCallback(userID));
    }

    @Override
    public void onSetVideoEnabled(boolean isNeedEnableCam) {
        setVideoEnabled(isNeedEnableCam);
    }

    @Override
    public void onSwitchAudio() {
        AppRTCAudioManager.AudioDevice audioDevice = audioManager.getSelectedAudioDevice() == null ? audioManager.getDefaultAudioDevice()
                : audioManager.getSelectedAudioDevice();

        if (audioDevice == AppRTCAudioManager.AudioDevice.WIRED_HEADSET
                || audioDevice == AppRTCAudioManager.AudioDevice.EARPIECE) {
            audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        } else {
            audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
        }
    }

    @Override
    public void removeClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks) {
        if (currentSession != null) {
            currentSession.removeSessionCallbacksListener(clientConnectionCallbacks);
        }
    }

    @Override
    public void addCurrentCallStateCallback(CurrentCallStateCallback currentCallStateCallback) {
        currentCallStateCallbackList.add(currentCallStateCallback);
    }

    @Override
    public void removeCurrentCallStateCallback(CurrentCallStateCallback currentCallStateCallback) {
        currentCallStateCallbackList.remove(currentCallStateCallback);
    }

    ////////////////////////////// ConferenceSessionCallbacks ////////////////////////////

    private void subscribeToPublishers(ArrayList<Integer> publishersList) {
        subscribedPublishers.addAll(currentSession.getActivePublishers());
        for (Integer publisher : publishersList) {
            currentSession.subscribeToPublisher(publisher);
        }
    }

    @Override
    public void onPublishersReceived(ArrayList<Integer> publishersList) {
        Log.d(TAG, "OnPublishersReceived connectedToJanus " + connectedToJanus + ", readyToSubscribe= " + readyToSubscribe);
        if (connectedToJanus && readyToSubscribe) {
            subscribedPublishers.addAll(publishersList);
            subscribeToPublishers(publishersList);
        }
    }

    @Override
    public void onPublisherLeft(Integer userID) {
        Log.d(TAG, "OnPublisherLeft userID" + userID);
        subscribedPublishers.remove(userID);
    }

    @Override
    public void onMediaReceived(String type, boolean success) {
        Log.d(TAG, "OnMediaReceived type " + type + ", success" + success);
    }

    @Override
    public void onSlowLinkReceived(boolean uplink, int nacks) {
        Log.d(TAG, "OnSlowLinkReceived uplink " + uplink + ", nacks" + nacks);
    }

    @Override
    public void onError(WsException exception) {
        Log.d(TAG, "OnError getClass= " + exception.getClass());
        if (WsHangUpException.class.isInstance(exception)) {
            Log.d(TAG, "OnError exception= " + exception.getMessage());
            if (exception.getMessage().equals(ICE_FAILED_REASON)) {
                showToast(exception.getMessage());
                releaseCurrentSession();
                finish();
            }
        } else {
            showToast((WsNoResponseException.class.isInstance(exception)) ? getString(R.string.packet_failed) : exception.getMessage());
        }
    }

    @Override
    public void onSessionClosed(final ConferenceSession session) {
        Log.d(TAG, "Session " + session.getSessionID() + " start stop session");

        if (session.equals(currentSession)) {
            Log.d(TAG, "Stop session");

            if (audioManager != null) {
                audioManager.stop();
            }
            releaseCurrentSession();

            finish();
        }
    }

    //////////////////////////////////////////   end   /////////////////////////////////////////////


    public interface CurrentCallStateCallback {
        void onCallStarted();
    }

    private void notifyCallStateListenersCallStarted() {
        for (CurrentCallStateCallback callback : currentCallStateCallbackList) {
            callback.onCallStarted();
        }
    }

    private class JoinedCallback implements ConferenceEntityCallback<ArrayList<Integer>> {
        Integer userID;

        JoinedCallback(Integer userID) {
            this.userID = userID;
        }

        @Override
        public void onSuccess(ArrayList<Integer> publishers) {
            Log.d(TAG, "onSuccess joinDialog sessionUserID= " + userID + ", publishers= " + publishers);
            if (rtcClient.isAutoSubscribeAfterJoin()) {
                subscribedPublishers.addAll(publishers);
            }
            if (asListenerRole) {
                connectedToJanus = true;
            }
        }

        @Override
        public void onError(WsException exception) {
            Log.d(TAG, "onError joinDialog exception= " + exception);
            showToast("Join exception: " + exception.getMessage());
            releaseCurrentSession();
            finish();
        }
    }
}