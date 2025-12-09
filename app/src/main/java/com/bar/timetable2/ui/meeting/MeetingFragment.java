package com.bar.timetable2.ui.meeting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.data.repository.TimetableRepository;
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.google.firebase.firestore.ListenerRegistration;
import com.bar.timetable2.ui.timetable.TimetableViewModel;
import com.bar.timetable2.ui.timetable.view.TimetableView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingFragment extends Fragment {

    private TimetableViewModel timetableViewModel;
    private TimetableView timetableView;
    // 🔥 추가
    private TimetableRepository timetableRepository = TimetableRepository.getInstance();

    // 내 시간표 상태
    private TimetableState myState;

    // 선택된 친구들 ID
    private final List<String> selectedFriendIds = new ArrayList<>();

    // 각 친구의 시간표 상태 저장
    private final Map<String, TimetableState> friendStates = new HashMap<>();

    // 친구들의 listen 등록을 모아놓는 리스트 (나중에 제거용)
    private final List<ListenerRegistration> friendRegistrations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meeting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btnBackMeeting);
        Button btnSelectFriends = view.findViewById(R.id.btnSelectFriends);
        timetableView = view.findViewById(R.id.timetableViewMeeting);

        // 뒤로가기
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // 친구 선택 바텀시트 띄울 예정 – 지금은 그냥 토스트/로그 정도만
        btnSelectFriends.setOnClickListener(v -> {
            MeetingFriendBottomSheetDialogFragment sheet =
                    MeetingFriendBottomSheetDialogFragment.newInstance();

            sheet.setOnMeetingFriendsSelectedListener(userIds -> {
                // 선택된 친구들 ID 저장
                selectedFriendIds.clear();
                if (userIds != null) {
                    selectedFriendIds.addAll(userIds);
                }

                // 이전 리스너들 제거
                clearFriendRegistrations();
                friendStates.clear();

                // 새로 선택된 친구들 시간표 listen 시작
                for (String friendId : selectedFriendIds) {
                    ListenerRegistration reg = timetableRepository.listenTimetableOf(
                            friendId,
                            new TimetableRepository.TimetableStateListener() {
                                @Override
                                public void onChanged(TimetableState state) {
                                    friendStates.put(friendId, state);
                                    recomputeAndRender();
                                }

                                @Override
                                public void onError(Exception e) {
                                    e.printStackTrace();
                                }
                            }
                    );
                    if (reg != null) {
                        friendRegistrations.add(reg);
                    }
                }

                // 친구를 하나도 선택 안 한 경우 → 내 시간표만 기준으로 그리기
                recomputeAndRender();
            });

            sheet.show(getParentFragmentManager(), "MeetingFriends");
        });

        // ViewModel – MyTimetable에서 쓰던 거 그대로 재사용
        timetableViewModel = new ViewModelProvider(requireActivity())
                .get(TimetableViewModel.class);

        // 내 시간표 상태 observe → Meeting 화면에서도 동일하게 그려줌
        timetableViewModel.getTimetableState().observe(
                getViewLifecycleOwner(),
                state -> {
                    // 내 시간표 상태 저장
                    myState = state;
                    // 선택된 친구 상태와 합쳐서 그리기
                    recomputeAndRender();

                }
        );
    }

    private void clearFriendRegistrations() {
        for (ListenerRegistration reg : friendRegistrations) {
            if (reg != null) {
                reg.remove();
            }
        }
        friendRegistrations.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearFriendRegistrations();
    }

    private void recomputeAndRender() {
        // 1) 내 슬롯 + 친구 슬롯들을 전부 합친 리스트 만들기
        List<ClassSlot> mergedSlots = new ArrayList<>();

        if (myState != null && myState.getSlots() != null) {
            mergedSlots.addAll(myState.getSlots());
        }

        for (String friendId : selectedFriendIds) {
            TimetableState fs = friendStates.get(friendId);
            if (fs != null && fs.getSlots() != null) {
                mergedSlots.addAll(fs.getSlots());
            }
        }

        // 2) 과목 색은 Meeting에서는 전부 같은 색 / 텍스트 없어도 되니까
        //    courseMap은 비워두고, slots만 채운 TimetableState 생성
        Map<String, Course> emptyMap = new HashMap<>();

        TimetableState mergedState = new TimetableState(emptyMap, mergedSlots);

        // 3) TimetableView에 설정
        timetableView.setTimetableState(mergedState);
    }


}
