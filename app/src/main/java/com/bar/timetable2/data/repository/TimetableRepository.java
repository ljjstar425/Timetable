package com.bar.timetable2.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.bar.timetable2.data.firebase.FirestoreClient;
import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.bar.timetable2.data.model.TimetableState;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
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

    // 수업 추가용 콜백
    public interface AddClassCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // 과목 + 슬롯 저장
    public void addClass(Course course,
                         List<ClassSlot> slots,
                         AddClassCallback callback) {

        // 1) 새 course 문서 레퍼런스 만들기 (자동 ID)
        FirebaseFirestore db = client.getDb();
        DocumentReference courseRef = client.getMyCoursesCollection().document();
        String courseId = courseRef.getId();

        // 2) course 객체에 id & createdAt 세팅 (필요 없다면 생략해도 됨)
        course.setId(courseId);
        try {
            // Course 모델에 createdAt 필드가 있다면:
            // private Timestamp createdAt;
            course.setCreatedAt(new Timestamp(new Date()));
        } catch (Exception e) {
            // createdAt 없으면 setCreatedAt 부분은 그냥 빼도 됨
        }

        // 3) batch 시작
        WriteBatch batch = db.batch();

        // 3-1) course 문서 쓰기
        batch.set(courseRef, course);

        // 3-2) slots 문서들 쓰기
        for (ClassSlot slot : slots) {
            if (slot == null) continue;

            slot.setCourseId(courseId);

            DocumentReference slotRef = client.getMySlotsCollection().document();
            slot.setId(slotRef.getId());

            batch.set(slotRef, slot);
        }

        // 4) 커밋
        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
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
