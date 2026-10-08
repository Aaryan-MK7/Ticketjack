package com.example.fairchance.domain;

/**
 * Utility class responsible for validating event creation input fields.
 * Ensures that event data meets minimum logical and structural requirements
 * before being submitted to Firestore.
 */
public class EventCreationValidator {

    /**
     * Validates basic event properties such as name, capacity, registration window,
     * and price correctness.
     *
     * @param name        the event name (must be non-null and non-empty)
     * @param capacity    expected maximum number of entrants (must be > 0)
     * @param regStartMs  registration start time in milliseconds since epoch
     * @param regEndMs    registration end time in milliseconds since epoch
     * @param price       event price (must be >= 0)
     *
     * @return {@code true} if all fields are logically valid; {@code false} otherwise
     */
    public static boolean isValid(
            String name,
            int capacity,
            long regStartMs,
            long regEndMs,
            double price
    ) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (capacity <= 0) {
            return false;
        }
        if (regStartMs >= regEndMs) {
            return false;
        }
        if (price < 0.0) {
            return false;
        }
        return true;
    }
}
