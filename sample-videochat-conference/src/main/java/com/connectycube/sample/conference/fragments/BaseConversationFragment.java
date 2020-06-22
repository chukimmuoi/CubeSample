package com.connectycube.sample.conference.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.activities.CallActivity;
import com.connectycube.sample.conference.activities.SelectUsersActivity;
import com.connectycube.sample.conference.adapters.OpponentsFromCallAdapter;
import com.connectycube.sample.conference.utils.CollectionsUtils;
import com.connectycube.sample.conference.utils.Consts;
import com.connectycube.sample.conference.utils.SharedPrefsHelper;
import com.connectycube.sample.conference.utils.WebRtcSessionManager;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.BaseSession;
import com.connectycube.videochat.callbacks.RTCClientVideoTracksCallback;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;
import com.connectycube.videochat.conference.ConferenceSession;
import com.connectycube.videochat.conference.view.ConferenceSurfaceView;
import com.connectycube.videochat.view.RTCVideoTrack;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.connectycube.sample.conference.utils.Consts.EXTRA_OPPONENTS;

public abstract class BaseConversationFragment extends BaseToolBarFragment implements CallActivity.CurrentCallStateCallback, RTCSessionStateCallback<ConferenceSession>,
        RTCClientVideoTracksCallback<ConferenceSession>, OpponentsFromCallAdapter.OnAdapterEventListener {

    private static final String TAG = BaseConversationFragment.class.getSimpleName();

    protected static final long LOCAL_TRACk_INITIALIZE_DELAY = 500;
    private static final int REQUEST_ADD_OCCUPANTS = 175;

    private static final int DISPLAY_ROW_AMOUNT = 3;
    private static final int SMALL_CELLS_AMOUNT = 8;
    private static final int LARGE_CELLS_AMOUNT = 12;

    protected WebRtcSessionManager sessionManager;
    protected ConferenceSession currentSession;
    protected ArrayList<ConnectycubeUser> opponents;
    protected ArrayList<Integer> opponentsIds;
    private Set<Integer> usersToDestroy;
    private boolean allCallbacksInit;

    private RecyclerView recyclerView;
    protected ConferenceSurfaceView localVideoView;
    private List<ConnectycubeUser> allOpponents;
    protected boolean isRemoteShown;
    protected TextView connectionStatusLocal;
    protected LinearLayout actionButtonsLayout;
    protected OpponentsFromCallAdapter opponentsAdapter;

    private GridLayoutManager gridLayoutManager;
    private SpanSizeLookupImpl spanSizeLookup;

    protected boolean isNeedCleanUp;

    private ToggleButton micToggleCall;
    private ImageButton handUpCall;
    protected ConversationFragmentCallbackListener conversationFragmentCallbackListener;
    protected View outgoingOpponentsRelativeLayout;
    protected TextView allOpponentsTextView;
    protected TextView ringingTextView;
    protected ConnectycubeUser currentUser;
    protected SharedPrefsHelper sharedPrefsHelper;
    protected Map<Integer, RTCVideoTrack> videoTrackMap;
    protected boolean asListenerRole;


    private SparseArray<OpponentsFromCallAdapter.ViewHolder> opponentViewHolders;

    public static BaseConversationFragment newInstance(BaseConversationFragment baseConversationFragment, ArrayList<Integer> opponentsIdsList,
                                                       ArrayList<ConnectycubeUser> allOpponents, boolean asListenerRole) {
        Bundle args = new Bundle();
        args.putIntegerArrayList(Consts.EXTRA_DIALOG_OCCUPANTS, opponentsIdsList);
        args.putBoolean(Consts.EXTRA_AS_LISTENER, asListenerRole);
        args.putSerializable(EXTRA_OPPONENTS, allOpponents);
        baseConversationFragment.setArguments(args);
        return baseConversationFragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            conversationFragmentCallbackListener = (ConversationFragmentCallbackListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement ConversationFragmentCallbackListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPrefsHelper = SharedPrefsHelper.getInstance();
        conversationFragmentCallbackListener.addCurrentCallStateCallback(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        opponentsIds = this.getArguments().getIntegerArrayList(Consts.EXTRA_DIALOG_OCCUPANTS);
        asListenerRole = this.getArguments().getBoolean(Consts.EXTRA_AS_LISTENER);
        allOpponents = (ArrayList<ConnectycubeUser>) getArguments().getSerializable(EXTRA_OPPONENTS);
        sessionManager = WebRtcSessionManager.getInstance();
        currentSession = sessionManager.getCurrentSession();
        if (currentSession == null) {
            Log.d(TAG, "currentSession = null onCreateView");
            return view;
        }
        initFields();
        initViews(view);
        initActionBar();
        initButtonsListener();
        prepareAndShowOutgoingScreen();

        return view;
    }

    private void prepareAndShowOutgoingScreen() {
        configureOutgoingScreen();
        allOpponentsTextView.setText(CollectionsUtils.makeStringFromUsersFullNames(opponents));
    }

    @Override
    int getFragmentLayout() {
        return R.layout.fragment_conversation;
    }

    protected void configureOutgoingScreen() {
        outgoingOpponentsRelativeLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.grey_transparent_50));
        allOpponentsTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        ringingTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
    }

    private void initActionBar() {
        configureToolbar();
        configureActionBar();
    }

    protected void configureActionBar() {
        actionBar = ((AppCompatActivity) getActivity()).getDelegate().getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    protected void configureToolbar() {
        toolbar.setVisibility(View.VISIBLE);
        toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.black_transparent_50));
        toolbar.setTitleTextColor(ContextCompat.getColor(getContext(), R.color.white));
        toolbar.setSubtitleTextColor(ContextCompat.getColor(getContext(), R.color.white));
    }

    protected void initFields() {
        currentUser = sharedPrefsHelper.getUser();
        sessionManager = WebRtcSessionManager.getInstance();
        currentSession = sessionManager.getCurrentSession();

        initOpponentsList();

        usersToDestroy = new HashSet<>();

        Log.d(TAG, "opponents: " + opponents.toString());
        Log.d(TAG, "currentSession " + currentSession.toString());
    }

    protected void setOpponentToAdapter(Integer userID) {
        ConnectycubeUser user = getUserById(userID);
        if (user != null) {
            opponentsAdapter.add(user);
        } else {
            ConnectycubeUser connectycubeUser = new ConnectycubeUser(userID);
            connectycubeUser.setFullName("NoName");
            opponentsAdapter.add(connectycubeUser);
        }
        recyclerView.requestLayout();
    }

    public void setDuringCallActionBar() {
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(currentUser.getFullName());
    }

    protected void updateActionBar(int amountOpponents) {
        actionBar.setSubtitle(getString(R.string.opponents, String.valueOf(amountOpponents)));
    }

    private void setProgressBarForOpponentGone(int userId) {
        final OpponentsFromCallAdapter.ViewHolder holder = getViewHolderForOpponent(userId);
        if (holder == null) {
            return;
        }
        holder.getProgressBar().setVisibility(View.GONE);
    }

    @Override
    public void onToggleButtonItemClick(int position, boolean isAudioEnabled) {
        int userId = opponentsAdapter.getItem(position);
        Log.d(TAG, "onToggleButtonItemClick userId= " + userId);
        adjustOpponentAudio(userId, isAudioEnabled);
    }

    private void adjustOpponentAudio(int userID, boolean isAudioEnabled) {
        currentSession.getMediaStreamManager().getAudioTrack(userID).setEnabled(isAudioEnabled);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_ADD_OCCUPANTS) {
                Log.d(TAG, "onActivityResult REQUEST_ADD_OCCUPANTS");
                ArrayList<ConnectycubeUser> addedOccupants = (ArrayList<ConnectycubeUser>) data
                        .getSerializableExtra(SelectUsersActivity.EXTRA_USERS);
                List<Integer> allOccupants = (List<Integer>) data
                        .getSerializableExtra(SelectUsersActivity.EXTRA_OCCUPANTS_IDS);
                allOpponents.addAll(0, addedOccupants);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (currentSession == null) {
            Log.d(TAG, "currentSession = null onStart");
            return;
        }

        if (currentSession.getState() != BaseSession.RTCSessionState.RTC_SESSION_CONNECTED) {
            startJoinConference();
        }
        if (!allCallbacksInit) {
            conversationFragmentCallbackListener.addClientConnectionCallback(this);
            initTrackListeners();
            allCallbacksInit = true;
        }
    }

    protected void initTrackListeners() {
        initVideoTracksListener();
    }

    protected void removeTrackListeners() {
        removeVideoTracksListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        isNeedCleanUp = true;
        cleanAdapterIfNeed();
    }

    @Override
    public void onPause() {
        super.onPause();
        isNeedCleanUp = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        removeVideoTrackRenders();
        releaseViews();
        releaseViewHolders();
        removeConnectionStateListeners();
        removeTrackListeners();
    }

    private void removeConnectionStateListeners() {
        conversationFragmentCallbackListener.removeClientConnectionCallback(this);
    }


    protected void releaseOpponentsViews() {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int childCount = layoutManager.getChildCount();
        Log.d(TAG, " releaseOpponentsViews for  " + childCount + " views");
        for (int i = 0; i < childCount; i++) {
            View childView = layoutManager.getChildAt(i);
            Log.d(TAG, " release View for  " + i + ", " + childView);
            OpponentsFromCallAdapter.ViewHolder childViewHolder = (OpponentsFromCallAdapter.ViewHolder) recyclerView.getChildViewHolder(childView);
            childViewHolder.getOpponentView().release();
        }
    }

    @Override
    public void onDestroy() {
        conversationFragmentCallbackListener.removeCurrentCallStateCallback(this);
        super.onDestroy();
    }

    private void startJoinConference() {
        conversationFragmentCallbackListener.onStartJoinConference();
    }

    protected void initViews(View view) {
        Log.i(TAG, "initViews");
        micToggleCall = view.findViewById(R.id.toggle_mic);
        handUpCall = view.findViewById(R.id.button_hangup_call);
        outgoingOpponentsRelativeLayout = view.findViewById(R.id.layout_background_outgoing_screen);
        allOpponentsTextView = view.findViewById(R.id.text_outgoing_opponents_names);
        ringingTextView = view.findViewById(R.id.text_ringing);

        opponentViewHolders = new SparseArray<>(opponents.size());
        isRemoteShown = false;

        localVideoView = view.findViewById(R.id.local_video_view);

        recyclerView = view.findViewById(R.id.grid_opponents);

        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), R.dimen.grid_item_divider));
        recyclerView.setHasFixedSize(false);

        gridLayoutManager = new GridManager(getContext(), 12);
        gridLayoutManager.setReverseLayout(false);
        spanSizeLookup = new SpanSizeLookupImpl();
        spanSizeLookup.setSpanIndexCacheEnabled(false);
        gridLayoutManager.setSpanSizeLookup(spanSizeLookup);
        recyclerView.setLayoutManager(gridLayoutManager);

