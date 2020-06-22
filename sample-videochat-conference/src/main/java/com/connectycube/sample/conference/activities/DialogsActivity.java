package com.connectycube.sample.conference.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.connectycube.chat.ConnectycubeRestChatService;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.chat.utils.DialogUtils;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.helper.StringifyArrayList;
import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.adapters.DialogsAdapter;
import com.connectycube.sample.conference.utils.Consts;
import com.connectycube.sample.conference.utils.PermissionsChecker;
import com.connectycube.sample.conference.utils.WebRtcSessionManager;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.conference.ConferenceClient;
import com.connectycube.videochat.conference.ConferenceSession;
import com.connectycube.videochat.conference.WsException;
import com.connectycube.videochat.conference.callbacks.ConferenceEntityCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class DialogsActivity extends BaseActivity {
    private static final String TAG = DialogsActivity.class.getSimpleName();
    private static final int REQUEST_SELECT_PEOPLE = 174;
    private static final int REQUEST_PERMISSION = 175;

    private DialogsAdapter dialogsAdapter;
    private ListView dialogsListView;
    private ConnectycubeUser currentUser;
    private ArrayList<ConnectycubeChatDialog> chatDialogs;
    private WebRtcSessionManager webRtcSessionManager;
    private ActionMode currentActionMode;
    private FloatingActionButton fab;
    private String dialogID;
    private List<Integer> occupants;
    private ArrayList<ConnectycubeUser> allOpponents;
    private boolean isVideoCall;

    private PermissionsChecker checker;

    public static void start(Context context) {
        Intent intent = new Intent(context, DialogsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialogs);
        initFields();

        initDefaultActionBar();

        initUi();

        startLoadDialogs();
        loadUsersFromREST();

        checker = new PermissionsChecker(getApplicationContext());
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.list_dialogs);
    }

    private void initFields() {
        currentUser = sharedPrefsHelper.getUser();
        webRtcSessionManager = WebRtcSessionManager.getInstance();
    }

    private void initUi() {
        dialogsListView = findViewById(R.id.list_dialogs);
        fab = findViewById(R.id.fab_dialogs_new_chat);
    }

    private void startLoadDialogs() {
        showProgressDialog(R.string.dlg_loading_dialogs_users);
        ConnectycubeRestChatService.getChatDialogs(ConnectycubeDialogType.GROUP, null).performAsync(new EntityCallback<ArrayList<ConnectycubeChatDialog>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeChatDialog> result, Bundle params) {
                hideProgressDialog();
                chatDialogs = result;
                initDialogAdapter();
            }

            @Override
            public void onError(ResponseException responseException) {
                hideProgressDialog();
                showErrorSnackbar(R.string.loading_users_error, responseException, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startLoadDialogs();
                    }
                });
            }
        });
    }

    private void loadUsersFromREST() {
        showProgressDialog(R.string.dlg_loading_dialogs_users);
        String currentRoomName = sharedPrefsHelper.get(Consts.PREF_CURRENT_ROOM_NAME);

        List<String> tags = new LinkedList<>();
        tags.add(currentRoomName);
        ConnectycubeUsers.getUsersByTags(tags, null).performAsync(new EntityCallback<ArrayList<ConnectycubeUser>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeUser> result, Bundle params) {
                hideProgressDialog();
                allOpponents = new ArrayList<>(result);
            }

            @Override
            public void onError(ResponseException responseException) {
                hideProgressDialog();
                showErrorSnackbar(R.string.loading_users_error, responseException, v -> loadUsersFromREST());
            }
        });
    }

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        currentActionMode = super.startSupportActionMode(callback);
        return currentActionMode;
    }

    private void initDialogAdapter() {
        Log.d(TAG, "proceedInitUsersList chatDialogs= " + chatDialogs);
        if (dialogsAdapter == null) {
            dialogsAdapter = new DialogsAdapter(this, chatDialogs);
            dialogsListView.setAdapter(dialogsAdapter);
            dialogsListView.setOnItemClickListener((parent, view, position, id) -> {
                ConnectycubeChatDialog selectedDialog = (ConnectycubeChatDialog) parent.getItemAtPosition(position);
                if (currentActionMode == null) {
                    Log.d(TAG, "startConference selectedDialog.getDialogId()= " + selectedDialog.getDialogId()
                            + ", currentUser.getId()= " + currentUser.getId());
                    occupants = selectedDialog.getOccupants();
                    occupants.remove(currentUser.getId());
                    dialogID = selectedDialog.getDialogId();


                    dialogsAdapter.toggleOneItem(selectedDialog);
                    invalidateOptionsMenu();

                } else {
                    dialogsAdapter.toggleSelection(selectedDialog);
                    updateActionBar(dialogsAdapter.getSelectedItems().size());
                }
            });
            dialogsListView.setOnItemLongClickListener((parent, view, position, id) -> {
                ConnectycubeChatDialog selectedDialog = (ConnectycubeChatDialog) parent.getItemAtPosition(position);
                startSupportActionMode(new DeleteActionModeCallback());
                dialogsAdapter.selectItem(selectedDialog);
                updateActionBar(dialogsAdapter.getSelectedItems().size());
                return true;
            });
        } else {
            dialogsAdapter.updateList(chatDialogs);
        }
    }

    private void startPermissionsActivity(boolean checkOnlyAudio) {
        PermissionsActivity.startForResult(this, REQUEST_PERMISSION, checkOnlyAudio, Consts.PERMISSIONS);
    }

    private void updateActionBar(int countSelectedUsers) {
        currentActionMode.setSubtitle(null);
        currentActionMode.setTitle(String.format(getString(
                countSelectedUsers > 1
                        ? R.string.tile_many_dialogs_selected
                        : R.string.title_one_dialog_selected),
                countSelectedUsers));
        currentActionMode.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (dialogsAdapter != null && !dialogsAdapter.getSelectedItems().isEmpty()) {
            getMenuInflater().inflate(R.menu.activity_selected_opponents, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_opponents, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.update_opponents_list:
                updateDialogsAdapter();
                loadUsersFromREST();
                return true;

            case R.id.settings:
                showSettings();
                return true;

            case R.id.log_out:
                logOut();
                return true;

            case R.id.start_video_call:
                isVideoCall = true;
                startConference();
                return true;

            case R.id.start_audio_call:
                isVideoCall = false;
                startConference();
                return true;

            case R.id.start_as_listener:
                isVideoCall = true;
                startConference(dialogID, currentUser.getId(), isVideoCall, occupants, true);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startConference() {
        if (checker.lacksPermissions(Consts.PERMISSIONS)) {
            startPermissionsActivity(!isVideoCall);
        } else {
            startConference(dialogID, currentUser.getId(), isVideoCall, occupants, false);
        }
    }

    private void updateDialogsAdapter() {
        startLoadDialogs();
    }

    private void showSettings() {
        SettingsActivity.start(this);
    }

    private void logOut() {
        removeAllUserData();
        startLoginActivity();
    }


    private void removeAllUserData() {
        sharedPrefsHelper.clearAllData();
        ConnectycubeUsers.deleteUser().performAsync(new EntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                Log.d(TAG, "Current user was deleted");
            }

            @Override
            public void onError(ResponseException e) {
                Log.e(TAG, "Current user wasn't deleted" + e);
            }
        });
    }

    private void startLoginActivity() {
        LoginActivity.start(this);
        finish();
    }

    public void onCreateNewDialog(View view) {
        SelectUsersActivity.startForResult(this, REQUEST_SELECT_PEOPLE);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_PEOPLE) {
                ArrayList<ConnectycubeUser> selectedUsers = (ArrayList<ConnectycubeUser>) data
                        .getSerializableExtra(SelectUsersActivity.EXTRA_USERS);

                showProgressDialog(R.string.create_dialog);
                createDialog(selectedUsers);
            }
            if (requestCode == REQUEST_PERMISSION) {
                startConference(dialogID, currentUser.getId(), isVideoCall, occupants, false);
            } else {
                updateDialogsAdapter();
            }
        }
    }

    private void createDialog(final ArrayList<ConnectycubeUser> selectedUsers) {
        ConnectycubeChatDialog dialog = DialogUtils.buildDialog(DialogUtils.createChatNameFromUserList(selectedUsers.toArray(new ConnectycubeUser[selectedUsers.size()])),
                ConnectycubeDialogType.GROUP, DialogUtils.getUserIds(selectedUsers));

        ConnectycubeRestChatService.createChatDialog(dialog).performAsync(
                new EntityCallback<ConnectycubeChatDialog>() {
                    @Override
                    public void onSuccess(ConnectycubeChatDialog dialog, Bundle args) {
//                        dialogsManager.sendSystemMessageAboutCreatingDialog(systemMessagesManager, dialog);
                        Log.d(TAG, "createDialogWithSelectedUsers dialog name= " + dialog.getName());
                        updateDialogsAdapter();
                        hideProgressDialog();
                    }

                    @Override
                    public void onError(ResponseException e) {
                        hideProgressDialog();
                        showErrorSnackbar(R.string.dialogs_creation_error, null, null);
                    }
                }
        );
    }

    private class DeleteActionModeCallback implements ActionMode.Callback {

        public DeleteActionModeCallback() {
            fab.hide();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.activity_selected_dialogs, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete_dialog:
                    deleteSelectedDialogs();
                    if (currentActionMode != null) {
                        currentActionMode.finish();
                    }
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            currentActionMode = null;
            dialogsAdapter.clearSelection();
            invalidateOptionsMenu();
            fab.show();
        }


        private void deleteSelectedDialogs() {
            final Collection<ConnectycubeChatDialog> selectedDialogs = dialogsAdapter.getSelectedItems();

            StringifyArrayList<String> dialogsIds = new StringifyArrayList<>();
            for (ConnectycubeChatDialog dialog : selectedDialogs) {
                dialogsIds.add(dialog.getDialogId());
            }

            ConnectycubeRestChatService.deleteDialogs(dialogsIds, false, null).performAsync(new EntityCallback<ArrayList<String>>() {
                @Override
                public void onSuccess(ArrayList<String> dialogsIds, Bundle bundle) {
                    updateDialogsAdapter();
                }

                @Override
                public void onError(ResponseException e) {
                    showErrorSnackbar(R.string.dialogs_deletion_error, e,
                            v -> deleteSelectedDialogs());
                }
            });
        }
    }

    private void startConference(final String dialogID, int userID, boolean isVideoCall, final List<Integer> occupants, final boolean asListener) {
        Log.d(TAG, "startConference()");
        showProgressDialog(R.string.join_conference);
        ConferenceClient client = ConferenceClient.getInstance(getApplicationContext());

        RTCTypes.ConferenceType conferenceType = isVideoCall
                ? RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO
                : RTCTypes.ConferenceType.CONFERENCE_TYPE_AUDIO;

        client.createSession(userID, conferenceType, new ConferenceEntityCallback<ConferenceSession>() {
            @Override
            public void onSuccess(ConferenceSession session) {
                hideProgressDialog();
                webRtcSessionManager.setCurrentSession(session);
                Log.d(TAG, "DialogActivity setCurrentSession onSuccess() session getCurrentUserID= " + session.getCurrentUserID());

                CallActivity.start(DialogsActivity.this, dialogID, occupants, allOpponents, asListener);
            }

            @Override
            public void onError(WsException responseException) {
                hideProgressDialog();
                showErrorSnackbar(R.string.join_conference_error, null, null);
            }
        });
    }
}
