package com.bar.timetable2.ui.friend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.Friend;
import com.bar.timetable2.data.repository.FriendRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class FriendListBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvFriends;
    private ImageButton btnAddFriend;
    private ImageButton btnFriendRequests;

    private FriendAdapter adapter;
    private ListenerRegistration friendsListener;

    public FriendListBottomSheet() {
        // 기본 생성자
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFriends = view.findViewById(R.id.rvFriends);
        btnAddFriend = view.findViewById(R.id.btnAddFriend);
        btnFriendRequests = view.findViewById(R.id.btnFriendRequests);

        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendAdapter(friend -> {
            // TODO: 여기서 친구 클릭 시 친구 시간표 보기로 이동하거나
            //       콜백을 MyTimetableFragment 쪽으로 넘겨도 됨.
            Toast.makeText(getContext(),
                    "친구 클릭: " + friend.getFriendId(),
                    Toast.LENGTH_SHORT).show();
        });
        rvFriends.setAdapter(adapter);

        // 친구 목록 실시간 listen
        friendsListener = FriendRepository.getInstance()
                .listenFriends(new FriendRepository.FriendListListener() {
                    @Override
                    public void onChanged(List<Friend> friends) {
                        adapter.submitList(friends);
                    }

                    @Override
                    public void onError(String message) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "친구 목록 불러오기 실패: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 친구 추가 버튼
        btnAddFriend.setOnClickListener(v -> {
            AddFriendDialogFragment dialog = new AddFriendDialogFragment();
            dialog.show(getParentFragmentManager(), "AddFriendDialog");
        });

        // 친구 요청 목록 버튼 (지금은 임시 Toast)
        btnFriendRequests.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "친구 요청 목록은 다음 단계에서 구현할게요!",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (friendsListener != null) {
            friendsListener.remove();
            friendsListener = null;
        }
    }
}
