package com.example.fairchance.data.repository;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.fairchance.models.Event;
import com.example.fairchance.models.EventHistoryItem;
import com.example.fairchance.models.Invitation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class implementing the "single source of truth" pattern for all
 * event-related data. It handles all interactions with the {@code events}
 * collection, its sub-collections, and related user history.
 */
public class EventRepository {

    private static final String TAG = "EventRepository";
    private final FirebaseFirestore db;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth auth;
    private final CollectionReference eventsRef;
    private final CollectionReference usersRef;

    //region Callback Interfaces

    /**
     * Generic callback for event-related operations that either succeed or fail
     * without returning additional data.
     */
    public interface EventTaskCallback {
        /**
         * Called when the operation completes successfully.
         */
        void onSuccess();

        /**
         * Called when the operation fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback used when fetching a single {@link Event}.
     */
    public interface EventCallback {
        /**
         * Called when the event is successfully fetched.
         *
         * @param event the fetched {@link Event}
         */
        void onSuccess(Event event);

        /**
         * Called when fetching the event fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback used when fetching a list of {@link Event} objects.
     */
    public interface EventListCallback {
        /**
         * Called when the list of events is successfully fetched.
         *
         * @param events list of {@link Event} items
         */
        void onSuccess(List<Event> events);

        /**
         * Called when fetching the list of events fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for operations that return a waiting-list count.
     */
    public interface WaitlistCountCallback {
        /**
         * Called when the waiting list count is successfully computed.
         *
         * @param count number of entrants currently on the waiting list
         */
        void onSuccess(int count);

        /**
         * Called when fetching the waiting list count fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for checking a user's event history status (e.g., Waiting, Selected).
     */
    public interface EventHistoryCheckCallback {
        /**
         * Called when the status is successfully fetched.
         *
         * @param status the current status string, or {@code null} if no history entry exists
         */
        void onSuccess(String status);

        /**
         * Called when checking the status fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for retrieving a list of {@link EventHistoryItem} entries.
     */
    public interface EventHistoryListCallback {
        /**
         * Called when the event history list is successfully fetched.
         *
         * @param historyItems list of {@link EventHistoryItem} entries
         */
        void onSuccess(List<EventHistoryItem> historyItems);

        /**
         * Called when fetching the event history fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for retrieving a list of {@link Invitation} entries.
     */
    public interface InvitationListCallback {
        /**
         * Called when the invitation list is successfully fetched.
         *
         * @param historyItems list of {@link Invitation} objects
         */
        void onSuccess(List<Invitation> historyItems);

        /**
         * Called when fetching the invitations fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for resolving an organizer's display name.
     */
    public interface OrganizerNameCallback {
        /**
         * Called when the organizer's name is resolved.
         *
         * @param organizerName the resolved name (never {@code null})
         */
        void onSuccess(String organizerName);

        /**
         * Called when the organizer's name cannot be fetched.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for sampling attendees, reporting how many were selected.
     */
    public interface SampleAttendeesCallback {
        /**
         * Called when sampling completes successfully.
         *
         * @param selectedCount number of attendees selected by sampling
         */
        void onSuccess(int selectedCount);

        /**
         * Called when sampling fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for notification operations (Cloud Functions) that report
     * how many notifications were sent and failed.
     */
    public interface NotificationCallback {
        /**
         * Called when the notification operation succeeds.
         *
         * @param sentCount    number of notifications successfully sent
         * @param failureCount number of notifications that failed
         */
        void onSuccess(int sentCount, int failureCount);

        /**
         * Called when the notification operation fails entirely.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    //endregion

    /**
     * Initializes the repository, creating FirebaseAuth and FirebaseFirestore
     * instances and setting references to the main collections.
     */
    public EventRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.eventsRef = db.collection("events");
        this.usersRef = db.collection("users");
    }

