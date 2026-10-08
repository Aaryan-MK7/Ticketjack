package com.example.fairchance;

import com.example.fairchance.domain.WaitingListManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WaitingListUnitTest {

    private WaitingListManager waitingList;

    @Before
    public void setUp() {
        waitingList = new WaitingListManager();
    }

    @Test
    public void joinWaitingList_addsEntrant() {
        waitingList.join("user1");
        assertEquals(1, waitingList.count());
        assertTrue(waitingList.getWaitingList().contains("user1"));
    }

    @Test
    public void joinWaitingList_noDuplicates() {
        waitingList.join("user1");
        waitingList.join("user1");
        assertEquals(1, waitingList.count());
    }

    @Test
    public void leaveWaitingList_removesEntrant() {
        waitingList.join("user1");
        waitingList.leave("user1");
        assertEquals(0, waitingList.count());
        assertFalse(waitingList.getWaitingList().contains("user1"));
    }

    @Test
    public void waitingListCount_correct() {
        waitingList.join("u1");
        waitingList.join("u2");
        waitingList.join("u3");
        assertEquals(3, waitingList.count());
    }

    @Test
    public void drawReplacement_skipsAlreadySelected() {
        waitingList.join("u1");
        waitingList.join("u2");
        waitingList.join("u3");

        List<String> selected = Arrays.asList("u1");

        String replacement = waitingList.drawReplacement(selected);

        assertNotNull(replacement);
        assertTrue(waitingList.getWaitingList().contains(replacement));
        assertFalse(selected.contains(replacement));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void waitingListSnapshot_cannotMutateManager() {
        waitingList.join("user1");
        waitingList.getWaitingList().add("user2");
    }

    @Test
    public void nullInputs_areHandledSafely() {
        WaitingListManager initializedWithNull = new WaitingListManager(null);
        initializedWithNull.join(null);

        assertEquals(0, initializedWithNull.count());
        assertEquals(Collections.emptyList(), initializedWithNull.getWaitingList());
        assertNull(initializedWithNull.drawReplacement(Arrays.asList("user1")));
    }

    @Test
    public void nullSelection_treatsEveryoneAsEligible() {
        waitingList.join("user1");

        assertEquals("user1", waitingList.drawReplacement(null));
    }
}
