package com.bar.timetable2.ui.timetable;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.google.firebase.firestore.ListenerRegistration;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.data.repository.TimetableRepository;

import java.util.List;

public class TimetableViewModel extends ViewModel {

    private final TimetableRepository repository = TimetableRepository.getInstance();

    private final MutableLiveData<TimetableState> timetableStateLiveData =
            new MutableLiveData<>();

    private ListenerRegistration registration;
    private boolean started = false;

    public LiveData<TimetableState> getTimetableState() {
        return timetableStateLiveData;
    }

    public TimetableState getCurrentTimetableState() {
        return timetableStateLiveData.getValue();
    }


    /**
     * 내 시간표 listen 시작 (한 번만 부르면 됨)
     */
    public void startListenMyTimetable() {
        if (started) return;
        started = true;

        registration = repository.listenMyTimetable(new TimetableRepository.TimetableStateListener() {
            @Override
            public void onChanged(TimetableState state) {
                timetableStateLiveData.postValue(state);
            }

            @Override
            public void onError(Exception e) {
                // TODO: 필요하면 에러 상태도 LiveData로 노출
                e.printStackTrace();
            }
        });
    }

    // 수업 추가 결과 모델
    public static class AddClassResult {
        public boolean success;
        public String errorMessage;

        public static AddClassResult success() {
            AddClassResult r = new AddClassResult();
            r.success = true;
            return r;
        }

        public static AddClassResult error(String msg) {
            AddClassResult r = new AddClassResult();
            r.success = false;
            r.errorMessage = msg;
            return r;
        }
    }

    private final MutableLiveData<AddClassResult> addClassResult = new MutableLiveData<>();

    public LiveData<AddClassResult> getAddClassResult() {
        return addClassResult;
    }

    // ===== 수업 삭제 결과 =====
    public static class DeleteClassResult {
        public boolean success;
        public String errorMessage;

        public static DeleteClassResult success() {
            DeleteClassResult r = new DeleteClassResult();
            r.success = true;
            return r;
        }

        public static DeleteClassResult error(String msg) {
            DeleteClassResult r = new DeleteClassResult();
            r.success = false;
            r.errorMessage = msg;
            return r;
        }
    }

    private final MutableLiveData<DeleteClassResult> deleteClassResult = new MutableLiveData<>();

    public LiveData<DeleteClassResult> getDeleteClassResult() {
        return deleteClassResult;
    }

    // 수업 추가 메서드
    public void addClass(Course course, List<ClassSlot> slots) {
        Log.e("TT-VM", "addClass() 호출됨, slots=" + (slots != null ? slots.size() : 0));

        repository.addClass(course, slots, new TimetableRepository.AddClassCallback() {
            @Override
            public void onSuccess() {
                Log.e("TT-VM", "addClass 성공");
                addClassResult.postValue(AddClassResult.success());
                // 별도로 할 건 없음: Firestore snapshot listener가
                // 자동으로 timetableState를 업데이트 해줄 것.
            }

            @Override
            public void onError(Exception e) {
                Log.e("TT-VM", "addClass 실패: " + e.getMessage());
                addClassResult.postValue(AddClassResult.error(e.getMessage()));
            }
        });
    }

    // 수업 삭제 메서드
    public void deleteClass(String courseId) {
        Log.e("TT-VM", "deleteClass() 호출됨, courseId=" + courseId);

        repository.deleteCourseWithSlots(courseId, new TimetableRepository.DeleteClassCallback() {
            @Override
            public void onSuccess() {
                Log.e("TT-VM", "deleteClass 성공");
                deleteClassResult.postValue(DeleteClassResult.success());
                // Firestore snapshot listener가 알아서 timetableState 갱신
            }

            @Override
            public void onError(Exception e) {
                Log.e("TT-VM", "deleteClass 실패: " + e.getMessage());
                deleteClassResult.postValue(DeleteClassResult.error(e.getMessage()));
            }
        });
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
        }
    }
}
