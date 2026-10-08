package com.example.fairchance.ui;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.R;
import com.example.fairchance.ui.fragments.EntrantHomeFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

/**
 * UI tests for EntrantHomeFragment.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantDashboardUiTest {

    private FragmentScenario<EntrantHomeFragment> launchEntrantHome() {
        return FragmentScenario.launchInContainer(
                EntrantHomeFragment.class,
                null,
                R.style.Theme_FairChance,
                (FragmentFactory) null
        );
    }

    @Test
    public void entrantHomeFragment_launchesWithoutCrash() {
        launchEntrantHome();
        onView(isRoot()).check(matches(isDisplayed()));
    }
}
