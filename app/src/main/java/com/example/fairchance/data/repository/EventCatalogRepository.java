package com.example.fairchance.data.repository;

import com.example.fairchance.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Read-only access to event catalog and participant-display data.
 *
 * State-changing enrollment and lottery operations belong in
 * {@link EnrollmentRepository} and callable Cloud Functions.
 */
public class EventCatalogRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface EventCallback {
        void onSuccess(Event event);
        void onError(String message);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(String message);
    }

    public interface StatusCallback {
        void onSuccess(String status);
        void onError(String message);
    }

    public EventCatalogRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void getEvent(String eventId, EventCallback callback) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError("Event not found.");
                        return;
                    }
                    Event event = document.toObject(Event.class);
                    if (event == null) {
                        callback.onError("Event data could not be read.");
                        return;
                    }
                    event.setEventId(document.getId());
                    callback.onSuccess(event);
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public ListenerRegistration observeWaitingListCount(String eventId, CountCallback callback) {
        return db.collection("events").document(eventId).collection("waitingList")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                    } else if (snapshot != null) {
                        callback.onSuccess(snapshot.size());
                    }
                });
    }

    public void getCurrentUserStatus(String eventId, StatusCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }

        db.collection("users").document(user.getUid()).collection("eventHistory")
                .document(eventId).get()
                .addOnSuccessListener(document -> callback.onSuccess(
                        document.exists() ? document.getString("status") : null
                ))
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }
}
