package com.example.fairchance;

import com.example.fairchance.models.EventHistoryItem;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EventHistoryItemTest {

    private EventHistoryItem historyItem;
    private Date eventDate;

    @Before
    public void setUp() {
        historyItem = new EventHistoryItem();
        eventDate = new Date();
        historyItem.setEventName("Science Fair");
        historyItem.setEventDate(eventDate);
        historyItem.setStatus("Completed");
        historyItem.setEventId("E001");
    }

    @Test
    public void testHistoryItemFields() {
        assertEquals("Science Fair", historyItem.getEventName());
        assertEquals(eventDate, historyItem.getEventDate());
        assertEquals("Completed", historyItem.getStatus());
        assertEquals("E001", historyItem.getEventId());
    }

    @Test
    public void allowsNullDate() {
        EventHistoryItem item = new EventHistoryItem();
        item.setEventName("No Date Event");
        item.setEventDate(null);
        item.setStatus("Selected");
        item.setEventId("E002");

        assertEquals("No Date Event", item.getEventName());
        assertNull(item.getEventDate());
        assertEquals("Selected", item.getStatus());
        assertEquals("E002", item.getEventId());
    }
}
