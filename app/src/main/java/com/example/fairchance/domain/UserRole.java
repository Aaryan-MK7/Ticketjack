package com.example.fairchance.domain;

/**
 * Roles recognized by Ticketjack's navigation and authorization boundaries.
 * Firestore values are parsed at the edge instead of being distributed as
 * unvalidated strings through the UI layer.
 */
public enum UserRole {
    ENTRANT("entrant"),
    ORGANIZER("organizer"),
    ADMIN("admin"),
    UNKNOWN("");

    private final String storageValue;

    UserRole(String storageValue) {
        this.storageValue = storageValue;
    }

    public static UserRole fromStorage(String value) {
        for (UserRole role : values()) {
            if (role.storageValue.equalsIgnoreCase(value == null ? "" : value.trim())) {
                return role;
            }
        }
        return UNKNOWN;
    }
}
