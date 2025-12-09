package com.bar.timetable2.ui.timetable;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.bar.timetable2.data.model.Friend;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.data.repository.FriendRepository;
import com.bar.timetable2.ui.friend.AddFriendDialogFragment;
import com.bar.timetable2.ui.friend.FriendAdapter;
import com.bar.timetable2.ui.friend.FriendListBottomSheet;
import com.bar.timetable2.ui.friend.FriendRequestListBottomSheet;
import com.bar.timetable2.ui.friend.FriendTimetableFragment;
import com.bar.timetable2.ui.meeting.MeetingFragment;
import com.bar.timetable2.ui.timetable.view.TimetableView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;


public class MyTimetableFragment extends Fragment {

    private TimetableViewModel viewModel;
    private TimetableView timetableView;
    private TimetableState currentState;
    private BottomSheetBehavior<View> friendBottomSheetBehavior;
    private RecyclerView rvFriends;
    private FriendAdapter friendAdapter;
    private ListenerRegistration friendsListener;

    public MyTimetableFragment() {
        // 기본 생성자
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timetableView = view.findViewById(R.id.timetableView);

        // 바텀시트 설정
        View friendBottomSheet = view.findViewById(R.id.friendBottomSheet);
        friendBottomSheetBehavior = BottomSheetBehavior.from(friendBottomSheet);
        friendBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        friendBottomSheetBehavior.setPeekHeight(80); // 검색창이 보이는 높이
        friendBottomSheetBehavior.setHideable(false);

        // RecyclerView 설정
        rvFriends = view.findViewById(R.id.rvFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        // 친구 목록 어댑터 설정 (FriendAdapter는 별도로 구현 필요)
        friendAdapter = new FriendAdapter(friend -> {
            // 친구 클릭 시 친구 시간표 Fragment로 이동
            FriendTimetableFragment fragment =
                    FriendTimetableFragment.newInstance(
                            friend.getFriendId(),
                            friend.getDisplayName()
                    );

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvFriends.setAdapter(friendAdapter);

        // 친구 목록 실시간 listen
        friendsListener = FriendRepository.getInstance()
                .listenFriends(new FriendRepository.FriendListListener() {
                    @Override
                    public void onChanged(List<Friend> friends) {
                        friendAdapter.submitList(friends);
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

        timetableView.setOnSlotClickListener(slot -> {
            Course course = null;
            if (currentState != null && currentState.getCourseMap() != null && slot != null) {
                Map<String, Course> map = currentState.getCourseMap();
                if (map != null && slot.getCourseId() != null) {
                    course = map.get(slot.getCourseId());
                }
            }
            showSlotBottomSheet(slot, course);
        });

        viewModel = new ViewModelProvider(requireActivity()).get(TimetableViewModel.class);

        // LiveData observe: 바뀔 때마다 TimetableView에 전달
        viewModel.getTimetableState().observe(
                getViewLifecycleOwner(),
                state -> {
                    currentState = state;  // 현재 상태 저장

                    int size = (state != null && state.getSlots() != null)
                            ? state.getSlots().size() : 0;
                    Log.e("TT-FRAG", "onTimetableStateChanged 호출, slots = " + size);
                    timetableView.setTimetableState(state);
                }
        );

        // 삭제 결과 알림
        viewModel.getDeleteClassResult().observe(
                getViewLifecycleOwner(),
                result -> {
                    if (result == null) return;
                    if (result.success) {
                        Toast.makeText(getContext(), "수업이 삭제되었어요.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "삭제 실패: " + result.errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 친구 추가 요청 버튼
        ImageButton btnAddFriend = view.findViewById(R.id.btnAddFriend);
        btnAddFriend.setOnClickListener(v -> {
            AddFriendDialogFragment dialog = new AddFriendDialogFragment();
            dialog.show(getParentFragmentManager(), "AddFriendDialog");
        });

        // 친구 요청 목록 버튼
        ImageButton btnFriendRequests = view.findViewById(R.id.btnFriendRequests);
        btnFriendRequests.setOnClickListener(v -> {
            FriendRequestListBottomSheet sheet = new FriendRequestListBottomSheet();
            sheet.show(getParentFragmentManager(), "FriendRequestListBottomSheet");
        });


        // 🔥 Firestore
        viewModel.startListenMyTimetable();

        // + 아이콘 클릭 -> AddClass 화면으로 이동
        ImageButton btnAdd = view.findViewById(R.id.btnAddClass);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new AddClassFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // 사람들 아이콘 클릭 -> Meeting 화면으로 이동
        ImageButton btnMeeting = view.findViewById(R.id.btnMeeting);
        btnMeeting.setOnClickListener(v -> {
            MeetingFragment fragment = new MeetingFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void onTimetableStateChanged(@Nullable TimetableState state) {
        // null일 수도 있으니 그대로 넘기고, TimetableView에서 처리
        timetableView.setTimetableState(state);
    }

    // 수업 정보 바텀시트 띄우는 메서드
    private void showSlotBottomSheet(ClassSlot slot, Course course) {
        if (getContext() == null || slot == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottomsheet_class_detail, null);
        dialog.setContentView(sheet);

        TextView tvName = sheet.findViewById(R.id.tvClassName);
        TextView tvTime = sheet.findViewById(R.id.tvClassTime);
        TextView tvLocation = sheet.findViewById(R.id.tvClassLocation);
        Button btnDelete = sheet.findViewById(R.id.btnDeleteClass);
        Button btnClose = sheet.findViewById(R.id.btnClose);

        String name = (course != null && course.getName() != null)
                ? course.getName() : "(이름 없음)";
        tvName.setText(name);

        String dayText = dayOfWeekToText(slot.getDayOfWeek());
        String timeText = String.format("%s %s ~ %s",
                dayText,
                minutesToTime(slot.getStartMin()),
                minutesToTime(slot.getEndMin()));
        tvTime.setText(timeText);

        // Course에 location 필드가 있다면 사용, 없으면 생략
        String loc = "";
        try {
            loc = course.getLocation();
        } catch (Exception ignored) {}
        if (loc == null || loc.isEmpty()) loc = "(강의실 정보 없음)";
        tvLocation.setText(loc);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            if (course == null || course.getId() == null) {
                dialog.dismiss();
                return;
            }
            // 삭제 확인 다이얼로그
            new AlertDialog.Builder(requireContext())
                    .setTitle("수업 삭제")
                    .setMessage("'" + name + "' 수업을 모두 삭제할까요?")
                    .setPositiveButton("삭제", (d, which) -> {
                        viewModel.deleteClass(course.getId());
                        dialog.dismiss();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        dialog.show();
    }

    private String dayOfWeekToText(int day) {
        switch (day) {
            case 1: return "월";
            case 2: return "화";
            case 3: return "수";
            case 4: return "목";
            case 5: return "금";
            case 6: return "토";
            case 7: return "일";
            default: return "";
        }
    }

    private String minutesToTime(int min) {
        int h = min / 60;
        int m = min % 60;
        return String.format("%02d:%02d", h, m);
    }

}
