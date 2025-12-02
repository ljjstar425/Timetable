package com.bar.timetable2.data.model;

public class Friend {
    private String friendId;
    private String displayName; // 있으면 사용, 없으면 friendId로 대체

    public Friend() {
        // Firestore 역직렬화용 빈 생성자
    }

    public Friend(String friendId, String displayName) {
        this.friendId = friendId;
        this.displayName = displayName;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
