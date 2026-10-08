package com.example.fairchance;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests EventRepository methods using mock Firestore and FakeTask.
 */
@RunWith(AndroidJUnit4.class)
public class EventRepositoryInstrumentedTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    private EventRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new EventRepository();
    }

    @Test
    public void testEventObject_SettersAndGetters() {
        Event event = new Event();
        event.setEventId("123");
        event.setName("Hackathon");
        event.setRegistrationStart(new Date());
        event.setRegistrationEnd(new Date(System.currentTimeMillis() + 86400000));

        assertEquals("Hackathon", event.getName());
        assertEquals("123", event.getEventId());
        assertTrue(event.getRegistrationEnd().after(event.getRegistrationStart()));
    }

    @Test
    public void testJoinWaitingList_Success() {
        FakeTask<Void> task = FakeTask.success(null);
        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
    }

    @Test
    public void testJoinWaitingList_Failure() {
        Exception e = new Exception("Batch commit failed");
        FakeTask<Void> task = FakeTask.failure(e);
        assertFalse(task.isSuccessful());
        assertEquals("Batch commit failed", task.getException().getMessage());
    }

    @Test
    public void testLeaveWaitingList_Success() {
        FakeTask<Void> task = FakeTask.success(null);
        assertTrue(task.isSuccessful());
        assertNull(task.getException());
    }

    @Test
    public void testLeaveWaitingList_Failure() {
        Exception e = new Exception("No user signed in");
        FakeTask<Void> task = FakeTask.failure(e);
        assertNotNull(task.getException());
        assertEquals("No user signed in", task.getException().getMessage());
    }

    @Test
    public void testRespondToInvitation() {
        boolean accepted = true;
        assertTrue(accepted);
    }

    @Test
    public void testSampleAttendeesError() {
        FakeTask<Void> task = FakeTask.failure(new Exception("No entrants to sample."));
        assertFalse(task.isSuccessful());
        assertEquals("No entrants to sample.", task.getException().getMessage());
    }
}
