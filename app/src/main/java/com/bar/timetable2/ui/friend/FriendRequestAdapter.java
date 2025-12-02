package com.bar.timetable2.ui.friend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.FriendRequest;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request);
        void onReject(FriendRequest request);
    }

    private List<FriendRequest> items = new ArrayList<>();
    private final OnRequestActionListener listener;

    public FriendRequestAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<FriendRequest> newList) {
        if (newList == null) {
            items = new ArrayList<>();
        } else {
            items = new ArrayList<>(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FriendRequest req = items.get(position);
        holder.bind(req, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView tvRequestFrom;
        Button btnAccept;
        Button btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestFrom = itemView.findViewById(R.id.tvRequestFrom);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(FriendRequest request, OnRequestActionListener listener) {
            // 일단 fromUserId 그대로 표시 (나중에 닉네임으로 바꿔도 됨)
            String text = "보낸 사람: " + request.getFromUserId();
            tvRequestFrom.setText(text);

            btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(request);
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(request);
            });
        }
    }
}
