package com.bar.timetable2.ui.friend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.Friend;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    private List<Friend> items = new ArrayList<>();
    private final OnFriendClickListener listener;

    public FriendAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Friend> newList) {
        if (newList == null) {
            items = new ArrayList<>();
        } else {
            items = new ArrayList<>(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = items.get(position);
        holder.bind(friend, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {

        TextView tvFriendName;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
        }

        public void bind(Friend friend, OnFriendClickListener listener) {
            String name = friend.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = friend.getFriendId();
            }
            tvFriendName.setText(name);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFriendClick(friend);
                }
            });
        }
    }
}
