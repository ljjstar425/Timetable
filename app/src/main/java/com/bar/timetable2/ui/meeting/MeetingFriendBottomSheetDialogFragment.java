package com.bar.timetable2.ui.meeting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bar.timetable2.R;
import com.bar.timetable2.data.firebase.FirestoreClient;
import com.bar.timetable2.data.user.UserManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MeetingFriendBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public interface OnMeetingFriendsSelectedListener {
        void onFriendsSelected(List<String> userIds);
    }

    private OnMeetingFriendsSelectedListener listener;
    private MeetingFriendAdapter adapter;

    public static MeetingFriendBottomSheetDialogFragment newInstance() {
        return new MeetingFriendBottomSheetDialogFragment();
    }

    public void setOnMeetingFriendsSelectedListener(OnMeetingFriendsSelectedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_meeting_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvFriends);
        Button btnApply = view.findViewById(R.id.btnApply);

        adapter = new MeetingFriendAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        loadFriends();

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                List<String> selected = adapter.getSelectedUserIds();
                listener.onFriendsSelected(selected);
            }
            dismiss();
        });
    }

    private void loadFriends() {
        String myId = UserManager.getInstance().getCurrentUserId();
        if (myId == null || myId.isEmpty()) return;

        FirestoreClient.getInstance()
                .getFriendsCollectionOf(myId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return;

                    List<MeetingFriendAdapter.FriendItem> list = new ArrayList<>();
                    for (DocumentSnapshot ds : task.getResult().getDocuments()) {
                        MeetingFriendAdapter.FriendItem item = new MeetingFriendAdapter.FriendItem();
                        item.userId = ds.getId();  // friends/{friendId}
                        Object nameObj = ds.get("displayName");
                        if (nameObj != null) {
                            item.displayName = String.valueOf(nameObj);
                        } else {
                            item.displayName = item.userId;
                        }
                        item.selected = false;
                        list.add(item);
                    }
                    adapter.setItems(list);
                });

    }
}
