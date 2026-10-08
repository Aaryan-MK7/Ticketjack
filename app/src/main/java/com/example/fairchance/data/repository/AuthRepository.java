package com.example.fairchance.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class implementing the "single source of truth" pattern for all
 * authentication and user profile-related data. It handles all interactions
 * with Firebase Authentication and the {@code users} collection in Firestore.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    //region Callback Interfaces

    /**
     * Callback for authentication operations such as login or registration.
     */
    public interface AuthCallback {
        /**
         * Called when the auth operation completes successfully.
         *
         * @param user the authenticated {@link FirebaseUser}
         */
        void onSuccess(FirebaseUser user);

        /**
         * Called when the auth operation fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for retrieving the current user's role.
     */
    public interface RoleCallback {
        /**
         * Called when the role is successfully fetched.
         *
         * @param role the role string (e.g., {@code "organizer"}, {@code "admin"}, {@code "entrant"})
         */
        void onRoleFetched(String role);

        /**
         * Called when the role could not be fetched.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Callback for retrieving a full {@link User} profile.
     */
    public interface UserProfileCallback {
        /**
         * Called when the user profile is successfully fetched.
         *
         * @param user the parsed {@link User} model
         */
        void onSuccess(User user);

        /**
         * Called when the user profile could not be fetched or parsed.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Simple success/error callback for Firestore or configuration tasks
     * where no return value is needed.
     */
    public interface TaskCallback {
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
     * Lightweight user summary for admin views when listing users.
     * Contains only the fields required for user management UIs.
     */
    public static class AdminUserSummary {
        private final String userId;
        private final String name;
        private final String email;
        private final String role;

        /**
         * Constructs a new user summary for admin use.
         *
         * @param userId the user's UID in Firebase Authentication / Firestore
         * @param name   the user's display name
         * @param email  the user's email address
         * @param role   the user's role (e.g., {@code "organizer"}, {@code "admin"}, {@code "entrant"})
         */
        public AdminUserSummary(String userId, String name, String email, String role) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }

    /**
     * Callback for retrieving a list of users, typically for admin tools
     * that need to inspect multiple full {@link User} profiles.
     */
    public interface AdminUsersCallback {
        /**
         * Called when the list of users is successfully fetched.
         *
         * @param users list of {@link AdminUserSummary} for each user
         */
        void onSuccess(List<AdminUserSummary> users);

        /**
         * Called when fetching the user list fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }
    //endregion

    /**
     * Callback for retrieving a list of full {@link User} profiles.
     * (Currently not used in most admin views, which prefer {@link AdminUserSummary}.)
     */
    public interface UserListCallback {
        /**
         * Called when the list of users is successfully fetched.
         *
         * @param users list of {@link User} models
         */
        void onSuccess(List<User> users);

        /**
         * Called when fetching the user list fails.
         *
         * @param message human-readable error message
         */
        void onError(String message);
    }

    /**
     * Initializes the repository with instances of {@link FirebaseAuth}
     * and {@link FirebaseFirestore}.
     */
    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets the currently signed-in {@link FirebaseUser}.
     *
     * @return the current user, or {@code null} if no user is signed in
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Fetches the full profile (as a {@link User} object) for the currently
     * logged-in user from the {@code users} collection in Firestore.
     *
     * @param callback callback to receive the {@link User} on success or an error message
     */
    public void getUserProfile(UserProfileCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }

        db.collection("users").document(fUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError("Failed to parse user data.");
                        }
                    } else {
                        callback.onError("User profile document not found.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Fetches just the {@code role} field for the currently logged-in user.
     * Used for determining which dashboard to display.
     *
     * @param callback callback to receive the role string on success or an error message
     */
    public void getUserRole(RoleCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is currently logged in.");
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            if (role != null) {
                                callback.onRoleFetched(role);
                            } else {
                                callback.onError("Role field is missing in user document.");
                            }
                        } else {
                            callback.onError("User profile document not found.");
                        }
                    } else {
                        callback.onError("Failed to fetch user role: " +
                                (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                    }
                });
    }

    /**
     * Updates the current user's profile data in their Firestore document.
     *
     * @param newName           updated name
     * @param newEmail          updated email
     * @param newPhone          updated phone number
     * @param notificationPrefs updated notification preferences map
     * @param callback          callback notified of success or failure
     */
    public void updateUserProfile(String newName, String newEmail, String newPhone,
                                  Map<String, Object> notificationPrefs, TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("phone", newPhone);
        updates.put("notificationPreferences", notificationPrefs);

        db.collection("users").document(fUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Deletes the current user's Firestore document and their Authentication record.
     * This is an irreversible action.
     *
     * @param callback callback notified of success or failure
     */
    public void deleteCurrentUser(TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }
        String userId = fUser.getUid();

        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    fUser.delete()
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Saves the user's FCM registration token to their Firestore document.
     *
     * @param token the FCM token, or {@code null} to remove it
     */
    public void saveFcmToken(String token) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            Log.d(TAG, "No user logged in, skipping FCM token save.");
            return;
        }

        db.collection("users").document(fUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore."))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM Token", e));
    }

    /**
     * Saves the user's FCM token and reports the result through a {@link TaskCallback}.
     *
     * @param token    the FCM token, or {@code null} to remove it
     * @param callback callback notified of success or failure
     */
    private void saveFcmToken(String token, TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user logged in.");
            return;
        }

        db.collection("users").document(fUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM Token update successful: " +
                            (token == null ? "removed" : "set"));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving FCM Token with callback", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Updates the user's overall notification status by deleting or setting the FCM token.
     * Fulfills US 01.04.03 Criterion 2: mechanism to stop push notifications.
     *
     * @param enableNotifications {@code true} to fetch and save a token, {@code false} to clear it
     * @param callback            notified of success or failure
     */
    public void updateNotificationStatus(boolean enableNotifications, TaskCallback callback) {
        if (!enableNotifications) {
            // User opts out - clear token
            saveFcmToken(null, callback);
            return;
        }

        // User opts in - fetch new token and save
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError("Fetching FCM registration token failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                        return;
                    }
                    String token = task.getResult();
                    saveFcmToken(token, callback);
                });
    }

    /**
     * Logs in a user with email and password (for Organizer or Admin) and
     * verifies that the user's role in Firestore matches the expected role.
     *
     * @param email        user's email
     * @param password     user's password
     * @param expectedRole role the user is trying to log in as (e.g., {@code "organizer"}, {@code "admin"})
     * @param callback     callback returning the authenticated {@link FirebaseUser} or an error
     */
    public void loginEmailPasswordUser(String email, String password, String expectedRole, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user == null) {
                            callback.onError("Login successful but user was null.");
                            return;
                        }

                        // Authorize by fetching role from Firestore
                        db.collection("users").document(user.getUid()).get()
                                .addOnCompleteListener(docTask -> {
                                    if (docTask.isSuccessful()) {
                                        DocumentSnapshot document = docTask.getResult();
                                        if (document != null && document.exists()) {
                                            String actualRole = document.getString("role");

                                            if (expectedRole.equals(actualRole)) {
                                                Log.d(TAG, "Role match. Login successful.");
                                                callback.onSuccess(user);
                                            } else {
                                                Log.w(TAG, "Role mismatch. Expected: " + expectedRole +
                                                        ", Got: " + actualRole);
                                                auth.signOut(); // Log the user back out
                                                callback.onError("Access Denied: You do not have " +
                                                        expectedRole + " privileges.");
                                            }
                                        } else {
                                            Log.w(TAG, "Login failed: User profile document not found.");
                                            auth.signOut();
                                            callback.onError("Login failed: User profile not found.");
                                        }
                                    } else {
                                        Log.w(TAG, "Login failed: Could not read user document.",
                                                docTask.getException());
                                        auth.signOut();
                                        callback.onError("Login failed: Could not read user profile.");
                                    }
                                });
                    } else {
                        Log.w(TAG, "Email/Password login failed.", task.getException());
                        callback.onError(task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown login error");
                    }
                });
    }

    /**
     * Registers a new user with email and password (for Organizer or Admin),
     * creates their Auth record, and creates their user document in Firestore.
     *
     * @param email     user's email
     * @param password  user's password
     * @param role      role to assign (e.g., {@code "organizer"}, {@code "admin"})
     * @param firstName user's first name
     * @param lastName  user's last name
     * @param phone     user's phone number
     * @param callback  callback returning the new {@link FirebaseUser} or an error
     */
    public void registerEmailPasswordUser(String email, String password, String role,
                                          String firstName, String lastName, String phone, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email/Password registration successful.");
                        FirebaseUser user = task.getResult().getUser();
                        createUserDocument(user, email, role, firstName, lastName, phone, callback);
                    } else {
                        Log.w(TAG, "Email/Password registration failed.", task.getException());
                        callback.onError(task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown registration error");
                    }
                });
    }

    /**
     * Registers and logs in a new Entrant user via anonymous authentication,
     * creates their Auth record, and creates their user document in Firestore.
     *
     * @param email     user's email
     * @param firstName user's first name
     * @param lastName  user's last name
     * @param phone     user's phone number
     * @param callback  callback returning the new anonymous {@link FirebaseUser} or an error
     */
    public void registerAndLoginEntrant(String email, String firstName, String lastName,
                                        String phone, AuthCallback callback) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous sign-in successful.");
                        FirebaseUser user = task.getResult().getUser();
                        createUserDocument(user, email, "entrant", firstName, lastName, phone, callback);
                    } else {
                        Log.w(TAG, "Anonymous sign-in failed.", task.getException());
                        callback.onError(task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown anonymous sign-in error");
                    }
                });
    }

    /**
     * Creates a new user document in the {@code users} collection after a successful
     * Firebase Authentication registration.
     *
     * @param user     the authenticated {@link FirebaseUser}
     * @param email    user's email
     * @param role     user's role
     * @param firstName user's first name
     * @param lastName  user's last name
     * @param phone     user's phone number
     * @param callback  callback returning the {@link FirebaseUser} or an error
     */
    private void createUserDocument(FirebaseUser user, String email, String role,
                                    String firstName, String lastName, String phone,
                                    AuthCallback callback) {

        if (user == null) {
            callback.onError("User is null, cannot create profile.");
            return;
        }

        Map<String, Object> notificationPrefs = new HashMap<>();
        notificationPrefs.put("lotteryResults", true);
        notificationPrefs.put("organizerUpdates", true);

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);
        userData.put("name", firstName + " " + lastName);
        userData.put("phone", phone);
        userData.put("notificationPreferences", notificationPrefs);
        userData.put("fcmToken", null);

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created for " + user.getUid());
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                    callback.onError("Failed to save user profile: " + e.getMessage());
                });
    }

    /**
     * Fetches a lightweight list of all user profiles for the Admin UI.
     * Each entry is represented as an {@link AdminUserSummary}.
     *
     * @param callback callback receiving the list of {@link AdminUserSummary} or an error
     */
    public void fetchAllUsers(AdminUsersCallback callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener((QuerySnapshot snapshots) -> {
                    List<AdminUserSummary> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        result.add(new AdminUserSummary(id, name, email, role));
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Deletes a specific user's Firestore profile document (not the Auth record),
     * and logs the removal to the {@code adminRemovalLogs} collection.
     *
     * @param userId   ID of the user to remove
     * @param callback callback for success / error
     */
    public void deleteUserProfileById(String userId, TaskCallback callback) {
        FirebaseUser admin = auth.getCurrentUser();
        String adminId = admin != null ? admin.getUid() : "unknown";

        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    logUserRemoval(userId, adminId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Logs that an admin removed a user profile to the
     * {@code adminRemovalLogs} collection.
     *
     * @param removedUserId ID of the user that was removed
     * @param adminId       ID of the admin who performed the removal
     */
    private void logUserRemoval(String removedUserId, String adminId) {
        Map<String, Object> log = new HashMap<>();
        log.put("removedUserId", removedUserId);
        log.put("removedByAdminId", adminId);
        log.put("timestamp", FieldValue.serverTimestamp());

        db.collection("adminRemovalLogs").add(log)
                .addOnSuccessListener(ref ->
                        Log.d(TAG, "Removal logged with id: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to log removal", e));
    }

    /**
     * Deactivates an organizer user (soft delete) instead of removing the document.
     * Sets flags on the user document, records metadata, and logs the action to
     * {@code organizerDeactivationLogs}.
     *
     * @param organizerId ID of the organizer to deactivate
     * @param reason      human-readable reason for deactivation
     * @param callback    callback notified of success or failure (may be {@code null})
     */
    public void deactivateOrganizer(String organizerId, String reason, TaskCallback callback) {
        FirebaseUser admin = auth.getCurrentUser();
        String adminId = admin != null ? admin.getUid() : "unknown";

        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", false);
        updates.put("roleActive", false);
        updates.put("deactivatedAt", FieldValue.serverTimestamp());
        updates.put("deactivatedByAdminId", adminId);
        updates.put("deactivationReason", reason);

        db.collection("users").document(organizerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    logOrganizerDeactivation(organizerId, adminId, reason);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Logs an admin action that deactivated an organizer to the
     * {@code organizerDeactivationLogs} collection.
     *
     * @param organizerId ID of the organizer whose account was deactivated
     * @param adminId     ID of the admin who performed the deactivation
     * @param reason      reason for deactivation
     */
    private void logOrganizerDeactivation(String organizerId, String adminId, String reason) {
        Map<String, Object> log = new HashMap<>();
        log.put("organizerId", organizerId);
        log.put("adminId", adminId);
        log.put("reason", reason);
        log.put("timestamp", FieldValue.serverTimestamp());

        db.collection("organizerDeactivationLogs")
                .add(log)
                .addOnSuccessListener(ref ->
                        Log.d(TAG, "Organizer deactivation logged: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to log organizer deactivation", e));
    }

    /**
     * Signs out the current user from Firebase Authentication.
     */
    public void signOut() {
        auth.signOut();
    }
}
