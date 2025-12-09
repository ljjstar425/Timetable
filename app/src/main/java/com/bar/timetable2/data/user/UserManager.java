package com.bar.timetable2.data.user;

public class UserManager {

    private static UserManager instance;

    // 지금은 로그인 기능이 없으니까, 임시로 고정 아이디 사용
    // 나중에 FirebaseAuth 연동하면 여기만 바꾸면 됨.
    private String currentUserId = "test-user-1";

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }
}
