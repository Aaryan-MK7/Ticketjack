package com.example.fairchance.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.data.repository.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Displays details for a specific Organizer.
 * Implements US 03.07.01 by providing the functionality to remove an organizer
 * who violates app policy.
 */
public class AdminOrganizerDetailsFragment extends Fragment {

    private static final String ARG_ORGANIZER_ID = "organizer_id";

    /**
     * Factory method to create an instance with the organizer ID.
     *
     * @param organizerId The ID of the organizer.
     * @return New fragment instance.
     */
    public static AdminOrganizerDetailsFragment newInstance(String organizerId) {
        AdminOrganizerDetailsFragment frag = new AdminOrganizerDetailsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ORGANIZER_ID, organizerId);
        frag.setArguments(b);
        return frag;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat df =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    private String organizerId;

    private TextView tvName, tvEmail, tvPhone, tvRole, tvCreated;
    private ProgressBar progressBar;

    private AuthRepository authRepository;
    private EventRepository eventRepository;

    /**
     * Inflates the organizer details layout.
     *
     * @param inflater           The LayoutInflater.
     * @param container          The parent view.
     * @param savedInstanceState Previous state.
     * @return The inflated view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_organizer_details, container, false);
    }

    /**
     * Binds UI components and triggers data loading.
     *
     * @param view               The created view.
     * @param savedInstanceState Previous state.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        organizerId = getArguments() != null ? getArguments().getString(ARG_ORGANIZER_ID) : null;
        if (organizerId == null) {
            Toast.makeText(requireContext(), "Missing organizer ID", Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        authRepository = new AuthRepository();
        eventRepository = new EventRepository();

        tvName = view.findViewById(R.id.tvDetailName);
        tvEmail = view.findViewById(R.id.tvDetailEmail);
        tvPhone = view.findViewById(R.id.tvDetailPhone);
        tvRole = view.findViewById(R.id.tvDetailRole);
        tvCreated = view.findViewById(R.id.tvDetailCreated);
        progressBar = view.findViewById(R.id.progressBarUserDetail);

        View btnBack = view.findViewById(R.id.btnBack);
        View btnRemove = view.findViewById(R.id.btnRemoveOrganizer);

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        btnRemove.setOnClickListener(v -> confirmRemoveOrganizer());

        loadOrganizer();
    }

    /**
     * Loads the organizer's profile data from Firestore.
     */
    private void loadOrganizer() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .document(organizerId)
                .get()
                .addOnSuccessListener(this::bindOrganizer)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error loading organizer: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Populates the view with organizer data.
     *
     * @param doc The Firestore document.
     */
    private void bindOrganizer(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(requireContext(), "Organizer not found", Toast.LENGTH_LONG).show();
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

        Timestamp ts = doc.getTimestamp("timeCreated");
        if (ts != null) {
            tvCreated.setText("Registered: " + df.format(ts.toDate()));
        } else {
            tvCreated.setText("Registered: N/A");
        }
    }

    /**
     * Shows a confirmation dialog requesting a reason for removal.
     */
    private void confirmRemoveOrganizer() {

        final EditText input = new EditText(requireContext());
        input.setHint("Reason for removal (optional)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Organizer")
                .setMessage("This will deactivate the organizer and all their events.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) reason = "Policy violation";
                    performRemoval(reason);
                })
                .show();
    }

    /**
     * Performs the deactivation of the organizer and cascades the deactivation
     * to all events owned by them.
     *
     * @param reason The administrative reason for removal.
     */
    private void performRemoval(String reason) {
        progressBar.setVisibility(View.VISIBLE);

        String adminId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown";

        authRepository.deactivateOrganizer(organizerId, reason, new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {

                eventRepository.deactivateEventsByOrganizer(
                        organizerId,
                        adminId,
                        reason,
                        new EventRepository.EventTaskCallback() {

                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(),
                                        "Organizer and their events deactivated.",
                                        Toast.LENGTH_LONG).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }

                            @Override
                            public void onError(String message) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(),
                                        "Organizer removed but failed to deactivate some events: "
                                                + message,
                                        Toast.LENGTH_LONG).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }
                        }
                );
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Error deactivating organizer: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