    /**
     * Creates a new event document in the {@code events} collection and
     * attaches metadata such as eventId, createdAt, and creator info.
     *
     * @param event    the {@link Event} object to create
     * @param callback callback notified of success or failure
     */
    public void createEvent(Event event, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();

        eventsRef.add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("eventId", eventId);
                    updates.put("createdAt", com.google.firebase.Timestamp.now());

                    if (user != null) {
                        String uploaderName = user.getDisplayName();
                        if (uploaderName == null || uploaderName.isEmpty()) {
                            uploaderName = user.getEmail();
                        }
                        updates.put("createdById", user.getUid());
                        if (uploaderName != null && !uploaderName.isEmpty()) {
                            updates.put("createdByName", uploaderName);
                        }
                    }

                    documentReference.set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Event created with ID: " + eventId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating event with metadata", e);
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating event", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Retrieves a single {@link Event} from Firestore by its ID.
     *
     * @param eventId  ID of the event to fetch
     * @param callback callback receiving the {@link Event} or an error
     */
    public void getEvent(String eventId, EventCallback callback) {
        eventsRef.document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(documentSnapshot.getId());
                            callback.onSuccess(event);
                        } else {
                            callback.onError("Failed to parse event object.");
                        }
                    } else {
                        callback.onError("Event not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting event", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Subscribes to real-time updates for all events whose registration window
     * is currently open, and returns a {@link ListenerRegistration} to remove
     * the listener when no longer needed.
     *
     * @param callback callback that receives a filtered list of open events
     * @return the {@link ListenerRegistration} for this real-time listener
     */
    public ListenerRegistration getAllEvents(EventListCallback callback) {
        return eventsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error getting all events in real-time: ", error);
                callback.onError(error.getMessage());
                return;
            }

            if (value != null) {
                List<Event> eventList = new ArrayList<>();
                Date now = new Date();

                for (QueryDocumentSnapshot document : value) {
                    Event event = document.toObject(Event.class);
                    event.setEventId(document.getId());

                    Date registrationStart = event.getRegistrationStart();
                    Date registrationEnd = event.getRegistrationEnd();

                    boolean isRegistrationOpen =
                            (registrationStart == null || registrationStart.before(now)) &&
                                    (registrationEnd == null || registrationEnd.after(now));

                    if (isRegistrationOpen) {
                        eventList.add(event);
                    }
                }
                callback.onSuccess(eventList);
            }
        });
    }

    /**
     * Subscribes to real-time updates for events created by a specific organizer.
     *
     * @param organizerId ID of the organizer
     * @param callback    callback receiving the organizer's events or an error
     * @return the {@link ListenerRegistration} for this real-time listener
     */
    public ListenerRegistration getEventsForOrganizer(String organizerId, EventListCallback callback) {
        return eventsRef
                .whereEqualTo("organizerId", organizerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting organizer events in real-time: ", error);
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Event> eventList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId());
                            eventList.add(event);
                        }
                        callback.onSuccess(eventList);
                    }
                });
    }

    /**
     * One-time fetch of the number of users currently on an event's waiting list.
     *
     * @param eventId  ID of the event
     * @param callback callback receiving the count or an error
     */
    public void getWaitingListCount(String eventId, WaitlistCountCallback callback) {
        eventsRef.document(eventId).collection("waitingList")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        callback.onSuccess(count);
                    } else {
                        Log.e(TAG, "Error getting waitlist count: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Subscribes to real-time updates of the waiting list size for a given event.
     *
     * @param eventId  ID of the event
     * @param callback callback receiving the updated count or an error
     * @return the {@link ListenerRegistration} for this real-time listener
     */
    public ListenerRegistration listenToWaitingListCount(String eventId, WaitlistCountCallback callback) {
        return eventsRef.document(eventId).collection("waitingList")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to waitlist count", error);
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        callback.onSuccess(value.size());
                    }
                });
    }

