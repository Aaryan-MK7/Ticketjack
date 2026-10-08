package com.example.fairchance.ui;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class RoleSelectionUITest {

    @Rule
    public ActivityScenarioRule<RoleSelectionActivity> activityRule =
            new ActivityScenarioRule<>(RoleSelectionActivity.class);

    @Test
    public void roleCardsAreVisible() {
        onView(withId(R.id.cardEntrant)).check(matches(isDisplayed()));
        onView(withId(R.id.cardOrganizer)).check(matches(isDisplayed()));
        onView(withId(R.id.cardAdmin)).check(matches(isDisplayed()));
    }

    @Test
    public void entrantCardIsClickable() {
        onView(withId(R.id.cardEntrant)).check(matches(isClickable()));
    }
}
