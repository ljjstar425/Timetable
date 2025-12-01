package com.bar.timetable2.data.model;

import java.util.List;
import java.util.Map;

public class TimetableState {

    // courseId -> Course
    private Map<String, Course> courseMap;

    // Slot 리스트 (화면 그릴 때 사용)
    private List<ClassSlot> slots;

    public TimetableState(Map<String, Course> courseMap, List<ClassSlot> slots) {
        this.courseMap = courseMap;
        this.slots = slots;
    }

    public Map<String, Course> getCourseMap() {
        return courseMap;
    }

    public List<ClassSlot> getSlots() {
        return slots;
    }
}
