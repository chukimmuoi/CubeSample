package com.connectycube.sample.conference;

import android.app.Application;
import android.text.TextUtils;

import com.connectycube.auth.session.ConnectycubeSettings;
import com.connectycube.videochat.conference.ConferenceConfig;


public class App extends Application {
    private final static String APPLICATION_ID = "2725";
    private final static String AUTH_KEY       = "SEz8FZzfDqf-qqR";
    private final static String AUTH_SECRET    = "gZKtvNMLOpFaO8b";
    private final static String ACCOUNT_KEY    = "SUXyLLntbrzsFm5p9JkP";

    private final static String JANUS_SERVER_URL = "wss://janus.connectycube.com:8989";

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initApplication();
        checkEndpoints();
        initConferenceConfig();
        checkMultiServer();
        initCredentials();
    }

    private void initApplication() {
        instance = this;
    }

    private void initConferenceConfig() {
        if (!TextUtils.isEmpty(JANUS_SERVER_URL)) {
            ConferenceConfig.setUrl(JANUS_SERVER_URL);
        }
    }

    private void checkEndpoints() {
        if (APPLICATION_ID.isEmpty() || AUTH_KEY.isEmpty() || AUTH_SECRET.isEmpty()) {
            throw new AssertionError(getString(R.string.error_credentials_empty));
        }
    }

    private void checkMultiServer() {
        if (ConferenceConfig.getUrl() == null) {
            throw new AssertionError(getString(R.string.error_server_url_null));
        }
    }

    private void initCredentials() {
        ConnectycubeSettings.getInstance().init(this, APPLICATION_ID, AUTH_KEY, AUTH_SECRET);
        ConnectycubeSettings.getInstance().setAccountKey(ACCOUNT_KEY);

        // Uncomment and put your Api and Chat servers endpoints to point the sample
        // against your own server.
//        ConnectycubeSettings.getInstance().setEndpoints("https://your_api_endpoint.com", "your_chat_endpoint", ServiceZone.PRODUCTION);
//        ConnectycubeSettings.getInstance().setZone(ServiceZone.PRODUCTION);
    }
}
