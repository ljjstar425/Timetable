package com.bar.timetable2.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.bar.timetable2.data.firebase.FirestoreClient;
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.bar.timetable2.data.model.TimetableState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableRepository {

    private final FirestoreClient client;

    public TimetableRepository() {
        client = new FirestoreClient();
    }

    public interface TimetableStateListener {
        void onChanged(TimetableState state);
        void onError(Exception e);
    }

    /**
     * 내 시간표(Courses + Slots)를 실시간으로 듣기
     */
    public ListenerRegistration listenMyTimetable(TimetableStateListener listener) {

        // 1) Course, Slot 각각 리스너를 붙여서
        // 2) 둘 다 변경될 때마다 합쳐서 timetableState로 만들기

        // 여기서는 간단하게 "슬롯 변화 기준"으로만 구현하는 뼈대 예시를 보여줄게.
        // (실제로는 Course도 snapshotListener 붙이는 게 좋음)

        ListenerRegistration reg = client.getMySlotsCollection()
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            listener.onError(error);
                            return;
                        }

                        List<ClassSlot> slots = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ClassSlot slot = doc.toObject(ClassSlot.class);
                            if (slot != null) {
                                slot.setId(doc.getId());
                                slots.add(slot);
                            }
                        }

                        // Slot에서 사용되는 courseId 목록 추출 후, 한 번에 Course들 로딩
                        loadCoursesForSlots(slots, listener);
                    }
                });

        return reg;
    }

    private void loadCoursesForSlots(List<ClassSlot> slots, TimetableStateListener listener) {

        // courseId를 set으로 모은 뒤, 해당 course만 가져오기 (간단 버전: 전체 courses 로딩)
        client.getMyCoursesCollection()
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, Course> courseMap = new HashMap<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Course course = doc.toObject(Course.class);
                        if (course != null) {
                            course.setId(doc.getId());
                            courseMap.put(course.getId(), course);
                        }
                    }

                    TimetableState state = new TimetableState(courseMap, slots);
                    listener.onChanged(state);
                })
                .addOnFailureListener(listener::onError);
    }

    // --------------------------------------------
    // course + slots 등록/삭제 관련 메서드는
    // 다음 단계에서 "겹침 체크" 로직이랑 같이 detail 짤 예정.
    // --------------------------------------------

}