//          for correct removing item in adapter
        recyclerView.setItemAnimator(null);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setGrid(recyclerView.getHeight());
                recyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        connectionStatusLocal = view.findViewById(R.id.connectionStatusLocal);

        actionButtonsLayout = view.findViewById(R.id.element_set_call_buttons);

        actionButtonsEnabled(false);
        setActionButtonsVisibility();
    }

    private void setActionButtonsVisibility() {
        if (asListenerRole) {
            setActionButtonsInvisible();
        }
    }

    protected void setActionButtonsInvisible() {
        micToggleCall.setVisibility(View.INVISIBLE);
    }

    private void setGrid(int recycleViewHeight) {
        ArrayList<ConnectycubeUser> users = new ArrayList<>();
        int itemHeight = recycleViewHeight / DISPLAY_ROW_AMOUNT;
        opponentsAdapter = new OpponentsFromCallAdapter(getActivity(), currentSession, users,
                (int) getResources().getDimension(R.dimen.item_width),
                itemHeight);
        opponentsAdapter.setAdapterListener(this);
        recyclerView.setAdapter(opponentsAdapter);
    }

    private void releaseViewHolders() {
        if (opponentViewHolders != null) {
            opponentViewHolders.clear();
        }
    }

    private void removeVideoTrackRenders() {
        Log.d(TAG, "removeVideoTrackRenders");
        Log.d(TAG, "remove opponents video Tracks");
        Map<Integer, RTCVideoTrack> videoTrackMap = getVideoTrackMap();
        for (RTCVideoTrack videoTrack : videoTrackMap.values()) {
            if (videoTrack.getRenderer() != null) {
                Log.d(TAG, "remove opponent video Tracks");
                videoTrack.removeRenderer(videoTrack.getRenderer());
            }
        }
    }

    private void releaseViews() {
        if (localVideoView != null) {
            localVideoView.release();
        }
        localVideoView = null;

        releaseOpponentsViews();
    }

    protected void initButtonsListener() {

        micToggleCall.setOnCheckedChangeListener((buttonView, isChecked) -> conversationFragmentCallbackListener.onSetAudioEnabled(isChecked));

        handUpCall.setOnClickListener(v -> {
            actionButtonsEnabled(false);
            handUpCall.setEnabled(false);
            handUpCall.setActivated(false);

            conversationFragmentCallbackListener.onLeaveCurrentSession();
            Log.d(TAG, "Call is stopped");
        });
    }

    protected void actionButtonsEnabled(boolean inability) {

        micToggleCall.setEnabled(inability);

        // inactivate toggle buttons
        micToggleCall.setActivated(inability);
    }

    private void hideOutgoingScreen() {
        outgoingOpponentsRelativeLayout.setVisibility(View.GONE);
    }

    @Override
    public void onCallStarted() {
        hideOutgoingScreen();
        actionButtonsEnabled(true);
    }

    private void initOpponentsList() {
        Log.v(TAG, "initOpponentsList() opponentsIds= " + opponentsIds);
        opponents = getUsersByIds(opponentsIds);
    }

    public ArrayList<ConnectycubeUser> getUsersByIds(List<Integer> usersIds) {
        ArrayList<ConnectycubeUser> users = new ArrayList<>();
        for (Integer userId : usersIds) {
            if (getUserById(userId) != null) {
                users.add(getUserById(userId));
            }
        }
        return users;
    }

    private ConnectycubeUser getUserById(int userID) {
        for (ConnectycubeUser user : allOpponents) {
            if (user.getId().equals(userID)) {
                return user;
            }
        }
        return null;
    }

    protected OpponentsFromCallAdapter.ViewHolder getViewHolderForOpponent(Integer userID) {
        OpponentsFromCallAdapter.ViewHolder holder = opponentViewHolders.get(userID);
        if (holder == null) {
            Log.d(TAG, "holder not found in cache");
            holder = findHolder(userID);
            if (holder != null) {
                opponentViewHolders.put(userID, holder);
            }
        }
        return holder;
    }

    private void setStatusForOpponent(int userId, final String status) {
        if (userId == currentUser.getId()) {
            return;
        }
        final OpponentsFromCallAdapter.ViewHolder holder = getViewHolderForOpponent(userId);
        if (holder == null) {
            return;
        }

        holder.setStatus(status);
    }

    protected void setStatusForCurrentUser(final String status) {
        connectionStatusLocal.setText(status);
    }

    protected void cleanUpAdapter(int userId) {
        Log.d(TAG, "onConnectionClosedForUser cleanUpAdapter userId= " + userId);
        OpponentsFromCallAdapter.ViewHolder itemHolder = getViewHolderForOpponent(userId);
        if (itemHolder != null) {
            if (itemHolder.getAdapterPosition() != -1) {
                Log.d(TAG, "onConnectionClosedForUser  opponentsAdapter.removeItem");
                opponentsAdapter.removeItem(itemHolder.getAdapterPosition());
                opponentViewHolders.remove(userId);
            }
        }
        updateActionBar(opponentsAdapter.getItemCount());
        recyclerView.requestLayout();
        getVideoTrackMap().remove(userId);
    }

    protected void addOpponentToDialog() {
        SelectUsersActivity.startForResult(this, REQUEST_ADD_OCCUPANTS, getChatDialog(currentSession.getDialogID()));
    }

    private ConnectycubeChatDialog getChatDialog(String dialogId) {
        ConnectycubeChatDialog chatDialog = new ConnectycubeChatDialog(dialogId);
        chatDialog.setType(ConnectycubeDialogType.GROUP);
        return chatDialog;
    }

    protected void cleanAdapterIfNeed() {
        if (!usersToDestroy.isEmpty()) {
            Iterator<Integer> iterator = usersToDestroy.iterator();
            while (iterator.hasNext()) {
                cleanUpAdapter(iterator.next());
                iterator.remove();
            }
        }
    }

    protected void setRecyclerViewVisibleState() {
        recyclerView.setVisibility(View.VISIBLE);
    }


    protected OpponentsFromCallAdapter.ViewHolder findHolder(Integer userID) {
        Log.d(TAG, "findHolder for " + userID);
        int childCount = recyclerView.getChildCount();
        Log.d(TAG, "findHolder for childCount= " + childCount);
        for (int i = 0; i < childCount; i++) {
            View childView = recyclerView.getChildAt(i);
            OpponentsFromCallAdapter.ViewHolder childViewHolder = (OpponentsFromCallAdapter.ViewHolder) recyclerView.getChildViewHolder(childView);
            Log.d(TAG, "childViewHolder.getUserId= " + childViewHolder.getUserId());
            if (userID.equals(childViewHolder.getUserId())) {
                Log.d(TAG, "return childViewHolder");
                return childViewHolder;
            }
        }
        return null;
    }

    private void setOpponentView(int userID) {
        setOpponentToAdapter(userID);
        if (!isRemoteShown) {
            isRemoteShown = true;
            setRecyclerViewVisibleState();
            setDuringCallActionBar();
        }
        updateActionBar(opponentsAdapter.getItemCount());
    }

    private boolean checkIfUserInAdapter(int userId) {
        for (ConnectycubeUser user : opponentsAdapter.getOpponents()) {
            if (user.getId() == userId) {
                return true;
            }
        }
        return false;
    }

    ///////////////////////////////  RTCSessionConnectionCallbacks ///////////////////////////

    @Override
    public void onConnectedToUser(ConferenceSession rtcSession, final Integer userId) {
        if (checkIfUserInAdapter(userId)) {
            setStatusForOpponent(userId, getString(R.string.text_status_connected));
            Log.d(TAG, "onConnectedToUser user already in, userId= " + userId);
            return;
        }
        setOpponentView(userId);

        mainHandler.postDelayed(() -> {
            setRemoteViewMultiCall(userId);

            setStatusForOpponent(userId, getString(R.string.text_status_connected));
            setProgressBarForOpponentGone(userId);
        }, LOCAL_TRACk_INITIALIZE_DELAY);
    }

    @Override
    public void onConnectionClosedForUser(ConferenceSession rtcSession, Integer userId) {
        Log.d(TAG, "onConnectionClosedForUser userId= " + userId);

        if (currentSession.isDestroyed()) {
            Log.d(TAG, "onConnectionClosedForUser isDestroyed userId= " + userId);
            return;
        }

        if (isNeedCleanUp) {
            setStatusForOpponent(userId, getString(R.string.text_status_closed));
            cleanUpAdapter(userId);
        } else {
            usersToDestroy.add(userId);
        }

    }

    @Override
    public void onDisconnectedFromUser(ConferenceSession rtcSession, Integer integer) {
        setStatusForOpponent(integer, getString(R.string.text_status_disconnected));
    }

    @Override
    public void onStateChanged(ConferenceSession session, BaseSession.RTCSessionState state) {

    }

    //////////////////////////////////   end     //////////////////////////////////////////


    protected Map<Integer, RTCVideoTrack> getVideoTrackMap() {
        if (videoTrackMap == null) {
            videoTrackMap = new HashMap<>();
        }
        return videoTrackMap;
    }


    @Override
    public void onLocalVideoTrackReceive(ConferenceSession session, RTCVideoTrack videoTrack) {
        Log.d(TAG, "onLocalVideoTrackReceive");
    }

    @Override
    public void onRemoteVideoTrackReceive(ConferenceSession session, final RTCVideoTrack videoTrack, final Integer userID) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userID);
        getVideoTrackMap().put(userID, videoTrack);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conversation_fragment, menu);
        if (asListenerRole) {
            MenuItem cameraSwitchItem = menu.findItem(R.id.camera_switch);
            cameraSwitchItem.setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    protected void setRemoteViewMultiCall(int userID) {
        if (currentSession.isDestroyed()) {
            Log.d(TAG, "setRemoteViewMultiCall currentSession.isDestroyed RETURN");
            return;
        }
        updateActionBar(opponentsAdapter.getItemCount());
        Log.d(TAG, "setRemoteViewMultiCall fillVideoView");

        final OpponentsFromCallAdapter.ViewHolder itemHolder = getViewHolderForOpponent(userID);
        if (itemHolder == null) {
            Log.d(TAG, "itemHolder == null - true");
            return;
        }
        final ConferenceSurfaceView remoteVideoView = itemHolder.getOpponentView();

        if (remoteVideoView != null) {
            remoteVideoView.setZOrderMediaOverlay(true);
            updateVideoView(remoteVideoView, false);
            Log.d(TAG, "onRemoteVideoTrackReceive fillVideoView");
            RTCVideoTrack remoteVideoTrack = getVideoTrackMap().get(userID);
            if (remoteVideoTrack != null) {
                fillVideoView(remoteVideoView, remoteVideoTrack, true);
            }
        }
    }

    protected void updateVideoView(SurfaceViewRenderer surfaceViewRenderer, boolean mirror) {
        updateVideoView(surfaceViewRenderer, mirror, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    }

    protected void updateVideoView(SurfaceViewRenderer surfaceViewRenderer, boolean mirror, RendererCommon.ScalingType scalingType) {
        Log.i(TAG, "updateVideoView mirror:" + mirror + ", scalingType = " + scalingType);
        surfaceViewRenderer.setScalingType(scalingType);
        surfaceViewRenderer.setMirror(mirror);
        surfaceViewRenderer.requestLayout();
    }

    protected void fillVideoView(ConferenceSurfaceView videoView, RTCVideoTrack videoTrack,
                                 boolean remoteRenderer) {
        videoTrack.removeRenderer(videoTrack.getRenderer());
        videoTrack.addRenderer(videoView);
        Log.d(TAG, (remoteRenderer ? "remote" : "local") + " Track is rendering");
    }

    private void initVideoTracksListener() {
        if (currentSession != null) {
            currentSession.addVideoTrackCallbacksListener(this);
        }
    }

    private void removeVideoTracksListener() {
        if (currentSession != null) {
            currentSession.removeVideoTrackCallbacksListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_opponent:
                Log.d(TAG, "add_opponent");
                addOpponentToDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GridManager extends GridLayoutManager {

        GridManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        @Override
        public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
            super.onItemsAdded(recyclerView, positionStart, itemCount);
            Log.d("GridManager", "onItemsAdded positionStart= " + positionStart);
        }

        @Override
        public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
            super.onItemsRemoved(recyclerView, positionStart, itemCount);
            Log.d("GridManager", "onItemsRemoved positionStart= " + positionStart);
            updateAdaptersItems();
        }

        private void updateAdaptersItems() {
            if (opponentsAdapter.getItemCount() > 0) {
                OpponentsFromCallAdapter.ViewHolder itemHolder = getViewHolderForOpponent(opponentsAdapter.getItem(0));
                if (itemHolder != null) {
                    itemHolder.itemView.requestLayout();
                }
            }
        }

        @Override
        public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount,
                                   Object payload) {
            super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
            Log.d("GridManager", "onItemsUpdated positionStart= " + positionStart);
        }

        @Override
        public void onItemsChanged(RecyclerView recyclerView) {
            super.onItemsChanged(recyclerView);
            Log.d("GridManager", "onItemsChanged");
        }

        @Override
        public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            Log.d("GridManager", "onLayoutCompleted");
        }
    }

    private class SpanSizeLookupImpl extends GridManager.SpanSizeLookup {


        @Override
        public int getSpanSize(int position) {
            int itemCount = opponentsAdapter.getItemCount();
            if (itemCount % 4 == 0) {
                return 3;
            }

            if (itemCount % 4 == 1) {
//              check last position
                if (position == itemCount - 1) {
                    return 12;
                }
            } else if (itemCount % 4 == 2) {
                if (position == itemCount - 1 || position == itemCount - 2) {
                    return 6;
                }
            } else if (itemCount % 4 == 3) {
                if (position == itemCount - 1 || position == itemCount - 2 || position == itemCount - 3) {
                    return 4;
                }
            }

            return 3;
        }
    }


    private class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        public DividerItemDecoration(@NonNull Context context, @DimenRes int dimensionDivider) {
            this.space = context.getResources().getDimensionPixelSize(dimensionDivider);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(space, space, space, space);
        }
    }
}