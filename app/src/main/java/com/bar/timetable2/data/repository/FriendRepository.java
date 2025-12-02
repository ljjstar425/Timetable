package com.bar.timetable2.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.bar.timetable2.data.firebase.FirestoreClient;
import com.bar.timetable2.data.model.Friend;
import com.bar.timetable2.data.model.FriendRequest;
import com.bar.timetable2.data.user.UserManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendRepository {

    private static final String TAG = "FriendRepository";

    private static FriendRepository instance;
    private final FirestoreClient client;

    private FriendRepository() {
        client = new FirestoreClient();
    }

    public static FriendRepository getInstance() {
        if (instance == null) {
            instance = new FriendRepository();
        }
        return instance;
    }

    private String getMyId() {
        return UserManager.getInstance().getCurrentUserId();
    }

    // ===== 콜백 인터페이스 =====

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface FriendListListener {
        void onChanged(List<Friend> friends);
        void onError(String message);
    }

    public interface FriendRequestListListener {
        void onChanged(List<FriendRequest> requests);
        void onError(String message);
    }

    // ===== 1) 친구 요청 보내기 =====
    public void sendFriendRequest(String targetUserId, SimpleCallback callback) {
        String myId = getMyId();

        if (myId.equals(targetUserId)) {
            if (callback != null) callback.onError("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
            return;
        }

        // 1) 대상 유저 존재 확인
        client.getUserDoc(targetUserId).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (callback != null) callback.onError("유저 조회 실패: " + task.getException());
                return;
            }
            DocumentSnapshot doc = task.getResult();
            if (doc == null || !doc.exists()) {
                if (callback != null) callback.onError("해당 ID의 사용자가 없습니다.");
                return;
            }

            // 2) friend_requests 에 새 문서 추가 (pending)
            DocumentReference newReqRef = client.getFriendRequestsCollection().document();
            FriendRequest req = new FriendRequest();
            req.setId(newReqRef.getId());
            req.setFromUserId(myId);
            req.setToUserId(targetUserId);
            req.setStatus("pending");
            req.setCreatedAt(Timestamp.now());

            newReqRef.set(req).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    if (callback != null) callback.onSuccess();
                } else {
                    if (callback != null) callback.onError("친구 요청 실패: " + t.getException());
                }
            });
        });
    }

    // ===== 2) 내가 받은 '대기중' 친구 요청 listen =====
    public ListenerRegistration listenIncomingRequests(FriendRequestListListener listener) {
        String myId = getMyId();
        return client.getFriendRequestsCollection()
                .whereEqualTo("toUserId", myId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        if (listener != null) listener.onError(e.getMessage());
                        return;
                    }
                    if (snap == null) {
                        if (listener != null) listener.onChanged(new ArrayList<>());
                        return;
                    }
                    List<FriendRequest> list = new ArrayList<>();
                    for (DocumentSnapshot ds : snap.getDocuments()) {
                        FriendRequest fr = ds.toObject(FriendRequest.class);
                        if (fr != null) {
                            fr.setId(ds.getId());
                            list.add(fr);
                        }
                    }
                    if (listener != null) listener.onChanged(list);
                });
    }

    // ===== 3) 친구 목록 listen =====
    public ListenerRegistration listenFriends(FriendListListener listener) {
        return client.getMyFriendsCollection()
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        if (listener != null) listener.onError(e.getMessage());
                        return;
                    }
                    if (snap == null) {
                        if (listener != null) listener.onChanged(new ArrayList<>());
                        return;
                    }
                    List<Friend> list = new ArrayList<>();
                    for (DocumentSnapshot ds : snap.getDocuments()) {
                        Friend f = ds.toObject(Friend.class);
                        if (f != null) {
                            list.add(f);
                        }
                    }
                    if (listener != null) listener.onChanged(list);
                });
    }

    // ===== 4) 친구 요청 수락 =====
    public void acceptFriendRequest(FriendRequest request, SimpleCallback callback) {
        if (request == null || request.getId() == null) {
            if (callback != null) callback.onError("잘못된 요청입니다.");
            return;
        }

        String myId = getMyId();
        String otherId = request.getFromUserId();

        // 1) friend_requests 상태 업데이트
        client.getFriendRequestsCollection()
                .document(request.getId())
                .update("status", "accepted")
                .addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        if (callback != null) callback.onError("요청 상태 업데이트 실패: " + t.getException());
                        return;
                    }

                    // 2) friends 양방향 추가
                    addFriendBothSides(myId, otherId, callback);
                });
    }

    private void addFriendBothSides(String myId, String otherId, SimpleCallback callback) {
        // 나 -> 친구
        DocumentReference myFriendDoc = client.getUsersCollection()
                .document(myId)
                .collection("friends")
                .document(otherId);

        // 친구 -> 나
        DocumentReference otherFriendDoc = client.getUsersCollection()
                .document(otherId)
                .collection("friends")
                .document(myId);

        Friend friendForMe = new Friend(otherId, otherId); // displayName은 나중에 users에서 가져와도 됨
        Friend friendForOther = new Friend(myId, myId);

        myFriendDoc.set(friendForMe).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return otherFriendDoc.set(friendForOther);
        }).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                if (callback != null) callback.onSuccess();
            } else {
                if (callback != null) callback.onError("친구 추가 처리 실패: " + t.getException());
            }
        });
    }

    // ===== 5) 친구 요청 거절 =====
    public void rejectFriendRequest(FriendRequest request, SimpleCallback callback) {
        if (request == null || request.getId() == null) {
            if (callback != null) callback.onError("잘못된 요청입니다.");
            return;
        }

        client.getFriendRequestsCollection()
                .document(request.getId())
                .update("status", "rejected")
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        if (callback != null) callback.onError("친구 요청 거절 실패: " + t.getException());
                    }
                });
    }

    // ===== 6) 친구 삭제 (한쪽만 해도 서로 목록에서 제거) =====
    public void removeFriend(String friendId, SimpleCallback callback) {
        String myId = getMyId();

        DocumentReference myFriendDoc = client.getUsersCollection()
                .document(myId)
                .collection("friends")
                .document(friendId);

        DocumentReference otherFriendDoc = client.getUsersCollection()
                .document(friendId)
                .collection("friends")
                .document(myId);

        myFriendDoc.delete().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return otherFriendDoc.delete();
        }).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                if (callback != null) callback.onSuccess();
            } else {
                if (callback != null) callback.onError("친구 삭제 실패: " + t.getException());
            }
        });
    }
}
