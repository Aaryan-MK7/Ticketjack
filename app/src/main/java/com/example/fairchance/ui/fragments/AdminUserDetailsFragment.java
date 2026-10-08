package com.example.fairchance.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Displays detailed information about a specific user.
 * Provides the functionality for Administrators to remove a user profile (US 03.02.01).
 */
public class AdminUserDetailsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    /**
     * Factory method to create a new instance of this fragment with a specific user ID.
     *
     * @param userId The ID of the user to display.
     * @return A new instance of AdminUserDetailsFragment.
     */
    public static AdminUserDetailsFragment newInstance(String userId) {
        AdminUserDetailsFragment frag = new AdminUserDetailsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USER_ID, userId);
        frag.setArguments(b);
        return frag;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat df =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    private final AuthRepository authRepository = new AuthRepository();

    private TextView tvName, tvEmail, tvPhone, tvRole, tvCreated;
    private ProgressBar progressBar;
    private View btnRemoveUser;

    private String userId;

    /**
     * Inflates the user details layout.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view.
     * @param savedInstanceState Previous state bundle.
     * @return The inflated view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_details, container, false);
    }

    /**
     * Binds UI references and initiates loading of user details.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState Previous state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvDetailName);
        tvEmail = view.findViewById(R.id.tvDetailEmail);
        tvPhone = view.findViewById(R.id.tvDetailPhone);
        tvRole = view.findViewById(R.id.tvDetailRole);
        tvCreated = view.findViewById(R.id.tvDetailCreated);
        progressBar = view.findViewById(R.id.progressBarUserDetail);
        btnRemoveUser = view.findViewById(R.id.btnRemoveUser);

        userId = getArguments() != null ? getArguments().getString(ARG_USER_ID) : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "Missing user id", Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        btnRemoveUser.setOnClickListener(v -> confirmRemoveUser());

        loadUser(userId);
    }

    /**
     * Fetches the user document from Firestore.
     *
     * @param id The ID of the user to load.
     */
    private void loadUser(String id) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(id)
                .get()
                .addOnSuccessListener(this::bindUser)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error loading user: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Populates the UI fields with data from the user document.
     *
     * @param doc The Firestore document snapshot.
     */
    private void bindUser(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        String name = doc.getString("name");
        String email = doc.getString("email");
        String role = doc.getString("role");
        String phone = doc.getString("phone");

        tvName.setText(name != null ? name : "N/A");
        tvEmail.setText(email != null ? "Email: " + email : "Email: N/A");
        tvRole.setText(role != null ? "Role: " + role : "Role: N/A");
        tvPhone.setText(phone != null ? "Phone: " + phone : "Phone: N/A");

        com.google.firebase.Timestamp ts = doc.getTimestamp("timeCreated");
        if (ts != null) {
            tvCreated.setText("Registered: " + df.format(ts.toDate()));
        } else {
            tvCreated.setText("Registered: N/A");
        }
    }

    /**
     * Displays a confirmation dialog before executing the profile removal.
     */
    private void confirmRemoveUser() {
        if (userId == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove profile")
                .setMessage("Are you sure you want to remove this user profile?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> performRemoval())
                .show();
    }

    /**
     * Executes the actual removal logic via the AuthRepository.
     */
    private void performRemoval() {
        progressBar.setVisibility(View.VISIBLE);
        btnRemoveUser.setEnabled(false);

        authRepository.deleteUserProfileById(userId, new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Profile removed.",
                        Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                btnRemoveUser.setEnabled(true);
                Toast.makeText(requireContext(),
                        "Error removing profile: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
