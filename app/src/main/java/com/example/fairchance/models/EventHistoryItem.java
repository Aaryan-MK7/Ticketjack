package com.example.fairchance.models;

import java.util.Date;

/**
 * Represents a historical record of an event interaction for a user.
 * This model maps to items in the 'users/{UserID}/eventHistory' sub-collection,
 * tracking the user's journey through different lottery states (e.g., Joined, Selected, Cancelled).
 */
public class EventHistoryItem {
    private String eventName;
    private Date eventDate;
    private String status;
    private String eventId;

    public EventHistoryItem() {
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
