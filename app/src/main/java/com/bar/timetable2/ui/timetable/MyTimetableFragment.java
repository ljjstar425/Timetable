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

        // 🔥 Firestore
        viewModel.startListenMyTimetable();
    }

    private void onTimetableStateChanged(@Nullable TimetableState state) {
        // null일 수도 있으니 그대로 넘기고, TimetableView에서 처리
        timetableView.setTimetableState(state);
    }
}
