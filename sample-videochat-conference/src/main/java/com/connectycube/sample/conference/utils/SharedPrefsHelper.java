package com.connectycube.sample.conference.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.connectycube.core.helper.StringifyArrayList;
import com.connectycube.sample.conference.App;
import com.connectycube.users.model.ConnectycubeUser;

public class SharedPrefsHelper {
    private static final String SHARED_PREFS_NAME = "connectycube_conference";

    private static final String USER_ID = "user_id";
    private static final String USER_LOGIN = "user_login";
    private static final String USER_PASSWORD = "user_password";
    private static final String USER_FULL_NAME = "user_full_name";
    private static final String USER_TAGS = "user_tags";

    private static SharedPrefsHelper instance;

    private SharedPreferences sharedPreferences;

    public static synchronized SharedPrefsHelper getInstance() {
        if (instance == null) {
            instance = new SharedPrefsHelper();
        }

        return instance;
    }

    private SharedPrefsHelper() {
        instance = this;
        sharedPreferences = App.getInstance().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void delete(String key) {
        if (sharedPreferences.contains(key)) {
            getEditor().remove(key).commit();
        }
    }

    public void save(String key, Object value) {
        SharedPreferences.Editor editor = getEditor();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Enum) {
            editor.putString(key, value.toString());
        } else if (value != null) {
            throw new RuntimeException("Attempting to save non-supported preference");
        }

        editor.commit();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) sharedPreferences.getAll().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defValue) {
        T returnValue = (T) sharedPreferences.getAll().get(key);
        return returnValue == null ? defValue : returnValue;
    }

    public boolean has(String key) {
        return sharedPreferences.contains(key);
    }


    public void saveUser(ConnectycubeUser user) {
        save(USER_ID, user.getId());
        save(USER_LOGIN, user.getLogin());
        save(USER_PASSWORD, user.getPassword());
        save(USER_FULL_NAME, user.getFullName());
        save(USER_TAGS, user.getTags().getItemsAsString());
    }

    public void removeUser() {
        delete(USER_ID);
        delete(USER_LOGIN);
        delete(USER_PASSWORD);
        delete(USER_FULL_NAME);
        delete(USER_TAGS);
    }

    public ConnectycubeUser getUser() {
        if (hasUser()) {
            Integer id = get(USER_ID);
            String login = get(USER_LOGIN);
            String password = get(USER_PASSWORD);
            String fullName = get(USER_FULL_NAME);
            String tagsInString = get(USER_TAGS);

            StringifyArrayList<String> tags = null;

            if (tagsInString != null) {
                tags = new StringifyArrayList<>();
                tags.add(tagsInString.split(","));
            }

            ConnectycubeUser user = new ConnectycubeUser(login, password);
            user.setId(id);
            user.setFullName(fullName);
            user.setTags(tags);
            return user;
        } else {
            return null;
        }
    }

    public boolean hasUser() {
        return has(USER_LOGIN) && has(USER_PASSWORD);
    }

    public void clearAllData(){
        SharedPreferences.Editor editor = getEditor();
        editor.clear().commit();
    }

    private SharedPreferences.Editor getEditor() {
        return sharedPreferences.edit();
    }
}
