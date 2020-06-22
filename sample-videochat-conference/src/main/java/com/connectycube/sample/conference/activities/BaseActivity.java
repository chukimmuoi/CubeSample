package com.connectycube.sample.conference.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;

import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.utils.SharedPrefsHelper;
import com.connectycube.sample.conference.utils.Consts;


public abstract class BaseActivity extends AppCompatActivity {

    protected SharedPrefsHelper sharedPrefsHelper;
    protected ActionBar actionBar;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefsHelper = SharedPrefsHelper.getInstance();
        actionBar = getSupportActionBar();
    }

    public void initDefaultActionBar() {
        String currentUserFullName = "";
        String currentRoomName = sharedPrefsHelper.get(Consts.PREF_CURRENT_ROOM_NAME, "");

        if (sharedPrefsHelper.getUser() != null) {
            currentUserFullName = sharedPrefsHelper.getUser().getFullName();
        }

        setActionBarTitle(currentRoomName);
        setActionbarSubTitle(String.format(getString(R.string.subtitle_text_logged_in_as), currentUserFullName));
    }

    protected void setActionBarTitle(CharSequence title) {
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    protected void setActionBarTitle(int title) {
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    public void setActionbarSubTitle(String subTitle) {
        if (actionBar != null)
            actionBar.setSubtitle(subTitle);
    }

    public void removeActionbarSubTitle() {
        if (actionBar != null)
            actionBar.setSubtitle(null);
    }

    void showProgressDialog(@StringRes int messageId) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            // Disable the back button
            DialogInterface.OnKeyListener keyListener = (dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK;
            progressDialog.setOnKeyListener(keyListener);
        }

        progressDialog.setMessage(getString(messageId));

        progressDialog.show();

    }

    void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    protected void showErrorSnackbar(@StringRes int resId, Exception e,
                                     View.OnClickListener clickListener) {
        if (getSnackbarAnchorView() != null) {
            showSnackbar(getSnackbarAnchorView(), resId, e,
                    R.string.dlg_retry, clickListener);
        }
    }

    private Snackbar showSnackbar(View view, int resId, Exception e,
                                  @StringRes int actionLabel,
                                  View.OnClickListener clickListener) {
        Snackbar snackbar = Snackbar.make(view, String.format("%s: %s", getString(R.string.login_chat_login_error), (e == null) ? "" : e.getMessage()), Snackbar.LENGTH_INDEFINITE);
        if (clickListener != null) {
            snackbar.setAction(actionLabel, clickListener);
        }
        snackbar.show();
        return snackbar;
    }

    protected abstract View getSnackbarAnchorView();
}




