package com.example.fairchance;

import com.example.fairchance.models.NotificationLog;
import java.util.Arrays;
import java.util.Date;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotificationLogTest {

    @Test
    public void constructor_setsFieldsAndRecipientCount() {
        String senderId = "org1";
        String senderName = "Organizer One";
        String messageType = "WAITLIST_SELECTED";
        String messageBody = "You have been selected";
        String eventId = "event123";
        String eventName = "Swim Lessons";
        Date timestamp = new Date();

        NotificationLog log = new NotificationLog(
                senderId,
                senderName,
                Arrays.asList("u1", "u2", "u3"),
                messageType,
                messageBody,
                eventId,
                eventName,
                timestamp
        );

        assertEquals(senderId, log.getSenderId());
        assertEquals(senderName, log.getSenderName());
        assertEquals(messageType, log.getMessageType());
        assertEquals(messageBody, log.getMessageBody());
        assertEquals(eventId, log.getEventId());
        assertEquals(eventName, log.getEventName());
        assertEquals(timestamp, log.getTimestamp());
        assertEquals(3, log.getRecipientCount());
    }

    @Test
    public void setRecipientIds_updatesRecipientCount() {
        NotificationLog log = new NotificationLog();
        log.setRecipientIds(Arrays.asList("x", "y"));

        assertEquals(2, log.getRecipientCount());
        assertEquals(Arrays.asList("x", "y"), log.getRecipientIds());
    }
}
