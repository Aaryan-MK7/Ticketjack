package com.example.fairchance.data.repository;

import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

/**
 * Entrant-owned workflow gateway. Every mutation is performed by an
 * authenticated callable Cloud Function to protect capacity and state
 * transitions against concurrent clients.
 */
public class EnrollmentRepository {
    public interface TaskCallback {
        void onSuccess();
        void onError(String message);
    }

    private final FirebaseFunctions functions;

    public EnrollmentRepository() {
        functions = FirebaseFunctions.getInstance();
    }

    public void joinWaitingList(String eventId, Double latitude, Double longitude,
                                TaskCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        if (latitude != null && longitude != null) {
            data.put("latitude", latitude);
            data.put("longitude", longitude);
        }

        functions.getHttpsCallable("joinWaitingList").call(data)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void leaveWaitingList(String eventId, TaskCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        functions.getHttpsCallable("leaveWaitingList").call(data)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }
}
