package com.bar.timetable2.ui.meeting;

import android.content.pm.LauncherActivityInfo;
import android.hardware.camera2.CameraExtensionSession;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bar.timetable2.R;
import com.bar.timetable2.ui.friend.FriendAdapter;
import com.bar.timetable2.ui.friend.FriendTimetableFragment;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class MeetingFriendAdapter extends RecyclerView.Adapter<MeetingFriendAdapter.FriendViewHolder> {
    public static class FriendItem {
        public String userId;
        public String displayName;
        public boolean selected;
    }

    private final List<FriendItem> items = new ArrayList<>();

    public void setItems(List<FriendItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public List<String> getSelectedUserIds() {
        List<String> result = new ArrayList<>();
        for (FriendItem f : items) {
            if (f.selected) {
                result.add(f.userId);
            }
        }
        return result;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meeting_friend, parent, false);
        return new FriendViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@Nonnull FriendViewHolder holder, int position) {
        FriendItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkFriend;
        TextView tvFriendName;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);

            checkFriend = (CheckBox) itemView.findViewById(R.id.checkFriend);
            tvFriendName = (TextView) itemView.findViewById(R.id.tvFriendName);
        }

        void bind(FriendItem item) {
            tvFriendName.setText(item.displayName != null ? item.displayName : item.userId);
            checkFriend.setChecked(item.selected);

            View.OnClickListener toggle = v -> {
                item.selected = ! item.selected;
                checkFriend.setChecked(item.selected);
            };

            itemView.setOnClickListener(toggle);
            checkFriend.setOnClickListener(toggle);
        }
    }
}
