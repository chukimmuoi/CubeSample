package com.connectycube.sample.conference.utils;

import android.Manifest;


public interface Consts {

    String DEFAULT_USER_PASSWORD = "cgsdfCsd";

    int ERR_LOGIN_ALREADY_TAKEN_HTTP_STATUS = 422;
    int ERR_MSG_DELETING_HTTP_STATUS = 401;

    String PREF_CURRENT_ROOM_NAME = "current_room_name";

    String EXTRA_DIALOG_ID = "dialog_id";
    String EXTRA_DIALOG_OCCUPANTS = "dialog_occupants";
    String EXTRA_AS_LISTENER = "role_as_listener";
    String EXTRA_OPPONENTS = "opponents_list";

    String EXTRA_LOGIN_RESULT = "login_result";
    String EXTRA_LOGIN_ERROR_MESSAGE = "login_error_message";
    int EXTRA_LOGIN_RESULT_CODE = 1002;

    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
}
