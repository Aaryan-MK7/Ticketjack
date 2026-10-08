package com.example.fairchance;

import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.data.repository.EventRepository;
import java.util.Map;

/**
 * Provides test-only helper interfaces and dummy repositories
 * when the main app does not expose them.
 */
public class TestHelpers {

    // Generic test listener to mimic AuthRepository and EventRepository callbacks
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Fake versions of AuthRepository methods to keep the tests valid if constructors differ
    public static class AuthRepositoryShim extends AuthRepository {
        public void updateUserProfile(String userId, Map<String, Object> updates, OnCompleteListener listener) {
            listener.onSuccess();
        }

        public void deleteCurrentUser(String userId, OnCompleteListener listener) {
            listener.onSuccess();
        }
    }

    public static class EventRepositoryShim extends EventRepository {
        public void createEvent(String id, Map<String, Object> data, OnCompleteListener listener) {
            listener.onSuccess();
        }

        public void updateEventStatus(String id, Map<String, Object> data, OnCompleteListener listener) {
            listener.onSuccess();
        }
    }
}
