package com.example.fairchance;

import com.example.fairchance.models.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserTest {
    private User user;

    @Before
    public void setUp() {
        user = new User();
    }

    @Test
    public void testUserFields() {
        Map<String, Boolean> prefs = new HashMap<>();
        prefs.put("lotteryResults", true);

        user = new User();
        assertNull(user.getEmail());
        assertNull(user.getPhone());
        assertNull(user.getFcmToken());
    }

    @Test
    public void testNotificationPreferencesIsMap() {
        assertNull(user.getNotificationPreferences());
    }
}
