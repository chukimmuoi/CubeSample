package com.connectycube.sample.conference.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.utils.RTCSessionUtils;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.conference.ConferencePeerConnection;
import com.connectycube.videochat.conference.ConferenceSession;
import com.connectycube.videochat.conference.view.ConferenceSurfaceView;

import java.util.List;


public class OpponentsFromCallAdapter extends RecyclerView.Adapter<OpponentsFromCallAdapter.ViewHolder> {

    private static final String TAG = OpponentsFromCallAdapter.class.getSimpleName();
    private final int itemHeight;
    private final int itemWidth;

    private Context context;
    private ConferenceSession session;
    private List<ConnectycubeUser> opponents;
    private LayoutInflater inflater;
    private OnAdapterEventListener adapterListener;


    public OpponentsFromCallAdapter(Context context, ConferenceSession session, List<ConnectycubeUser> users, int width, int height) {
        this.context = context;
        this.session = session;
        this.opponents = users;
        this.inflater = LayoutInflater.from(context);
        itemWidth = width;
        itemHeight = height;
        Log.d(TAG, "item width=" + itemWidth + ", item height=" + itemHeight);
    }

    public void setAdapterListener(OnAdapterEventListener adapterListener) {
        this.adapterListener = adapterListener;
    }

    @Override
    public int getItemCount() {
        return opponents.size();
    }

    public Integer getItem(int position) {
        return opponents.get(position).getId();
    }

    public List<ConnectycubeUser> getOpponents() {
        return opponents;
    }

    public void removeItem(int index) {
        opponents.remove(index);
        notifyItemRemoved(index);
        notifyItemRangeChanged(index, opponents.size());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.list_item_opponent_from_call, null);
        v.findViewById(R.id.innerLayout).setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, itemHeight));

        final ViewHolder vh = new ViewHolder(v);
        vh.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                adapterListener.onToggleButtonItemClick(vh.getAdapterPosition(), isChecked);
            }
        });
        vh.showOpponentView(true);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ConnectycubeUser user = opponents.get(position);
        int userID = user.getId();
        holder.opponentsName.setText(user.getFullName());

        if (session.getMediaStreamManager() != null) {
            holder.toggleButton.setChecked(session.getMediaStreamManager().getAudioTrack(userID).enabled());
        }

        holder.getOpponentView().setId(user.getId());
        holder.setUserId(userID);
        ConferencePeerConnection peerConnection = session.getPeerConnection(userID);
        if (peerConnection != null) {
            RTCTypes.RTCConnectionState state = peerConnection.getState();
            Log.d(TAG, "state ordinal= " + state.ordinal());
            holder.setStatus(context.getResources().getString(RTCSessionUtils.getStatusDescriptionResource(state)));
        }
    }

    public void add(ConnectycubeUser item) {
        opponents.add(item);
        notifyItemRangeChanged((opponents.size() - 1), opponents.size());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public interface OnAdapterEventListener {
        void onToggleButtonItemClick(int position, boolean isChecked);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ToggleButton toggleButton;
        TextView opponentsName;
        TextView connectionStatus;
        ConferenceSurfaceView opponentView;
        ProgressBar progressBar;
        private int userId;

        public ViewHolder(View itemView) {
            super(itemView);
            toggleButton = itemView.findViewById(R.id.opponent_toggle_mic);
            opponentsName = itemView.findViewById(R.id.opponentName);
            connectionStatus = itemView.findViewById(R.id.connectionStatus);
            opponentView = itemView.findViewById(R.id.opponentView);
            progressBar = itemView.findViewById(R.id.progress_bar_adapter);
        }

        public void setStatus(String status) {
            connectionStatus.setText(status);
        }

        public void setUserName(String userName) {
            opponentsName.setText(userName);
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public int getUserId() {
            return userId;
        }

        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public ConferenceSurfaceView getOpponentView() {
            return opponentView;
        }

        public void showOpponentView(boolean show) {
            Log.d(TAG, "show? " + show);
            opponentView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
