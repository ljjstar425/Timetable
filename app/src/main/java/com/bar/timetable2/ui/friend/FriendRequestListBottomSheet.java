package com.bar.timetable2.ui.friend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.FriendRequest;
import com.bar.timetable2.data.repository.FriendRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class FriendRequestListBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvFriendRequests;
    private FriendRequestAdapter adapter;
    private ListenerRegistration requestListener;

    public FriendRequestListBottomSheet() {
        // 기본 생성자 필요
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_requests_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFriendRequests = view.findViewById(R.id.rvFriendRequests);
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FriendRequestAdapter(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest request) {
                FriendRepository.getInstance()
                        .acceptFriendRequest(request, new FriendRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "친구 요청을 수락했습니다.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(String message) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "수락 실패: " + message,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

            @Override
            public void onReject(FriendRequest request) {
                FriendRepository.getInstance()
                        .rejectFriendRequest(request, new FriendRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "친구 요청을 거절했습니다.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(String message) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "거절 실패: " + message,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        rvFriendRequests.setAdapter(adapter);

        // 🔥 내가 받은 pending 요청 실시간 listen
        requestListener = FriendRepository.getInstance()
                .listenIncomingRequests(new FriendRepository.FriendRequestListListener() {
                    @Override
                    public void onChanged(List<FriendRequest> requests) {
                        adapter.submitList(requests);
                    }

                    @Override
                    public void onError(String message) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "요청 목록 불러오기 실패: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
    }
}
