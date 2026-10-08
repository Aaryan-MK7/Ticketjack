package com.example.fairchance;

import com.example.fairchance.models.AdminUserItem;
import com.google.firebase.Timestamp;
import java.util.Date;
import org.junit.Test;

import static org.junit.Assert.*;

public class AdminUserItemTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        String id = "user1";
        String name = "Owen RA";
        String email = "owen@example.com";
        String role = "organizer";
        Timestamp createdAt = new Timestamp(new Date());

        AdminUserItem user = new AdminUserItem(
                id,
                name,
                email,
                role,
                createdAt
        );

        assertEquals(id, user.getId());
        assertEquals(name, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(role, user.getRole());
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    public void gettersReturnEmptyStringWhenNull() {
        AdminUserItem user = new AdminUserItem(
                "user2",
                null,
                null,
                null,
                null
        );

        assertEquals("", user.getName());
        assertEquals("", user.getEmail());
        assertEquals("", user.getRole());
    }
}
