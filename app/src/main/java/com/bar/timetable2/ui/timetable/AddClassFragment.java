package com.bar.timetable2.ui.timetable;

import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bar.timetable2.R;
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddClassFragment extends Fragment {

    private TimetableViewModel viewModel;

    private EditText etCourseName;
    private EditText etLocation;
    private Spinner spinnerDayOfWeek;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private Button btnSave;
    private ImageButton btnBack;

    private int startMin = -1; // 분 단위 (0~1440)
    private int endMin = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_class, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 같은 Activity에서 MyTimetableFragment와 ViewModel 공유
        viewModel = new ViewModelProvider(requireActivity()).get(TimetableViewModel.class);

        etCourseName = view.findViewById(R.id.etCourseName);
        etLocation = view.findViewById(R.id.etLocation);
        spinnerDayOfWeek = view.findViewById(R.id.spinnerDayOfWeek);
        tvStartTime = view.findViewById(R.id.tvStartTime);
        tvEndTime = view.findViewById(R.id.tvEndTime);
        btnSave = view.findViewById(R.id.btnSaveClass);
        btnBack = view.findViewById(R.id.btnBack);

        setupDayOfWeekSpinner();
        setupTimePickers();
        setupButtons();

        // 수업 추가 결과 관찰
        viewModel.getAddClassResult().observe(
                getViewLifecycleOwner(),
                result -> {
                    if (result == null) return;
                    if (result.success) {
                        Toast.makeText(getContext(), "수업이 추가되었어요.", Toast.LENGTH_SHORT).show();
                        // 뒤로 가기
                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    } else {
                        Toast.makeText(getContext(),
                                "추가 실패: " + result.errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        btnSave.setOnClickListener(v -> {
            String name = etCourseName.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "과목명을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (spinnerDayOfWeek.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "요일을 선택해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startMin < 0 || endMin < 0) {
                Toast.makeText(getContext(), "시작/끝 시간을 선택해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (endMin <= startMin) {
                Toast.makeText(getContext(), "끝 시간은 시작 시간보다 늦어야 해요.", Toast.LENGTH_SHORT).show();
                return;
            }

            int dayOfWeek = mapSpinnerIndexToDayOfWeek(
                    spinnerDayOfWeek.getSelectedItemPosition()
            );

            // Course 객체 구성
            Course course = new Course();
            course.setName(name);
            // location, professor 등은 Course 모델에 있으면 세터 호출:
            // course.setLocation(location);
            course.setColorHex(generateRandomColorHex());

            // Slot 하나만 (다음 단계에서 여러 개로 확장)
            ClassSlot slot = new ClassSlot();
            slot.setDayOfWeek(dayOfWeek);
            slot.setStartMin(startMin);
            slot.setEndMin(endMin);

            List<ClassSlot> slots = new ArrayList<>();
            slots.add(slot);

            // ViewModel 통해 Firestore에 저장
            viewModel.addClass(course, slots);
        });
    }

    private void setupDayOfWeekSpinner() {
        // 0: 선택하세요, 1~7: 월~일
        String[] days = {"요일 선택", "월", "화", "수", "목", "금", "토", "일"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                days
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);
    }

    private void setupTimePickers() {
        tvStartTime.setOnClickListener(v ->
                showTimePicker(true)
        );
        tvEndTime.setOnClickListener(v ->
                showTimePicker(false)
        );
    }

    private void showTimePicker(boolean isStart) {
        int hour = 9;
        int minute = 0;

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, h, m) -> {
                    int totalMin = h * 60 + m;
                    String text = String.format("%02d:%02d", h, m);
                    if (isStart) {
                        startMin = totalMin;
                        tvStartTime.setText(text);
                    } else {
                        endMin = totalMin;
                        tvEndTime.setText(text);
                    }
                },
                hour,
                minute,
                true
        );
        dialog.show();
    }

    private int mapSpinnerIndexToDayOfWeek(int index) {
        // index: 0~8 -> dayOfWeek: 1~7
        // 0(선택X)은 이미 체크해서 여기 안 들어오게 했음
        return index; // 1=월, 2=화, ..., 7=일
    }

    private String generateRandomColorHex() {
        // 간단한 파스텔톤 랜덤 색 (과목마다 고정)
        Random rnd = new Random();
        int r = 150 + rnd.nextInt(100);
        int g = 150 + rnd.nextInt(100);
        int b = 150 + rnd.nextInt(100);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
