package com.bar.timetable2.data.model;

import com.google.firebase.Timestamp;

public class FriendRequest {
    private String id;          // document id
    private String fromUserId;
    private String toUserId;
    private String status;      // "pending", "accepted", "rejected"
    private Timestamp createdAt;

    public FriendRequest() {
        // Firestore 역직렬화용
    }

    public FriendRequest(String id, String fromUserId, String toUserId, String status, Timestamp createdAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
