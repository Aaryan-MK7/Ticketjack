package com.example.fairchance.models;

import java.util.Date;

/**
 * Represents a specific event invitation status for a user.
 * This DTO retrieves data from the 'eventHistory' sub-collection where the status indicates
 * the user has been selected, facilitating the UI display for accepting or declining invitations.
 */
public class Invitation {
    private String eventName;
    private Date eventDate;
    private String status;
    private String eventId;

    public Invitation() {
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
