package com.example.fairchance;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class FirebaseConnectionTest {

    @Test
    public void firebaseApp_andFirestore_areAvailable() {
        // Get application context used in instrumented tests
        Context context = ApplicationProvider.getApplicationContext();

        // Initialize FirebaseApp only if it hasn't been initialized yet
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }

        // Verify default app exists
        FirebaseApp app = FirebaseApp.getInstance();
        assertNotNull("FirebaseApp should be initialized", app);

        // Verify Firestore instance is available
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        assertNotNull("FirebaseFirestore instance should not be null", db);
    }
}
