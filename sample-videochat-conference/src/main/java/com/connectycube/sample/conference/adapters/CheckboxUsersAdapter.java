package com.connectycube.sample.conference.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.utils.UiUtils;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.List;


public class CheckboxUsersAdapter extends BaseListAdapter<ConnectycubeUser> {
    private ConnectycubeUser currentUser;

    private List<Integer> initiallySelectedUsers;
    private List<ConnectycubeUser> selectedUsers;

    public CheckboxUsersAdapter(Context context, List<ConnectycubeUser> users, ConnectycubeUser currentUser) {
        super(context, users);
        this.currentUser = currentUser;
        selectedUsers = new ArrayList<>();
        selectedUsers.add(currentUser);

        initiallySelectedUsers = new ArrayList<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ConnectycubeUser user = getItem(position);

        final ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_user, parent, false);
            holder = new ViewHolder();
            holder.userImageView = convertView.findViewById(R.id.image_user);
            holder.loginTextView = convertView.findViewById(R.id.text_user_login);
            holder.userCheckBox = convertView.findViewById(R.id.checkbox_user);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (isUserMe(user)) {
            holder.loginTextView.setText(context.getString(R.string.placeholder_username_you, user.getFullName()));
        } else {
            holder.loginTextView.setText(user.getFullName());
        }

        if (isAvailableForSelection(user)) {
            holder.loginTextView.setTextColor(UiUtils.getColor(R.color.text_color_black));
        } else {
            holder.loginTextView.setTextColor(UiUtils.getColor(R.color.text_color_medium_grey));
        }

        holder.userImageView.setBackgroundDrawable(UiUtils.getColorCircleDrawable(position));
        holder.userCheckBox.setVisibility(View.GONE);

        convertView.setOnClickListener(v -> {
            if (!isAvailableForSelection(user)) {
                return;
            }

            holder.userCheckBox.setChecked(!holder.userCheckBox.isChecked());
            if (holder.userCheckBox.isChecked()) {
                selectedUsers.add(user);
            } else {
                selectedUsers.remove(user);
            }
        });

        holder.userCheckBox.setVisibility(View.VISIBLE);
        holder.userCheckBox.setChecked(selectedUsers.contains(user));

        return convertView;
    }

    protected boolean isUserMe(ConnectycubeUser user) {
        return currentUser != null && currentUser.getId().equals(user.getId());
    }

    public List<ConnectycubeUser> getSelectedUsers() {
        return selectedUsers;
    }

    protected boolean isAvailableForSelection(ConnectycubeUser user) {
        return currentUser == null || !currentUser.getId().equals(user.getId()) && !initiallySelectedUsers.contains(user.getId());
    }

    protected static class ViewHolder {
        ImageView userImageView;
        TextView loginTextView;
        CheckBox userCheckBox;
    }
}
