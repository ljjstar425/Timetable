package com.bar.timetable2.data.firebase;

import com.bar.timetable2.data.user.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreClient {
    private static FirestoreClient instance;

    public static FirestoreClient getInstance() {
        if (instance == null) {
            instance = new FirestoreClient();
        }
        return instance;
    }

    private static final String COLLECTION_USERS = "users";
    private static final String SUB_COURSES = "courses";
    private static final String SUB_SLOTS = "slots";

    private final FirebaseFirestore db;

    public FirestoreClient() {

        db = FirebaseFirestore.getInstance();
    }

    private String getCurrentUid() {
//        if (FirebaseAuth.getInstance().getCurrentUser() == null) return null;
//        return FirebaseAuth.getInstance().getCurrentUser().getUid();

        return "test-user-1"; // 확인용 임시 유저 아이디
    }

    public CollectionReference getMyCoursesCollection() {
        String uid = getCurrentUid();
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COURSES);
    }

    public CollectionReference getSlotsCollectionOf(String userId) {
        return db.collection("users")
                .document(userId)
                .collection("slots");
    }

    public CollectionReference getCoursesCollectionOf(String userId) {
        return db.collection("users")
                .document(userId)
                .collection("courses");
    }


    public CollectionReference getMySlotsCollection() {
        String uid = getCurrentUid();
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_SLOTS);
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    // ===== users 컬렉션 =====
    public CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    public DocumentReference getUserDoc(String userId) {
        return getUsersCollection().document(userId);
    }

    // ===== 내 friends 서브컬렉션 =====
    public CollectionReference getMyFriendsCollection() {
        String myId = UserManager.getInstance().getCurrentUserId();
        return getUsersCollection()
                .document(myId)
                .collection("friends");
    }

    // 특정 유저의 friends (친구 시간표 볼 때 사용)
    public CollectionReference getFriendsCollectionOf(String userId) {
        return getUsersCollection()
                .document(userId)
                .collection("friends");
    }

    // ===== friend_requests 글로벌 컬렉션 =====
    public CollectionReference getFriendRequestsCollection() {
        return db.collection("friend_requests");
    }
}
