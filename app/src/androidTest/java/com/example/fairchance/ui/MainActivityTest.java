package com.example.fairchance.ui;

import android.os.SystemClock;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.R;
import com.example.fairchance.ui.MainActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This test verifies that MainActivity launches successfully without crashing.
 * We skip Firebase checks, since no user is logged in during instrumented tests.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testMainActivityLaunchesSuccessfully() {
        SystemClock.sleep(2000);

        onView(withId(android.R.id.content)).check(matches(isDisplayed()));
    }
}
