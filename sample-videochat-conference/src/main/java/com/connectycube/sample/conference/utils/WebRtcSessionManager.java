package com.connectycube.sample.conference.utils;

import com.connectycube.videochat.conference.ConferenceSession;


public class WebRtcSessionManager {
    private static final String TAG = WebRtcSessionManager.class.getSimpleName();

    private static WebRtcSessionManager instance;

    private static ConferenceSession currentSession;

    private WebRtcSessionManager() {

    }

    public static WebRtcSessionManager getInstance(){
        if (instance == null){
            instance = new WebRtcSessionManager();
        }

        return instance;
    }

    public ConferenceSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(ConferenceSession session) {
        currentSession = session;
    }
}
