package com.example.fairchance.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.ui.RoleSelectionActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * The main entry point for the Administrator actor.
 * This fragment provides a dashboard overview of system statistics (events, users, images)
 * and navigation to specific administrative functions defined in US 03.xx.xx.
 */
public class AdminDashboardFragment extends Fragment {

    private TextView tvActiveEventsCount;
    private TextView tvUserProfilesCount;
    private TextView tvUploadedImagesCount;
    private TextView tvOrganizersCount;

    private ListenerRegistration eventsListener;
    private ListenerRegistration usersListener;
    private ListenerRegistration imagesListener;
    private ListenerRegistration organizersListener;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Inflates the admin dashboard layout.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.admin_main_dashboard, container, false);
    }

    /**
     * Initializes the dashboard UI components, sets up navigation click listeners for
     * the management subsystems, and begins listening for live statistic updates.
     *
     * @param root               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(
            @NonNull View root,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(root, savedInstanceState);

        tvActiveEventsCount   = root.findViewById(R.id.tvActiveEventsCount);
        tvUserProfilesCount   = root.findViewById(R.id.tvUserProfilesCount);
        tvUploadedImagesCount = root.findViewById(R.id.tvUploadedImagesCount);
        tvOrganizersCount     = root.findViewById(R.id.tvOrganizersCount);

        CardView cardProfile            = root.findViewById(R.id.cardProfileManagement);
        CardView cardEvent              = root.findViewById(R.id.cardEventManagement);
        CardView cardImage              = root.findViewById(R.id.cardImageManagement);
        CardView cardNotifications      = root.findViewById(R.id.cardNotifications);
        CardView cardOrganizerManagement= root.findViewById(R.id.cardOrganizerManagement);

        Button btnLogout = root.findViewById(R.id.btnLogout);

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v ->
                    openFragment(new AdminProfileManagementFragment()));
        }

        if (cardEvent != null) {
            cardEvent.setOnClickListener(v ->
                    openFragment(new AdminEventManagementFragment()));
        }

        if (cardImage != null) {
            cardImage.setOnClickListener(v ->
                    openFragment(new AdminImageManagementFragment()));
        }

        if (cardNotifications != null) {
            cardNotifications.setOnClickListener(v ->
                    openFragment(new AdminNotificationFragment()));
        }

        if (cardOrganizerManagement != null) {
            cardOrganizerManagement.setOnClickListener(v ->
                    openFragment(new AdminOrganizerManagementFragment()));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AuthRepository().signOut();

                Intent intent = new Intent(requireContext(), RoleSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                requireActivity().finish();
            });
        }

        startDashboardListeners();
    }

    /**
     * Attaches Firestore listeners to the 'events' and 'users' collections to provide
     * real-time counts of active entities on the dashboard tiles.
     */
    private void startDashboardListeners() {
        eventsListener = db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    int count = 0;
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Boolean isActive = doc.getBoolean("isActive");
                            if (isActive == null || Boolean.TRUE.equals(isActive)) {
                                count++;
                            }
                        }
                    }
                    if (tvActiveEventsCount != null) {
                        tvActiveEventsCount.setText(String.valueOf(count));
                    }
                });

        usersListener = db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (tvUserProfilesCount != null) {
                        tvUserProfilesCount.setText(
                                value != null ? String.valueOf(value.size()) : "0"
                        );
                    }
                });

        imagesListener = db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    int imgCount = 0;
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String poster = doc.getString("posterImageUrl");
                            if (poster != null && !poster.isEmpty()) {
                                imgCount++;
                            }
                        }
                    }

                    if (tvUploadedImagesCount != null) {
                        tvUploadedImagesCount.setText(String.valueOf(imgCount));
                    }
                });

        organizersListener = db.collection("users")
                .whereEqualTo("role", "organizer")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (tvOrganizersCount != null) {
                        tvOrganizersCount.setText(
                                value != null ? String.valueOf(value.size()) : "0"
                        );
                    }
                });
    }

    /**
     * Helper method to navigate to a sub-fragment within the Admin interface.
     *
     * @param fragment The destination fragment to replace the current view with.
     */
    private void openFragment(Fragment fragment) {
        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        ft.replace(R.id.dashboard_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Cleans up Firestore listeners when the view is destroyed to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (eventsListener != null)     eventsListener.remove();
        if (usersListener != null)      usersListener.remove();
        if (imagesListener != null)     imagesListener.remove();
        if (organizersListener != null) organizersListener.remove();
    }
}
