package com.example.fairchance;

import com.example.fairchance.domain.EventCreationValidator;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for basic event creation rules.
 */
public class EventCreationLogicTest {

    @Test
    public void validEvent_returnsTrue() {
        String name = "Swim Lessons";
        int capacity = 20;
        long start = 1000L;
        long end = 2000L;
        double price = 60.0;

        boolean valid = EventCreationValidator.isValid(name, capacity, start, end, price);
        assertTrue(valid);
    }

    @Test
    public void emptyName_returnsFalse() {
        boolean valid = EventCreationValidator.isValid(
                "   ", 20, 1000L, 2000L, 60.0
        );
        assertFalse(valid);
    }

    @Test
    public void zeroCapacity_returnsFalse() {
        boolean valid = EventCreationValidator.isValid(
                "Yoga", 0, 1000L, 2000L, 25.0
        );
        assertFalse(valid);
    }

    @Test
    public void negativeCapacity_returnsFalse() {
        boolean valid = EventCreationValidator.isValid(
                "Dance", -5, 1000L, 2000L, 25.0
        );
        assertFalse(valid);
    }

    @Test
    public void invalidRegistrationWindow_returnsFalse() {
        // start >= end → invalid
        boolean valid = EventCreationValidator.isValid(
                "Music Class", 10, 2000L, 1000L, 30.0
        );
        assertFalse(valid);
    }

    @Test
    public void negativePrice_returnsFalse() {
        boolean valid = EventCreationValidator.isValid(
                "Art Class", 10, 1000L, 2000L, -5.0
        );
        assertFalse(valid);
    }
}
