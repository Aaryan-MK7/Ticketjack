package com.example.fairchance.ui;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.R;
import com.example.fairchance.ui.fragments.EventDetailsFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

@RunWith(AndroidJUnit4.class)
public class EventDetailsUiTest {

    private FragmentScenario<EventDetailsFragment> launchEventDetails() {
        return FragmentScenario.launchInContainer(
                EventDetailsFragment.class,
                null,
                R.style.Theme_FairChance,
                (FragmentFactory) null
        );
    }

    @Test
    public void eventDetailsFragment_launchesWithoutCrash() {
        launchEventDetails();
        onView(isRoot()).check(matches(isDisplayed()));
    }
}
