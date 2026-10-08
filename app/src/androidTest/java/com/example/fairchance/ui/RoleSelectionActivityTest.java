package com.example.fairchance.ui;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RoleSelectionActivityTest {

    @Rule
    public ActivityScenarioRule<RoleSelectionActivity> activityRule =
            new ActivityScenarioRule<>(RoleSelectionActivity.class);

    @Test
    public void roleSelectionActivity_launchesWithoutCrash() {
        // If the activity failed to launch, this test would fail automatically.
    }
}
