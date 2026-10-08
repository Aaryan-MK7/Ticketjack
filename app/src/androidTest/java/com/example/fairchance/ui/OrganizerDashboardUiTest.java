package com.example.fairchance.ui;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.R;
import com.example.fairchance.ui.fragments.OrganizerDashboardFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

/**
 * Basic UI test for OrganizerDashboardFragment.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDashboardUiTest {

    private FragmentScenario<OrganizerDashboardFragment> launchOrganizerDashboard() {
        return FragmentScenario.launchInContainer(
                OrganizerDashboardFragment.class,
                null,
                R.style.Theme_FairChance,
                (FragmentFactory) null
        );
    }

    @Test
    public void organizerDashboardFragment_launches() {
        launchOrganizerDashboard();

        onView(isRoot()).check(matches(isDisplayed()));
    }
}
