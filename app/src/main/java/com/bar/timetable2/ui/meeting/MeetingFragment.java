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
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.data.repository.TimetableRepository;
import com.bar.timetable2.ui.timetable.TimetableViewModel;
import com.bar.timetable2.ui.timetable.view.TimetableView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingFragment extends Fragment {

    private TimetableViewModel timetableViewModel;
    private TimetableView timetableView;

    private final TimetableRepository timetableRepository = TimetableRepository.getInstance();

    // 내 시간표 상태
    private TimetableState myState;

    // 선택된 친구들 ID
    private final List<String> selectedFriendIds = new ArrayList<>();

    // 각 친구의 시간표 상태 저장
    private final Map<String, TimetableState> friendStates = new HashMap<>();

    // 친구들의 snapshot 리스너들
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

        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // 친구 선택 바텀시트
        btnSelectFriends.setOnClickListener(v -> {
            MeetingFriendBottomSheetDialogFragment sheet =
                    MeetingFriendBottomSheetDialogFragment.newInstance();

            sheet.setOnMeetingFriendsSelectedListener(userIds -> {
                selectedFriendIds.clear();
                if (userIds != null) {
                    selectedFriendIds.addAll(userIds);
                }

                clearFriendRegistrations();
                friendStates.clear();

                // 선택된 친구들의 시간표 listen
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

                // 친구 아무도 선택 안 했을 때도 내 공강은 계속 보이게
                recomputeAndRender();
            });

            sheet.show(getParentFragmentManager(), "MeetingFriends");
        });

        // ViewModel – MyTimetable과 공유
        timetableViewModel = new ViewModelProvider(requireActivity())
                .get(TimetableViewModel.class);

        // 내 시간표 상태 observe
        timetableViewModel.getTimetableState().observe(
                getViewLifecycleOwner(),
                state -> {
                    myState = state;
                    recomputeAndRender();
                }
        );
    }

    private void clearFriendRegistrations() {
        for (ListenerRegistration reg : friendRegistrations) {
            if (reg != null) reg.remove();
        }
        friendRegistrations.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearFriendRegistrations();
    }

    /**
     * 1) TimetableView에는 "내 시간표"만 블록으로 그림
     * 2) 나 + 친구들의 공강을 FreeTimeBlock 리스트로 생성해서
     *    setFreeTimeBlocks()로 반투명 레이어를 덧칠
     */
    private void recomputeAndRender() {
        if (timetableView == null) return;

        // ----- 1) 기본 시간표: 내 시간표만 그림 -----
        timetableView.setTimetableState(null);


        // ----- 2) 공강 계산용 데이터 준비 -----
        // 내 슬롯
        List<ClassSlot> mySlots = new ArrayList<>();
        if (myState != null && myState.getSlots() != null) {
            mySlots.addAll(myState.getSlots());
        }

        // 친구별 슬롯
        Map<String, List<ClassSlot>> friendsSlotsByUser = new HashMap<>();
        for (String friendId : selectedFriendIds) {
            TimetableState fs = friendStates.get(friendId);
            if (fs != null && fs.getSlots() != null && !fs.getSlots().isEmpty()) {
                friendsSlotsByUser.put(friendId, new ArrayList<>(fs.getSlots()));
            }
        }

        // ----- 3) 나 + 친구들 공강 오버레이 계산 -----
        List<TimetableView.FreeTimeBlock> freeBlocks =
                MeetingUtils.buildFreeBlocksForAllParticipants(mySlots, friendsSlotsByUser);

        // ----- 4) TimetableView에 적용 -----
        timetableView.setFreeTimeBlocks(freeBlocks);
    }
}
