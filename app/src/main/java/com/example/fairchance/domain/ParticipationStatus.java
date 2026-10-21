package com.example.fairchance.domain;

/**
 * Canonical participant states stored in an entrant's event history.
 */
public enum ParticipationStatus {
    NONE(null),
    WAITING("Waiting"),
    SELECTED("Selected"),
    CONFIRMED("Confirmed"),
    DECLINED("Declined"),
    CANCELLED("Cancelled"),
    UNKNOWN("");

    private final String storageValue;

    ParticipationStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    public static ParticipationStatus fromStorage(String value) {
        for (ParticipationStatus status : values()) {
            if (status.storageValue != null && status.storageValue.equalsIgnoreCase(value == null ? "" : value.trim())) {
                return status;
            }
        }
        return value == null ? NONE : UNKNOWN;
    }

    public String displayName() {
        return storageValue == null ? "" : storageValue;
    }
}