    /**
     * Adds the current user to the event's waiting list, enforcing any waiting list
     * capacity limit configured on the {@link Event}.
     *
     * @param eventId  ID of the event
     * @param event    the {@link Event} instance (used for waiting list limit and history info)
     * @param callback callback notified of success or failure
     */
    public void joinWaitingList(String eventId, Event event, EventTaskCallback callback) {
        long limit = event.getWaitingListLimit();

        if (limit > 0) {
            getWaitingListCount(eventId, new WaitlistCountCallback() {
                @Override
                public void onSuccess(int count) {
                    if (count >= limit) {
                        callback.onError("The waiting list for this event is full.");
                    } else {
                        joinWaitingListInternal(eventId, event, callback);
                    }
                }
                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } else {
            joinWaitingListInternal(eventId, event, callback);
        }
    }

    /**
     * Adds the current user to the event's waiting list including an optional
     * geolocation, enforcing the event's waiting list capacity limit.
     *
     * @param eventId  ID of the event
     * @param event    the {@link Event} instance
     * @param lat      latitude of the user location (nullable)
     * @param lng      longitude of the user location (nullable)
     * @param callback callback notified of success or failure
     */
    public void joinWaitingListWithLocation(String eventId, Event event, Double lat, Double lng, EventTaskCallback callback) {
        long limit = event.getWaitingListLimit();

        if (limit > 0) {
            getWaitingListCount(eventId, new WaitlistCountCallback() {
                @Override
                public void onSuccess(int count) {
                    if (count >= limit) {
                        callback.onError("The waiting list for this event is full.");
                    } else {
                        joinWaitingListWithLocationInternal(eventId, event, lat, lng, callback);
                    }
                }
                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } else {
            joinWaitingListWithLocationInternal(eventId, event, lat, lng, callback);
        }
    }

    /**
     * Removes the current user from an event's waiting list and deletes their
     * corresponding event history entry.
     *
     * @param eventId  ID of the event
     * @param callback callback notified of success or failure
     */
    public void leaveWaitingList(String eventId, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);
        batch.delete(waitingListRef);

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        batch.delete(eventHistoryRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " successfully left waiting list for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to leave waiting list", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Applies partial updates to an event document using {@link SetOptions#merge()}.
     *
     * @param eventId  ID of the event to update
     * @param updates  map of fields to update
     * @param callback callback notified of success or failure
     */
    public void updateEventFields(String eventId, Map<String, Object> updates, EventTaskCallback callback) {
        eventsRef.document(eventId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Updates the current user's event history status for a given event.
     *
     * @param eventId   ID of the event
     * @param newStatus new status (e.g., Waiting, Selected, Confirmed)
     * @param callback  callback notified of success or failure
     */
    public void updateEventHistoryStatus(String eventId, String newStatus, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();
        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        eventHistoryRef.update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " history status updated to " + newStatus + " for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update event history status", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Uploads a poster image for an event to Firebase Storage and updates the
     * event document with the poster URL and metadata.
     *
     * @param eventId  ID of the event
     * @param imageUri URI of the local image to upload
     * @param callback callback notified of success or failure
     */
    public void uploadPosterAndUpdate(String eventId, Uri imageUri, EventTaskCallback callback) {
        if (imageUri == null) {
            callback.onError("No image selected.");
            return;
        }

        String fileName = "poster_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("event_posters")
                .child(eventId)
                .child(fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("posterImageUrl", uri.toString());
                            updates.put("posterUploadedAt", com.google.firebase.Timestamp.now());

                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String name = user.getDisplayName();
                                if (name == null || name.isEmpty()) {
                                    name = user.getEmail();
                                }
                                updates.put("posterUploadedById", user.getUid());
                                if (name != null && !name.isEmpty()) {
                                    updates.put("posterUploadedByName", name);
                                }
                            }
                            updateEventFields(eventId, updates, callback);
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Subscribes to real-time updates for the current user's event history,
     * ordered by event date descending.
     *
     * @param callback callback receiving a list of {@link EventHistoryItem} or an error
     * @return the {@link ListenerRegistration} for this real-time listener
     */
    public ListenerRegistration getEventHistory(EventHistoryListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return () -> {};
        }
        String userId = user.getUid();

        return usersRef.document(userId).collection("eventHistory")
                .orderBy("eventDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting event history in real-time: ", error);
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<EventHistoryItem> historyList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            EventHistoryItem item = document.toObject(EventHistoryItem.class);
                            item.setEventId(document.getId());
                            historyList.add(item);
                        }
                        callback.onSuccess(historyList);
                    }
                });
    }

    /**
     * Checks the current user's event history document for a given event and
     * returns the stored status (if any).
     *
     * @param eventId  ID of the event
     * @param callback callback receiving the status or {@code null} if not found
     */
    public void checkEventHistoryStatus(String eventId, EventHistoryCheckCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        usersRef.document(userId).collection("eventHistory").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String status = document.getString("status");
                            callback.onSuccess(status);
                        } else {
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error checking event status: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Fetches the current user's pending invitations, i.e., event history
     * entries with status "Selected", ordered by event date descending.
     *
     * @param callback callback receiving a list of {@link Invitation} or an error
     */
    public void getPendingInvitations(InvitationListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        usersRef.document(userId).collection("eventHistory")
                .whereEqualTo("status", "Selected")
                .orderBy("eventDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Invitation> invitationList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Invitation item = document.toObject(Invitation.class);
                            item.setEventId(document.getId());
                            invitationList.add(item);
                        }
                        callback.onSuccess(invitationList);
                    } else {
                        Log.e(TAG, "Error getting pending invitations: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Samples a set of attendees for the given event by moving users from the
     * waiting list to the selected list and updating user history.
     * This version reports how many users were selected.
     *
     * @param eventId  ID of the event
     * @param count    maximum number of entrants to sample
     * @param callback callback receiving the number selected or an error
     */
    public void sampleAttendees(String eventId, int count, SampleAttendeesCallback callback) {
        eventsRef.document(eventId).collection("selected").get()
                .addOnSuccessListener(selSnap -> {
                    java.util.Set<String> already = new java.util.HashSet<>();
                    for (QueryDocumentSnapshot d : selSnap) {
                        already.add(d.getId());
                    }

                    eventsRef.document(eventId).collection("waitingList").get()
                            .addOnSuccessListener(waitSnap -> {
                                java.util.List<String> pool = new java.util.ArrayList<>();
                                for (QueryDocumentSnapshot d : waitSnap) {
                                    String uid = d.getId();
                                    if (!already.contains(uid)) {
                                        pool.add(uid);
                                    }
                                }

                                if (pool.isEmpty()) {
                                    callback.onError("No entrants to sample.");
                                    return;
                                }

                                java.util.Collections.shuffle(pool);
                                int take = Math.min(count, pool.size());

                                WriteBatch batch = db.batch();
                                com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                                for (int i = 0; i < take; i++) {
                                    String uid = pool.get(i);

                                    DocumentReference selRef = eventsRef.document(eventId)
                                            .collection("selected").document(uid);
                                    Map<String, Object> selData = new HashMap<>();
                                    selData.put("status", "pending");
                                    selData.put("sampledAt", now);
                                    batch.set(selRef, selData);

                                    DocumentReference histRef = usersRef.document(uid)
                                            .collection("eventHistory").document(eventId);
                                    Map<String, Object> hist = new HashMap<>();
                                    hist.put("status", "Selected");
                                    hist.put("updatedAt", now);
                                    batch.set(histRef, hist, SetOptions.merge());

                                    DocumentReference waitRef = eventsRef.document(eventId)
                                            .collection("waitingList").document(uid);
                                    batch.delete(waitRef);
                                }

                                batch.commit()
                                        .addOnSuccessListener(a -> callback.onSuccess(take))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Convenience overload that samples attendees and reports only success or failure
     * (without returning the number selected).
     *
     * @param eventId  ID of the event
     * @param count    maximum number of entrants to sample
     * @param callback callback notified of success or failure
     */
    public void sampleAttendees(String eventId, int count, EventTaskCallback callback) {
        sampleAttendees(eventId, count, new SampleAttendeesCallback() {
            @Override
            public void onSuccess(int selectedCount) {
                callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Records a user's response to an invitation (accepted/declined) and updates
     * both the user's event history and the event's selected/confirmed collections.
     * If declined, automatically draws a replacement entrant.
     *
     * @param eventId  ID of the event
     * @param accepted {@code true} if the user accepts; {@code false} otherwise
     * @param callback callback notified of success or failure
     */
    public void respondToInvitation(String eventId, boolean accepted, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        String newStatus = accepted ? "Confirmed" : "Declined";
        batch.update(eventHistoryRef, "status", newStatus);

        DocumentReference selectedRef = eventsRef.document(eventId)
                .collection("selected").document(userId);
        String newSelectedStatus = accepted ? "accepted" : "declined";
        batch.update(selectedRef, "status", newSelectedStatus);

        if (accepted) {
            DocumentReference confirmedRef = eventsRef.document(eventId)
                    .collection("confirmedAttendees").document(userId);
            Map<String, Object> confirmedData = new HashMap<>();
            confirmedData.put("confirmedAt", com.google.firebase.Timestamp.now());
            batch.set(confirmedRef, confirmedData);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " responded to invitation for " + eventId);
                    if (!accepted) {
                        sampleAttendees(eventId, 1, new SampleAttendeesCallback() {
                            @Override
                            public void onSuccess(int selectedCount) {
                                callback.onSuccess();
                            }
                            @Override
                            public void onError(String message) {
                                // Decline succeeds even if replacement sampling fails
                                callback.onSuccess();
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to respond to invitation", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Cancels all entrants currently in the {@code selected} collection with status
     * {@code "pending"} for the given event, moves them into {@code cancelled}
     * (for organizer visibility), and updates user history status to {@code "Cancelled"}.
     * Fulfills US 02.06.04.
     *
     * @param eventId  ID of the event
     * @param callback callback notified of success or failure
     */
    public void cancelPendingEntrants(String eventId, EventTaskCallback callback) {
        eventsRef.document(eventId).collection("selected")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getId();

                        // Update status in 'selected'
                        batch.update(doc.getReference(), "status", "cancelled");
                        batch.update(doc.getReference(), "cancelledAt", now);

                        // Add to 'cancelled' collection
                        DocumentReference cancelledRef = eventsRef.document(eventId)
                                .collection("cancelled").document(userId);
                        Map<String, Object> cancelledData = new HashMap<>();
                        cancelledData.put("cancelledAt", now);
                        cancelledData.put("reason", "organizer_timeout");
                        batch.set(cancelledRef, cancelledData);

                        // Update user history
                        DocumentReference histRef = usersRef.document(userId)
                                .collection("eventHistory").document(eventId);
                        batch.update(histRef, "status", "Cancelled");
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Cancelled " + snapshot.size() + " pending entrants.");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to batch cancel entrants", e);
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Listener interface for admin views that observe all events in real-time.
     */
    public interface AdminEventsListener {
        /**
         * Called when the events collection changes.
         *
         * @param events the updated list of all {@link Event} objects
         */
        void onEventsChanged(List<Event> events);

        /**
         * Called when listening fails or cannot be established.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Subscribes to all events in real-time for admin dashboards, ordered by
     * {@code timeCreated} descending.
     *
     * @param listener listener notified when events change or on error
     * @return the {@link ListenerRegistration} for this real-time listener
     */
    public ListenerRegistration listenToAllEvents(final AdminEventsListener listener) {
        return db.collection("events")
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (value == null) {
                        listener.onEventsChanged(new ArrayList<>());
                        return;
                    }
                    List<Event> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        result.add(event);
                    }
                    listener.onEventsChanged(result);
                });
    }

    /**
     * Fetches all events that have a non-empty poster URL.
     *
     * @param callback callback receiving the list of events with posters or an error
     */
    public void getAllEventsWithPosters(EventListCallback callback) {
        eventsRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                            result.add(event);
                        }
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting events with posters", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Resolves the organizer's display name using several fallback fields
     * ({@code fullName}, {@code name}, {@code email}, then organizerId).
     *
     * @param organizerId ID of the organizer
     * @param callback    callback receiving the resolved name or an error
     */
    public void getOrganizerName(String organizerId, OrganizerNameCallback callback) {
        if (organizerId == null || organizerId.isEmpty()) {
            callback.onSuccess("Unknown");
            return;
        }
        usersRef.document(organizerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name == null || name.isEmpty()) name = doc.getString("name");
                        if (name == null || name.isEmpty()) name = doc.getString("email");
                        if (name == null || name.isEmpty()) name = organizerId;
                        callback.onSuccess(name);
                    } else {
                        callback.onSuccess(organizerId);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Invokes a Cloud Function to send notifications to users on an event's
     * waiting list.
     *
     * @param eventId  ID of the event
     * @param message  notification message to send
     * @param callback callback receiving sent/failure counts or an error
     */
    public void sendWaitingListNotifications(String eventId, String message, NotificationCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", message);

        FirebaseFunctions.getInstance().getHttpsCallable("sendWaitingListNotifications").call(data)
                .addOnSuccessListener(result -> {
                    int sentCount = 0;
                    int failureCount = 0;
                    Object raw = result.getData();
                    if (raw instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) raw;
                        Object s = map.get("sentCount");
                        Object f = map.get("failureCount");
                        if (s instanceof Number) sentCount = ((Number) s).intValue();
                        if (f instanceof Number) failureCount = ((Number) f).intValue();
                    }
                    if (callback != null) callback.onSuccess(sentCount, failureCount);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Invokes a Cloud Function to send notifications to users whose entries
     * were cancelled for an event.
     *
     * @param eventId  ID of the event
     * @param message  notification message to send
     * @param callback callback receiving sent/failure counts or an error
     */
    public void sendCancelledNotifications(String eventId, String message, NotificationCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", message);

        FirebaseFunctions.getInstance().getHttpsCallable("sendCancelledNotifications").call(data)
                .addOnSuccessListener(result -> {
                    int sentCount = 0;
                    int failureCount = 0;
                    Object raw = result.getData();
                    if (raw instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) raw;
                        Object s = map.get("sentCount");
                        Object f = map.get("failureCount");
                        if (s instanceof Number) sentCount = ((Number) s).intValue();
                        if (f instanceof Number) failureCount = ((Number) f).intValue();
                    }
                    if (callback != null) callback.onSuccess(sentCount, failureCount);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Removes the event poster from Firebase Storage (if a valid URL is provided)
     * and clears the {@code posterImageUrl} field in the event document.
     *
     * @param eventId   ID of the event
     * @param posterUrl URL of the poster image stored in Firebase Storage (nullable)
     * @param callback  callback notified of success or failure
     */
    public void removeEventPoster(String eventId, String posterUrl, EventTaskCallback callback) {
        if (posterUrl == null || posterUrl.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("posterImageUrl", null);
            updateEventFields(eventId, updates, callback);
            return;
        }
        try {
            StorageReference ref = storage.getReferenceFromUrl(posterUrl);
            ref.delete()
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("posterImageUrl", null);
                        updateEventFields(eventId, updates, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image from storage", e);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("posterImageUrl", null);
                        updateEventFields(eventId, updates, callback);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Invalid poster URL", e);
            Map<String, Object> updates = new HashMap<>();
            updates.put("posterImageUrl", null);
            updateEventFields(eventId, updates, callback);
        }
    }

    /**
     * Deletes an event document and its main subcollections
     * ({@code waitingList}, {@code selected}, {@code confirmedAttendees}).
     *
     * @param eventId  ID of the event to delete
     * @param callback callback notified of success or failure
     */
    public void deleteEvent(String eventId, EventTaskCallback callback) {
        DocumentReference eventDoc = eventsRef.document(eventId);
        String[] subcollections = new String[] { "waitingList", "selected", "confirmedAttendees" };
        deleteSubcollectionsThenEvent(eventDoc, subcollections, 0, callback);
    }

    /**
     * Marks all events belonging to a given organizer as inactive and
     * records deactivation metadata (admin ID and reason).
     *
     * @param organizerId ID of the organizer whose events should be deactivated
     * @param adminId     ID of the admin performing the action
     * @param reason      human-readable reason for deactivation
     * @param callback    callback notified of success or failure
     */
    public void deactivateEventsByOrganizer(@NonNull String organizerId,
                                            @NonNull String adminId,
                                            @NonNull String reason,
                                            @NonNull EventTaskCallback callback) {
        eventsRef.whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isActive", false);
                        updates.put("deactivatedAt", now);
                        updates.put("deactivatedByAdminId", adminId);
                        updates.put("deactivationReason", reason);
                        batch.set(doc.getReference(), updates, SetOptions.merge());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Convenience wrapper for drawing a single replacement attendee for an event.
     *
     * @param eventId  ID of the event
     * @param callback callback notified of success or failure
     */
    public void drawReplacement(String eventId, EventTaskCallback callback) {
        sampleAttendees(eventId, 1, callback);
    }

    // ---------- Private helpers ----------

    private void joinWaitingListInternal(String eventId, Event event, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);

        Map<String, Object> waitingListData = new HashMap<>();
        waitingListData.put("joinedAt", com.google.firebase.Timestamp.now());
        batch.set(waitingListRef, waitingListData);

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);

        Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put("eventName", event.getName());
        eventHistoryData.put("eventDate", event.getEventDate());
        eventHistoryData.put("status", "Waiting");
        batch.set(eventHistoryRef, eventHistoryData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " successfully joined waiting list for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join waiting list", e);
                    callback.onError(e.getMessage());
                });
    }

    private void joinWaitingListWithLocationInternal(String eventId,
                                                     Event event,
                                                     Double lat,
                                                     Double lng,
                                                     EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);

        Map<String, Object> waitingListData = new HashMap<>();
        waitingListData.put("joinedAt", com.google.firebase.Timestamp.now());
        if (lat != null && lng != null) {
            waitingListData.put("location", new GeoPoint(lat, lng));
        }
        batch.set(waitingListRef, waitingListData, SetOptions.merge());

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);

        Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put("eventName", event.getName());
        eventHistoryData.put("eventDate", event.getEventDate());
        eventHistoryData.put("status", "Waiting");
        batch.set(eventHistoryRef, eventHistoryData, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " successfully joined waiting list (with location) for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join waiting list (with location)", e);
                    callback.onError(e.getMessage());
                });
    }

    private void deleteSubcollectionsThenEvent(DocumentReference eventDoc,
                                               String[] subcollections,
                                               int index,
                                               EventTaskCallback callback) {
        if (index >= subcollections.length) {
            eventDoc.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Event deleted: " + eventDoc.getId());
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting event document", e);
                        callback.onError(e.getMessage());
                    });
            return;
        }
        String sub = subcollections[index];
        eventDoc.collection(sub).get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> deleteSubcollectionsThenEvent(eventDoc, subcollections, index + 1, callback))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting subcollection " + sub, e);
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching subcollection " + sub, e);
                    callback.onError(e.getMessage());
                });
    }
}
