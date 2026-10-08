package com.example.fairchance;

import com.example.fairchance.domain.InvitationManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for invitation status transitions:
 * - PENDING -> ACCEPTED
 * - PENDING -> DECLINED
 * - No transition out of ACCEPTED / DECLINED
 */
public class InvitationLogicTest {

    private InvitationManager manager;

    @Before
    public void setUp() {
        manager = new InvitationManager();
    }

    @Test
    public void initialState_isPending() {
        assertTrue(manager.isPending());
        assertEquals(InvitationManager.Status.PENDING, manager.getStatus());
    }

    @Test
    public void accept_fromPending_setsAccepted() {
        manager.accept();

        assertTrue(manager.isAccepted());
        assertEquals(InvitationManager.Status.ACCEPTED, manager.getStatus());
    }

    @Test
    public void decline_fromPending_setsDeclined() {
        manager.decline();

        assertTrue(manager.isDeclined());
        assertEquals(InvitationManager.Status.DECLINED, manager.getStatus());
    }

    @Test
    public void cannotDecline_afterAccepted() {
        manager.accept();
        manager.decline(); // should be ignored

        assertTrue(manager.isAccepted());
        assertEquals(InvitationManager.Status.ACCEPTED, manager.getStatus());
    }

    @Test
    public void cannotAccept_afterDeclined() {
        manager.decline();
        manager.accept(); // should be ignored

        assertTrue(manager.isDeclined());
        assertEquals(InvitationManager.Status.DECLINED, manager.getStatus());
    }
}
