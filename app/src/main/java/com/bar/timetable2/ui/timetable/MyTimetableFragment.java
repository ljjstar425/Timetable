package com.bar.timetable2.ui.timetable;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.ui.timetable.view.TimetableView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyTimetableFragment extends Fragment {

    private TimetableViewModel viewModel;
    private TimetableView timetableView;

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

        viewModel = new ViewModelProvider(this).get(TimetableViewModel.class);

        // LiveData observe: 바뀔 때마다 TimetableView에 전달
        viewModel.getTimetableState().observe(
                getViewLifecycleOwner(),
                this::onTimetableStateChanged
        );

        // 🔥 Firestore 일단 끄기
        // viewModel.startListenMyTimetable();

        // 🔥 더미로 직접 테스트
        testDummyBlock();
    }

    private void onTimetableStateChanged(@Nullable TimetableState state) {
        // null일 수도 있으니 그대로 넘기고, TimetableView에서 처리
        timetableView.setTimetableState(state);
    }

    // 체크용
    private void testDummyBlock() {
        Log.e("TT-FRAG", "testDummyBlock() 호출됨");

        // 과목 하나
        Course c = new Course();
        c.setId("c1");
        c.setName("자료구조");
        c.setColorHex("#FF7043");

        Map<String, Course> courseMap = new HashMap<>();
        courseMap.put(c.getId(), c);

        // 월요일 9:00~10:30 슬롯 하나
        ClassSlot slot = new ClassSlot();
        slot.setCourseId("c1");
        slot.setDayOfWeek(1);           // 월
        slot.setStartMin(9 * 60);       // 540
        slot.setEndMin(10 * 60 + 30);   // 630

        List<ClassSlot> slots = new ArrayList<>();
        slots.add(slot);

        TimetableState dummy = new TimetableState(courseMap, slots);
        timetableView.setTimetableState(dummy);
    }

}
