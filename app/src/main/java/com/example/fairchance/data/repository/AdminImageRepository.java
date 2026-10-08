package com.example.fairchance.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.fairchance.models.AdminImageItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for admin image management.
 * - Fetches all event poster images.
 * - Deletes a poster image from Firebase Storage.
 * - Logs deletions to Firestore.
 */
public class AdminImageRepository {

    private static final String TAG = "AdminImageRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface ImageListCallback {
        void onSuccess(List<AdminImageItem> images);
        void onError(String message);
    }

    public interface TaskCallback {
        void onSuccess();
        void onError(String message);
    }

    /**
     * Fetch all images for admin review.
     * Uses posterUploadedByName/posterUploadedAt if available,
     * otherwise falls back to createdByName/createdAt/organizerName.
     */
    public void fetchAllImages(@NonNull ImageListCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AdminImageItem> result = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String eventId = doc.getId();
                        String eventName = doc.getString("name");

                        // Uploader name priority:
                        // 1) posterUploadedByName
                        // 2) createdByName
                        // 3) organizerName
                        String uploaderName = doc.getString("posterUploadedByName");
                        if (uploaderName == null) {
                            uploaderName = doc.getString("createdByName");
                        }
                        if (uploaderName == null) {
                            uploaderName = doc.getString("organizerName");
                        }

                        // Upload time priority:
                        // 1) posterUploadedAt
                        // 2) createdAt
                        // 3) timeCreated (if you use that field)
                        com.google.firebase.Timestamp uploadedAt =
                                doc.getTimestamp("posterUploadedAt");
                        if (uploadedAt == null) {
                            uploadedAt = doc.getTimestamp("createdAt");
                        }
                        if (uploadedAt == null) {
                            uploadedAt = doc.getTimestamp("timeCreated");
                        }

                        // All possible image fields
                        String posterUrl = doc.getString("posterImageUrl");
                        String imageUrl = doc.getString("imageUrl");
                        String bannerUrl = doc.getString("bannerUrl");
                        String thumbnailUrl = doc.getString("thumbnailUrl");

                        ArrayList<String> urls = new ArrayList<>();
                        if (posterUrl != null && !posterUrl.isEmpty()) urls.add(posterUrl);
                        if (imageUrl != null && !imageUrl.isEmpty()) urls.add(imageUrl);
                        if (bannerUrl != null && !bannerUrl.isEmpty()) urls.add(bannerUrl);
                        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) urls.add(thumbnailUrl);

                        for (String url : urls) {
                            result.add(new AdminImageItem(
                                    eventId,
                                    url,
                                    eventName != null ? eventName : "Untitled event",
                                    uploaderName,
                                    uploadedAt   // may be null for very old events; UI handles that
                            ));
                        }
                    }

                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching images", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Deletes image from Storage, clears poster reference in event,
     * and logs the removal to Firestore.
     */
    public void deleteImage(@NonNull AdminImageItem item,
                            String adminUserId,
                            @NonNull TaskCallback callback) {

        String imageUrl = item.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onError("Image URL is empty.");
            return;
        }

        final StorageReference ref;
        try {
            ref = storage.getReferenceFromUrl(imageUrl);
        } catch (IllegalArgumentException ex) {
            callback.onError("Invalid storage URL.");
            return;
        }

        ref.delete()
                .addOnSuccessListener(aVoid -> {

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("posterImageUrl", null);
                    updates.put("imageUrl", null);
                    updates.put("bannerUrl", null);
                    updates.put("thumbnailUrl", null);

                    db.collection("events")
                            .document(item.getId())
                            .update(updates);

                    Map<String, Object> log = new HashMap<>();
                    log.put("eventId", item.getId());
                    log.put("eventName", item.getTitle());
                    log.put("imageUrl", item.getImageUrl());
                    log.put("removedByUserId", adminUserId);
                    log.put("removedAt", Timestamp.now());

                    db.collection("imageRemovalLogs")
                            .add(log)
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "Failed to log image removal",
                                            task.getException());
                                }
                                callback.onSuccess();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting image", e);
                    callback.onError(e.getMessage());
                });
    }
}
