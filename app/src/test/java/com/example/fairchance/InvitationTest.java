package com.example.fairchance;

import com.example.fairchance.models.Invitation;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InvitationTest {

    private Invitation invitation;
    private Date date;

    @Before
    public void setUp() {
        invitation = new Invitation();
        date = new Date();
        invitation.setEventName("Charity Gala");
        invitation.setEventDate(date);
        invitation.setStatus("Selected");
        invitation.setEventId("event123");
    }

    @Test
    public void testInvitationGettersAndSetters() {
        assertEquals("Charity Gala", invitation.getEventName());
        assertEquals(date, invitation.getEventDate());
        assertEquals("Selected", invitation.getStatus());
        assertEquals("event123", invitation.getEventId());
    }

    @Test
    public void invitation_allowsEmptyStringsAndNullDate() {
        Invitation inv = new Invitation();

        inv.setEventName("");
        inv.setEventDate(null);
        inv.setStatus("");
        inv.setEventId("");

        assertEquals("", inv.getEventName());
        assertNull(inv.getEventDate());
        assertEquals("", inv.getStatus());
        assertEquals("", inv.getEventId());
    }

    @Test
    public void invitation_defaultConstructor_setsNullFields() {
        Invitation inv = new Invitation();
        assertNull(inv.getEventName());
        assertNull(inv.getEventDate());
        assertNull(inv.getStatus());
        assertNull(inv.getEventId());
    }

}
