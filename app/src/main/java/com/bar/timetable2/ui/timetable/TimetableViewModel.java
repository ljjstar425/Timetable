package com.bar.timetable2.ui.timetable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.bar.timetable2.data.model.TimetableState;
import com.bar.timetable2.data.repository.TimetableRepository;

public class TimetableViewModel extends ViewModel {

    private final TimetableRepository repository = new TimetableRepository();

    private final MutableLiveData<TimetableState> timetableStateLiveData =
            new MutableLiveData<>();

    private ListenerRegistration registration;
    private boolean started = false;

    public LiveData<TimetableState> getTimetableState() {
        return timetableStateLiveData;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
        }
    }
}
