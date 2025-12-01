package com.bar.timetable2.data.model;

public class ClassSlot {

    private String id;          // Firestore 문서 id
    private String courseId;    // 해당 Slot이 속한 Course
    private int dayOfWeek;      // 1=Mon ... 7=Sun
    private int startMin;       // 0~1440, 5분 단위
    private int endMin;         // 0~1440, 5분 단위

    public ClassSlot() {}       // Firestore용 기본 생성자

    public ClassSlot(String courseId, int dayOfWeek, int startMin, int endMin) {
        this.courseId = courseId;
        this.dayOfWeek = dayOfWeek;
        this.startMin = startMin;
        this.endMin = endMin;
    }

    // --- getter / setter ---

    public String getId() {
        return id;
    }

    public void setId(String id) {  // documentSnapshot.getId()
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartMin() {
        return startMin;
    }

    public void setStartMin(int startMin) {
        this.startMin = startMin;
    }

    public int getEndMin() {
        return endMin;
    }

    public void setEndMin(int endMin) {
        this.endMin = endMin;
    }
}
