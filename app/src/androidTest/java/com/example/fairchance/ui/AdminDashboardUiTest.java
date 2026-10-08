package com.example.fairchance.ui;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.fairchance.R;
import com.example.fairchance.ui.fragments.AdminDashboardFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AdminDashboardUiTest {

    private FragmentScenario<AdminDashboardFragment> launchAdminDashboard() {
        return FragmentScenario.launchInContainer(
                AdminDashboardFragment.class,
                null,
                R.style.Theme_FairChance,
                (FragmentFactory) null
        );
    }

    @Test
    public void adminDashboard_showsHeaderAndStats() {
        launchAdminDashboard();

        onView(withId(R.id.tvActiveEventsCount))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.tvUserProfilesCount))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.tvUploadedImagesCount))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.tvOrganizersCount))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    public void adminDashboard_showsManagementCards() {
        launchAdminDashboard();

        onView(withId(R.id.cardNotifications))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.cardEventManagement))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.cardProfileManagement))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.cardImageManagement))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.cardOrganizerManagement))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    public void adminDashboard_logoutButtonVisible() {
        launchAdminDashboard();

        onView(withId(R.id.btnLogout))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }
}
