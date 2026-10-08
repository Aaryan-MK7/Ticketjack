package com.example.fairchance.models;

import java.util.Map;

/**
 * Domain model representing a registered user in the system.
 * This POJO maps directly to documents in the "users" collection in Firestore.
 * It encapsulates profile information, role-based access control data (entrant, organizer, admin),
 * and notification settings.
 */
public class User {
    private String name;
    private String email;
    private String phone;
    private String role;
    private Map<String, Boolean> notificationPreferences;
    private String fcmToken;

    public User() {
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public Map<String, Boolean> getNotificationPreferences() {
        return notificationPreferences;
    }

    public String getFcmToken() {
        return fcmToken;
    }
}
