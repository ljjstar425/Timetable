package com.bar.timetable2.ui.friend;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.data.repository.FriendRepository;
import com.bar.timetable2.data.repository.TimetableRepository;
import com.bar.timetable2.ui.timetable.view.TimetableView;
import com.google.firebase.firestore.ListenerRegistration;

public class FriendTimetableFragment extends Fragment {

    private static final String ARG_FRIEND_ID = "friend_id";
    private static final String ARG_FRIEND_NAME = "friend_name";

    private String friendId;
    private String friendName;

    private TimetableView timetableView;
    private ListenerRegistration friendTimetableListener;

    public static FriendTimetableFragment newInstance(String friendId, String friendName) {
        FriendTimetableFragment f = new FriendTimetableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRIEND_ID, friendId);
        args.putString(ARG_FRIEND_NAME, friendName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            friendId = getArguments().getString(ARG_FRIEND_ID);
            friendName = getArguments().getString(ARG_FRIEND_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btnBackFriend);
        ImageButton btnDeleteFriend = view.findViewById(R.id.btnDeleteFriend);
        TextView tvTitle = view.findViewById(R.id.tvFriendName);
        timetableView = view.findViewById(R.id.timetableView);

        // 제목: "OOO 시간표" 느낌
        if (friendName != null && !friendName.isEmpty()) {
            tvTitle.setText(friendName + " 시간표");
        } else {
            tvTitle.setText(friendId + " 시간표");
        }

        // 뒤로가기: 그냥 backstack pop
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // 점3개: 친구 삭제
        btnDeleteFriend.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("친구 삭제")
                    .setMessage("이 친구를 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (d, which) -> {
                        FriendRepository.getInstance()
                                .removeFriend(friendId, new FriendRepository.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(),
                                                    "친구를 삭제했습니다.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        if (getParentFragmentManager() != null) {
                                            getParentFragmentManager().popBackStack();
                                        }
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(),
                                                    "삭제 실패: " + message,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    })
                    .setNegativeButton("취소", null)
                    .create();

            // 다이얼로그 표시 후 버튼 색상 변경
            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                if (positiveButton != null) {
                    positiveButton.setTextColor(Color.parseColor("#3182F7"));
                }
                if (negativeButton != null) {
                    negativeButton.setTextColor(Color.parseColor("#3182F7"));
                }
            });

            dialog.show();
        });

        // 친구 시간표 listen
        friendTimetableListener = TimetableRepository.getInstance()
                .listenTimetableOf(friendId, new TimetableRepository.TimetableStateListener() {
                    @Override
                    public void onChanged(TimetableState state) {
                        timetableView.setTimetableState(state);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() != null) {
                            String msg = (e != null && e.getMessage() != null)
                                    ? e.getMessage()
                                    : "알 수 없는 오류";
                            Toast.makeText(getContext(),
                                    "친구 시간표 로드 실패: " + msg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // (필요하면 슬롯 클릭 콜백도 등록 가능: 친구 수업 상세 보기 등)
        // timetableView.setOnSlotClickListener(...);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (friendTimetableListener != null) {
            friendTimetableListener.remove();
            friendTimetableListener = null;
        }
    }
}