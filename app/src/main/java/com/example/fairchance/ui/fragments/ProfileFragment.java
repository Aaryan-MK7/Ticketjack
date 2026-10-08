package com.example.fairchance.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.models.User;
import com.example.fairchance.ui.RoleSelectionActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This fragment displays the entrant's profile information (name, email, phone).
 * It allows the user to:
 * 1. Update their profile information (US 01.02.02).
 * 2. Update their notification preferences (US 01.04.03).
 * 3. Log out of the application (Organizers/Admins only).
 * 4. Delete their profile and all associated data (US 01.02.04).
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private AuthRepository authRepository;
    private TextInputEditText etName, etEmail, etPhoneNumber, etRole;
    private Button btnSaveChanges, btnLogout, btnDeleteProfile;
    private ProgressBar progressBar;
    private SwitchCompat switchLotteryResults, switchOrganizerUpdates;
    private TextView tvNotificationPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entrant_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        etRole = view.findViewById(R.id.etRole);
        btnSaveChanges = view.findViewById(R.id.button_save_changes);
        btnLogout = view.findViewById(R.id.button_logout);
        btnDeleteProfile = view.findViewById(R.id.button_delete_profile);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        switchLotteryResults = view.findViewById(R.id.switch_lottery_results);
        switchOrganizerUpdates = view.findViewById(R.id.switch_organizer_updates);
        tvNotificationPreferences = view.findViewById(R.id.tv_notification_preferences);

        etRole.setEnabled(false);
        loadUserProfile();

        btnLogout.setOnClickListener(v -> logout());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
        btnDeleteProfile.setOnClickListener(v -> confirmDeleteProfile());
    }

    /**
     * Loads the current user's profile data from AuthRepository
     * and populates all the fields in the UI.
     */
    private void loadUserProfile() {
        setLoading(true);
        authRepository.getUserProfile(new AuthRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                etName.setText(user.getName());
                etEmail.setText(user.getEmail());
                etPhoneNumber.setText(user.getPhone());

                String role = user.getRole();
                if (role != null && !role.isEmpty()) {
                    String capitalizedRole = role.substring(0, 1).toUpperCase(Locale.ROOT) + role.substring(1);
                    etRole.setText(capitalizedRole);
                }

                // ROLE-BASED UI ADJUSTMENTS
                if ("entrant".equalsIgnoreCase(role)) {
                    // Entrants are anonymous/persistent users. They should NOT see a logout button.
                    btnLogout.setVisibility(View.GONE);

                    // Show notification preferences for Entrants
                    if (tvNotificationPreferences != null) tvNotificationPreferences.setVisibility(View.VISIBLE);
                    if (switchLotteryResults != null) switchLotteryResults.setVisibility(View.VISIBLE);
                    if (switchOrganizerUpdates != null) switchOrganizerUpdates.setVisibility(View.VISIBLE);

                } else {
                    // Organizers/Admins MUST be able to logout.
                    btnLogout.setVisibility(View.VISIBLE);

                    // Hide notification preferences for Organizers (not relevant to them)
                    if (tvNotificationPreferences != null) tvNotificationPreferences.setVisibility(View.GONE);
                    if (switchLotteryResults != null) switchLotteryResults.setVisibility(View.GONE);
                    if (switchOrganizerUpdates != null) switchOrganizerUpdates.setVisibility(View.GONE);
                }

                // Load switch preferences
                if (user.getNotificationPreferences() != null) {
                    Map<String, Boolean> prefs = user.getNotificationPreferences();
                    switchLotteryResults.setChecked(prefs.getOrDefault("lotteryResults", true));
                    switchOrganizerUpdates.setChecked(prefs.getOrDefault("organizerUpdates", true));
                } else {
                    switchLotteryResults.setChecked(true);
                    switchOrganizerUpdates.setChecked(true);
                }

                setLoading(false);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading profile: " + message);
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    /**
     * Validates input fields and saves all changes (name, email, phone, notifications)
     * to Firestore via the AuthRepository.
     */
    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            etName.setError(name.isEmpty() ? "Name cannot be empty" : null);
            etEmail.setError(email.isEmpty() ? "Email cannot be empty" : null);
            return;
        }

        Map<String, Object> notificationPrefs = new HashMap<>();
        boolean lotteryResultsEnabled = switchLotteryResults.isChecked();
        boolean organizerUpdatesEnabled = switchOrganizerUpdates.isChecked();

        notificationPrefs.put("lotteryResults", lotteryResultsEnabled);
        notificationPrefs.put("organizerUpdates", organizerUpdatesEnabled);

        boolean notificationsGloballyEnabled = lotteryResultsEnabled || organizerUpdatesEnabled;

        setLoading(true);

        authRepository.updateUserProfile(name, email, phone, notificationPrefs, new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {

                authRepository.updateNotificationStatus(notificationsGloballyEnabled, new AuthRepository.TaskCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        Log.e(TAG, "Error updating notification status/token: " + message);
                        Toast.makeText(getContext(), "Profile updated, but notification status failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Log.e(TAG, "Error updating profile: " + message);
                Toast.makeText(getContext(), "Failed to update: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows a confirmation dialog before deleting the user's profile.
     */
    private void confirmDeleteProfile() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to permanently delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Calls the AuthRepository to delete the user's Auth record and Firestore document.
     * On success, navigates back to the RoleSelectionActivity.
     */
    private void deleteProfile() {
        setLoading(true);
        authRepository.deleteCurrentUser(new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(getContext(), "Profile deleted.", Toast.LENGTH_SHORT).show();
                goToRoleSelection();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Log.e(TAG, "Error deleting profile: " + message);
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Signs the user out and navigates back to the RoleSelectionActivity.
     */
    private void logout() {
        authRepository.signOut();
        Toast.makeText(getContext(), "Logged out.", Toast.LENGTH_SHORT).show();
        goToRoleSelection();
    }

    /**
     * Navigates to the RoleSelectionActivity and clears the activity stack.
     */
    private void goToRoleSelection() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    /**
     * Helper method to show/hide the progress bar and disable/enable UI elements.
     * @param isLoading True to show loading state, false otherwise.
     */
    private void setLoading(boolean isLoading) {
        if (progressBar == null) return;

        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!isLoading);
        btnLogout.setEnabled(!isLoading);
        btnDeleteProfile.setEnabled(!isLoading);
        switchLotteryResults.setEnabled(!isLoading);
        switchOrganizerUpdates.setEnabled(!isLoading);
    }
}
