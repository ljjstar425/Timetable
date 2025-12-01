package com.bar.timetable2.data.model;

import com.google.firebase.Timestamp;

public class Course {

    private String id;          // Firestore 문서 id (수동 세팅)
    private String name;
    private String professor;
    private String location;
    private String colorHex;
    private String memo;
    private Timestamp createdAt;

    // Firestore용 기본 생성자 (필수)
    public Course() {}

    public Course(String name, String professor, String location,
                  String colorHex, String memo, Timestamp createdAt) {
        this.name = name;
        this.professor = professor;
        this.location = location;
        this.colorHex = colorHex;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    // --- getter / setter ---

    public String getId() {
        return id;
    }

    public void setId(String id) {  // documentSnapshot.getId()로 세팅
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfessor() {
        return professor;
    }

    public void setProfessor(String professor) {
        this.professor = professor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
