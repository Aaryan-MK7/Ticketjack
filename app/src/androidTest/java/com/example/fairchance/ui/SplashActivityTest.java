package com.example.fairchance.ui;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import com.example.fairchance.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    // 1. Set 'launchActivity' (3rd param) to false so it doesn't start automatically
    @Rule
    public ActivityTestRule<SplashActivity> rule =
            new ActivityTestRule<>(SplashActivity.class, true, false);

    @Before
    public void setUp() {
        // Initialize Espresso Intents
        Intents.init();

        // 2. Force sign-out to ensure the "Logged Out" path is taken
        // This must run BEFORE the activity creates its AuthRepository
        FirebaseAuth.getInstance().signOut();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSplashRoutesToRoleSelectionWhenLoggedOut() throws InterruptedException {
        // 3. Launch the activity manually now that clean state is ensured
        rule.launchActivity(null);

        // Wait for Splash delay (1.5s) + buffer
        Thread.sleep(2500);

        // Verify that SplashActivity launched RoleSelectionActivity
        intended(hasComponent(RoleSelectionActivity.class.getName()));
    }
}
