package com.example.fairchance.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.ui.RoleSelectionActivity;
import com.example.fairchance.ui.fragments.AdminDashboardFragment;
import com.example.fairchance.ui.fragments.EntrantHomeFragment;
import com.example.fairchance.ui.fragments.HistoryFragment;
import com.example.fairchance.ui.fragments.InvitationsFragment;
import com.example.fairchance.ui.fragments.OrganizerDashboardFragment;
import com.example.fairchance.ui.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The main container activity for the app after a user is logged in.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Fetch the user's role (Entrant, Organizer, Admin) via {@link AuthRepository}.</li>
 *     <li>Load the appropriate dashboard fragment based on the role.</li>
 *     <li>For Entrants and Organizers, manage the {@link BottomNavigationView}.</li>
 *     <li>Request notification permission on Android 13+ when needed.</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private BottomNavigationView bottomNav;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("MainActivity", "Notification permission granted");
                } else {
                    Log.w("MainActivity", "Notification permission denied");
                    Toast.makeText(
                            this,
                            "Notifications disabled. You may miss event invites.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the {@link AuthRepository}, sets up the bottom navigation,
     * requests notification permission when needed, and then fetches the user's
     * role to determine which UI to display.
     *
     * @param savedInstanceState previously saved instance state, or {@code null}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authRepository = new AuthRepository();
        bottomNav = findViewById(R.id.bottom_navigation);

        askNotificationPermission();

        authRepository.getUserRole(new AuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                switch (role) {
                    case "entrant":
                        bottomNav.setVisibility(View.VISIBLE);
                        setupEntrantNavigation();

                        // Special navigation target when launched from a notification
                        if (getIntent().hasExtra("NAV_TO_INVITATIONS")) {
                            loadDashboardFragment(new InvitationsFragment());
                            bottomNav.setSelectedItemId(R.id.nav_invitations);
                        } else {
                            loadDashboardFragment(new EntrantHomeFragment());
                            bottomNav.setSelectedItemId(R.id.nav_home);
                        }
                        break;
                    case "organizer":
                        bottomNav.setVisibility(View.VISIBLE);
                        setupOrganizerNavigation();
                        loadDashboardFragment(new OrganizerDashboardFragment());
                        bottomNav.setSelectedItemId(R.id.nav_home);
                        break;
                    case "admin":
                        bottomNav.setVisibility(View.GONE);
                        loadDashboardFragment(new AdminDashboardFragment());
                        break;
                    default:
                        Log.e("MainActivity", "Unknown role, defaulting to entrant.");
                        bottomNav.setVisibility(View.VISIBLE);
                        setupEntrantNavigation();
                        loadDashboardFragment(new EntrantHomeFragment());
                        bottomNav.setSelectedItemId(R.id.nav_home);
                        break;
                }
            }

            @Override
            public void onError(String message) {
                Log.e("MainActivity", "Error fetching role: " + message);
                logout();
            }
        });
    }

    /**
     * Checks and, if necessary, requests {@link Manifest.permission#POST_NOTIFICATIONS}
     * on Android 13+ (API 33 and above).
     * <p>
     * On earlier Android versions, this method does nothing.
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Configures the {@link BottomNavigationView} for the Entrant role:
     * inflates the entrant menu, and sets up navigation between
     * Home, Invitations, History, and Profile fragments.
     */
    private void setupEntrantNavigation() {
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.entrant_nav_menu);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new EntrantHomeFragment();
            } else if (itemId == R.id.nav_invitations) {
                selectedFragment = new InvitationsFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadDashboardFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Configures the {@link BottomNavigationView} for the Organizer role:
     * inflates the organizer menu, and sets up navigation between
     * the organizer dashboard and profile.
     */
    private void setupOrganizerNavigation() {
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.organizer_bottom_nav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new OrganizerDashboardFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadDashboardFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Replaces the content of {@code R.id.dashboard_container} with the given fragment.
     *
     * @param fragment the {@link Fragment} to display in the main dashboard area
     */
    private void loadDashboardFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.dashboard_container, fragment);
        transaction.commit();
    }

    /**
     * Logs out the current user via {@link AuthRepository}, clears the activity stack,
     * and navigates back to {@link RoleSelectionActivity}.
     */
    private void logout() {
        authRepository.signOut();
        Toast.makeText(MainActivity.this, "Logged out.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
