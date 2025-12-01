package com.bar.timetable2.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreClient {

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

        return "test-user-1"; // 🔥 임시 유저 아이디
    }

    public CollectionReference getMyCoursesCollection() {
        String uid = getCurrentUid();
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COURSES);
    }

    public CollectionReference getMySlotsCollection() {
        String uid = getCurrentUid();
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_SLOTS);
    }
}
