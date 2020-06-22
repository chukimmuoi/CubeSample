package com.connectycube.sample.conference.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.sample.conference.R;
import com.connectycube.sample.conference.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class DialogsAdapter extends BaseListAdapter<ConnectycubeChatDialog> {
    private List<ConnectycubeChatDialog> selectedItems;

    public DialogsAdapter(Context context, List<ConnectycubeChatDialog> dialogs) {
        super(context, dialogs);
        selectedItems = new ArrayList<>();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_dialog, parent, false);

            holder = new ViewHolder();
            holder.rootLayout = convertView.findViewById(R.id.root);
            holder.nameTextView = convertView.findViewById(R.id.text_dialog_name);
            holder.dialogImageView = convertView.findViewById(R.id.image_dialog_icon);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ConnectycubeChatDialog dialog = getItem(position);
        holder.dialogImageView.setBackgroundDrawable(UiUtils.getGreyCircleDrawable());

        holder.nameTextView.setText(dialog.getName());

        holder.rootLayout.setBackgroundColor(isItemSelected(position) ? UiUtils.getColor(R.color.selected_list_item_color) :
                UiUtils.getColor(android.R.color.transparent));

        return convertView;
    }

    public void selectItem(ConnectycubeChatDialog item) {
        if (selectedItems.contains(item)) {
            return;
        }
        selectedItems.add(item);
        notifyDataSetChanged();
    }

    public Collection<ConnectycubeChatDialog> getSelectedItems() {
        return selectedItems;
    }

    protected boolean isItemSelected(int position) {
        return !selectedItems.isEmpty() && selectedItems.contains(getItem(position));
    }

    public void toggleOneItem(ConnectycubeChatDialog item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.clear();
            selectedItems.add(item);
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(ConnectycubeChatDialog item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ViewGroup rootLayout;
        ImageView dialogImageView;
        TextView nameTextView;
    }
}
