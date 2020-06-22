package com.connectycube.sample.conference.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.connectycube.chat.ConnectycubeRestChatService;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.request.DialogRequestBuilder;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.request.PagedRequestBuilder;
import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.adapters.CheckboxUsersAdapter;
import com.connectycube.sample.conference.utils.Consts;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class SelectUsersActivity extends BaseActivity {
    public static final String EXTRA_USERS = "users";
    public static final String EXTRA_OCCUPANTS_IDS = "occupants_ids";
    public static final int MINIMUM_CHAT_OCCUPANTS_SIZE = 2;
    public static final int USERS_PER_PAGE = 50;
    private static final long CLICK_DELAY = TimeUnit.SECONDS.toMillis(2);

    private static final String EXTRA_DIALOG = "dialog";

    private ListView usersListView;
    private CheckboxUsersAdapter usersAdapter;
    private long lastClickTime = 0l;
    private ConnectycubeUser currentUser;
    private ConnectycubeChatDialog dialog;

    public static void startForResult(Fragment fragment, int code, ConnectycubeChatDialog dialog) {
        Intent intent = new Intent(fragment.getContext(), SelectUsersActivity.class);

        intent.putExtra(EXTRA_DIALOG, dialog);
        fragment.startActivityForResult(intent, code);
    }

    /**
     * Start activity for picking users
     *
     * @param activity activity to return result
     * @param code     request code for onActivityResult() method
     *                 <p>
     *                 in onActivityResult there will be 'ArrayList<ConnectycubeUser>' in the intent extras
     *                 which can be obtained with SelectPeopleActivity.EXTRA_USERS key
     */
    public static void startForResult(Activity activity, int code) {
        Intent intent = new Intent(activity, SelectUsersActivity.class);

        activity.startActivityForResult(intent, code);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_users);

        usersListView = findViewById(R.id.list_select_users);
        currentUser = sharedPrefsHelper.getUser();
        setActionBarTitle(isEditingChat() ? R.string.select_users_edit_dialog : R.string.select_users_create_dialog);
        actionBar.setDisplayHomeAsUpEnabled(true);

        initUsersAdapter();
    }

    @Override
    protected void onDestroy() {
        hideProgressDialog();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_select_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ((SystemClock.uptimeMillis() - lastClickTime) < CLICK_DELAY) {
            return super.onOptionsItemSelected(item);
        }
        lastClickTime = SystemClock.uptimeMillis();

        switch (item.getItemId()) {
            case R.id.menu_select_people_action_done:
                if (isEditingChat()) {
                    addOccupantsToDialog();
                } else if (usersAdapter != null) {
                    List<ConnectycubeUser> users = usersAdapter.getSelectedUsers();
                    if (users.size() >= MINIMUM_CHAT_OCCUPANTS_SIZE) {
                        passResultToCallerActivity(null);
                    } else {
                        Toast.makeText(this, R.string.select_users_choose_users, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case R.id.menu_refresh_users:
                if (isEditingChat()) {
                    updateDialogAndUsers();
                } else {
                    loadUsersFromREST();
                }
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.list_select_users);
    }

    private void addOccupantsToDialog() {
        showProgressDialogIfPossible(R.string.dlg_updating_dialog);

        List<ConnectycubeUser> users = usersAdapter.getSelectedUsers();
        ConnectycubeUser[] usersArray = users.toArray(new ConnectycubeUser[users.size()]);
        Log.d("SelectedUsersActivity", "usersArray= " + Arrays.toString(usersArray));

        DialogRequestBuilder requestBuilder = new DialogRequestBuilder();
        requestBuilder.addUsers(usersArray);

        ConnectycubeRestChatService.updateChatDialog(dialog, requestBuilder).performAsync(new EntityCallback<ConnectycubeChatDialog>() {
            @Override
            public void onSuccess(ConnectycubeChatDialog dialog, Bundle params) {
                dismissProgressDialogIfPossible();
                passResultToCallerActivity(dialog.getOccupants());
            }

            @Override
            public void onError(ResponseException responseException) {
                dismissProgressDialogIfPossible();
                showErrorSnackbar(R.string.dlg_updating_dialog, responseException, v -> addOccupantsToDialog());
            }
        });
    }

    private void removeExistentOccupants(List<ConnectycubeUser> users) {
        List<Integer> userIDs = dialog.getOccupants();
        if (userIDs == null) {
            return;
        }

        Iterator<ConnectycubeUser> i = users.iterator();
        while (i.hasNext()) {
            ConnectycubeUser user = i.next();

            for (Integer userID : userIDs) {
                if (user.getId().equals(userID)) {
                    Log.d("SelectedUsersActivity", "users.remove(user)= " + user);
                    i.remove();
                }
            }
        }
    }

    private void passResultToCallerActivity(List<Integer> occupantsIds) {
        Intent result = new Intent();
        ArrayList<ConnectycubeUser> selectedUsers = new ArrayList<>(usersAdapter.getSelectedUsers());
        result.putExtra(EXTRA_USERS, selectedUsers);
        if (occupantsIds != null) {
            result.putExtra(EXTRA_OCCUPANTS_IDS, (Serializable) occupantsIds);
        }
        setResult(RESULT_OK, result);
        finish();
    }

    private void initUsersAdapter() {
        dialog = (ConnectycubeChatDialog) getIntent().getSerializableExtra(EXTRA_DIALOG);
        if (dialog != null) {
            updateDialogAndUsers();
        } else {
            loadUsersFromREST();
        }
        usersAdapter = new CheckboxUsersAdapter(SelectUsersActivity.this, new ArrayList<>(), currentUser);
        usersListView.setAdapter(usersAdapter);
    }

    private boolean isEditingChat() {
        return getIntent().getSerializableExtra(EXTRA_DIALOG) != null;
    }

    private void updateDialogAndUsers() {
        showProgressDialogIfPossible(R.string.dlg_loading_dialogs_users);
        ConnectycubeRestChatService.getChatDialogById(dialog.getDialogId()).performAsync(new EntityCallback<ConnectycubeChatDialog>() {
            @Override
            public void onSuccess(ConnectycubeChatDialog dialog, Bundle params) {
                SelectUsersActivity.this.dialog.setOccupantsIds(dialog.getOccupants());
                loadUsersFromREST();
                dismissProgressDialogIfPossible();
            }

            @Override
            public void onError(ResponseException responseException) {
                dismissProgressDialogIfPossible();
                showErrorSnackbar(R.string.loading_dialog_error, responseException, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadUsersFromREST();
                    }
                });
            }
        });
    }

    private void showProgressDialogIfPossible(@StringRes int messageId) {
        if (!isFinishing()) {
            showProgressDialog(messageId);
        }
    }

    private void dismissProgressDialogIfPossible() {
        if (!isFinishing()) {
            hideProgressDialog();
        }
    }

    private void loadUsersFromREST() {
        showProgressDialogIfPossible(R.string.dlg_loading_dialogs_users);
        String currentRoomName = sharedPrefsHelper.get(Consts.PREF_CURRENT_ROOM_NAME);

        PagedRequestBuilder requestBuilder = new PagedRequestBuilder();
        requestBuilder.setPerPage(USERS_PER_PAGE);
        List<String> tags = new LinkedList<>();
        tags.add(currentRoomName);

        ConnectycubeUsers.getUsersByTags(tags, requestBuilder).performAsync(new EntityCallback<ArrayList<ConnectycubeUser>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeUser> users, Bundle params) {
                if (isEditingChat()) {
                    users.remove(currentUser);
                    removeExistentOccupants(users);
                }
                usersAdapter.updateList(users);

                dismissProgressDialogIfPossible();
            }

            @Override
            public void onError(ResponseException responseException) {
                dismissProgressDialogIfPossible();
                showErrorSnackbar(R.string.loading_users_error, responseException, v -> loadUsersFromREST());
            }
        });
    }
}
