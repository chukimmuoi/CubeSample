package com.connectycube.sample.conference.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.helper.StringifyArrayList;
import com.connectycube.core.helper.Utils;
import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.utils.Consts;
import com.connectycube.sample.conference.utils.ValidationUtils;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;

public class LoginActivity extends BaseActivity {

    private String TAG = LoginActivity.class.getSimpleName();

    private EditText userNameEditText;
    private EditText chatRoomNameEditText;

    private ConnectycubeUser userForSave;

    public static void start(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        performLoginAction();
        initUI();
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.root_view_login_activity);
    }

    private void initUI() {
        setActionBarTitle(R.string.title_login_activity);
        userNameEditText = findViewById(R.id.user_name);
        userNameEditText.addTextChangedListener(new LoginEditTextWatcher(userNameEditText));

        chatRoomNameEditText = findViewById(R.id.chat_room_name);
        chatRoomNameEditText.addTextChangedListener(new LoginEditTextWatcher(chatRoomNameEditText));
    }

    protected void performLoginAction() {
        if (sharedPrefsHelper.hasUser()) {
            startDialogsActivity();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_login_user_done:
                if (isEnteredUserNameValid() && isEnteredRoomNameValid()) {
                    hideKeyboard();
                    startSignUpNewUser(createUserWithEnteredData());
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isEnteredRoomNameValid() {
        return ValidationUtils.isRoomNameValid(this, chatRoomNameEditText);
    }

    private boolean isEnteredUserNameValid() {
        return ValidationUtils.isUserNameValid(this, userNameEditText);
    }

    private void hideKeyboard() {
        hideKeyboard(userNameEditText);
        hideKeyboard(chatRoomNameEditText);
    }

    private void startSignUpNewUser(final ConnectycubeUser newUser) {
        showProgressDialog(R.string.dlg_creating_new_user);


        ConnectycubeUsers.signUp(newUser).performAsync(
                new EntityCallback<ConnectycubeUser>() {
                    @Override
                    public void onSuccess(ConnectycubeUser user, Bundle params) {
                        signIn(user);
                    }

                    @Override
                    public void onError(ResponseException e) {
                        if (e.getHttpStatusCode() == Consts.ERR_LOGIN_ALREADY_TAKEN_HTTP_STATUS) {
                            signInCreatedUser(newUser, true);
                        } else {
                            hideProgressDialog();
                            Toast.makeText(LoginActivity.this, R.string.sign_up_error, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void signIn(final ConnectycubeUser user) {
        user.setPassword(Consts.DEFAULT_USER_PASSWORD);

        userForSave = user;
        saveUserData(userForSave);

        signInCreatedUser(userForSave, false);
    }

    private void startDialogsActivity() {
        DialogsActivity.start(LoginActivity.this);
        finish();
    }

    private void saveUserData(ConnectycubeUser user) {
        sharedPrefsHelper.save(Consts.PREF_CURRENT_ROOM_NAME, user.getTags().get(0));
        sharedPrefsHelper.saveUser(user);
    }

    private ConnectycubeUser createUserWithEnteredData() {
        return createConnectycubeUserWithCurrentData(String.valueOf(userNameEditText.getText()),
                String.valueOf(chatRoomNameEditText.getText()));
    }

    private ConnectycubeUser createConnectycubeUserWithCurrentData(String userName, String chatRoomName) {
        ConnectycubeUser user = null;
        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(chatRoomName)) {
            StringifyArrayList<String> userTags = new StringifyArrayList<>();
            userTags.add(chatRoomName);

            user = new ConnectycubeUser();
            user.setFullName(userName);
            user.setLogin(getCurrentDeviceId());
            user.setPassword(Consts.DEFAULT_USER_PASSWORD);
            user.setTags(userTags);
        }

        return user;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Consts.EXTRA_LOGIN_RESULT_CODE) {
            hideProgressDialog();
            boolean isLoginSuccess = data.getBooleanExtra(Consts.EXTRA_LOGIN_RESULT, false);
            String errorMessage = data.getStringExtra(Consts.EXTRA_LOGIN_ERROR_MESSAGE);

            if (isLoginSuccess) {
                saveUserData(userForSave);

                signInCreatedUser(userForSave, false);
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.login_chat_login_error) + errorMessage, Toast.LENGTH_LONG).show();
                userNameEditText.setText(userForSave.getFullName());
                chatRoomNameEditText.setText(userForSave.getTags().get(0));
            }
        }
    }

    private void signInCreatedUser(final ConnectycubeUser user, final boolean deleteCurrentUser) {
        ConnectycubeUsers.signIn(user).performAsync(new EntityCallback<ConnectycubeUser>() {
            @Override
            public void onSuccess(ConnectycubeUser result, Bundle params) {
                if (deleteCurrentUser) {
                    removeAllUserData(result);
                } else {
                    hideProgressDialog();
                    startDialogsActivity();
                }
            }

            @Override
            public void onError(ResponseException responseException) {
                hideProgressDialog();
                Toast.makeText(LoginActivity.this, R.string.sign_up_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void removeAllUserData(final ConnectycubeUser user) {
        ConnectycubeUsers.deleteUser().performAsync(new EntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                sharedPrefsHelper.clearAllData();
                startSignUpNewUser(createUserWithEnteredData());
            }

            @Override
            public void onError(ResponseException e) {
                hideProgressDialog();
                Toast.makeText(LoginActivity.this, R.string.sign_up_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getCurrentDeviceId() {
        return Utils.generateDeviceId(this);
    }

    private class LoginEditTextWatcher implements TextWatcher {
        private EditText editText;

        private LoginEditTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            editText.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    public void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
