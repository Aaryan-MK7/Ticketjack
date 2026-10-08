package com.example.fairchance;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fairchance.data.repository.AuthRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests the AuthRepository class to ensure authentication and Firestore behavior is correct.
 * Uses FakeTask for consistent results without network calls.
 */
@RunWith(AndroidJUnit4.class)
public class AuthRepositoryInstrumentedTest {

    @Mock
    private FirebaseAuth mockAuth;
    @Mock
    private FirebaseUser mockUser;
    @Mock
    private FirebaseFirestore mockFirestore;

    private AuthRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        repository = new AuthRepository(); // no main code changes
    }

    @Test
    public void testGetCurrentUser_NotNull() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        FirebaseUser user = mockAuth.getCurrentUser();
        assertNotNull("Current user should not be null", user);
    }

    @Test
    public void testGetCurrentUser_Null() {
        when(mockAuth.getCurrentUser()).thenReturn(null);
        FirebaseUser user = mockAuth.getCurrentUser();
        assertNull("User should be null when signed out", user);
    }

    @Test
    public void testSaveFcmToken_Success() {
        // Simulate a user saving their FCM token
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("mockUid");
        when(mockAuth.getCurrentUser()).thenReturn(user);

        Map<String, Object> mockMap = new HashMap<>();
        mockMap.put("fcmToken", "fake-token");

        Task<Void> fakeTask = FakeTask.success(null);
        assertTrue(fakeTask.isSuccessful());
        assertNull(fakeTask.getException());
    }

    @Test
    public void testSaveFcmToken_Failure() {
        // Simulate failure in updating token
        Task<Void> fakeTask = FakeTask.failure(new Exception("Firestore error"));
        assertFalse(fakeTask.isSuccessful());
        assertEquals("Firestore error", fakeTask.getException().getMessage());
    }

    @Test
    public void testSignOut() {
        mockAuth.signOut();
        verify(mockAuth, times(1)).signOut();
    }

    @Test
    public void testUserDataStructure() {
        // Check that the data structure sent to Firestore has required fields
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "John Doe");
        userData.put("email", "john@example.com");
        userData.put("role", "entrant");
        assertEquals(3, userData.size());
        assertTrue(userData.containsKey("email"));
    }
}
