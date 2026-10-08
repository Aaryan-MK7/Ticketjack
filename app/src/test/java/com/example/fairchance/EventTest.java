package com.example.fairchance;

import com.example.fairchance.models.Event;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EventTest {
    private Event event;
    private Date now;

    @Before
    public void setUp() {
        event = new Event();
        now = new Date();
        event.setName("Hackathon");
        event.setOrganizerId("org123");
        event.setCategory("Tech");
        event.setEventDate(now);
        event.setCapacity(100);
        event.setPrice(25.5);
        event.setGeolocationRequired(true);
    }

    @Test
    public void testGettersAndSetters() {
        assertEquals("Hackathon", event.getName());
        assertEquals("org123", event.getOrganizerId());
        assertEquals("Tech", event.getCategory());
        assertEquals(100, event.getCapacity());
        assertEquals(25.5, event.getPrice(), 0.001);
        assertTrue(event.isGeolocationRequired());
        assertEquals(now, event.getEventDate());
    }
}
