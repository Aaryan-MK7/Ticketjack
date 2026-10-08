package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.models.AdminUserItem;
import com.example.fairchance.ui.adapters.AdminUserAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment allowing Administrators to browse and manage Organizer profiles.
 * Displays a list of users with the "organizer" role and provides search functionality.
 * Clicking a user allows the admin to view details and potentially remove them.
 *
 * Implements User Stories:
 * - US 03.05.01 (Browse profiles)
 * - US 03.07.01 (Remove organizers that violate policy)
 */
public class AdminOrganizerManagementFragment extends Fragment
        implements AdminUserAdapter.OnUserClickListener {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearch;

    private AdminUserAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Inflates the layout for the Admin Organizer Management screen.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_organizer_management, container, false);
    }

    /**
     * Initializes the RecyclerView, Search functionality, and loads the initial list of organizers.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btnBack);
        rvUsers = view.findViewById(R.id.rvAdminUsers);
        progressBar = view.findViewById(R.id.progressBarUsers);
        tvEmpty = view.findViewById(R.id.tvEmptyUsers);
        etSearch = view.findViewById(R.id.etSearchUsers);

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminUserAdapter(this);
        rvUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadOrganizers();
    }

    /**
     * Fetches all users from Firestore, filters for those with the 'organizer' role,
     * and updates the RecyclerView adapter.
     */
    private void loadOrganizers() {
        setLoading(true);
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    setLoading(false);
                    List<AdminUserItem> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");

                        if (role == null || !role.equalsIgnoreCase("organizer")) {
                            continue;
                        }

                        Boolean isActive = doc.getBoolean("isActive");
                        Boolean roleActive = doc.getBoolean("roleActive");
                        if (isActive != null && !isActive) continue;
                        if (roleActive != null && !roleActive) continue;

                        com.google.firebase.Timestamp createdAt =
                                doc.getTimestamp("timeCreated");

                        list.add(new AdminUserItem(id, name, email, role, createdAt));
                    }

                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvUsers.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvUsers.setVisibility(View.VISIBLE);
                    }

                    adapter.submitList(list);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvUsers.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error loading organizers: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Toggles the loading indicator visibility.
     *
     * @param loading True to show the progress bar, False to hide it.
     */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvUsers.setEnabled(!loading);
    }

    /**
     * Callback method triggered when an organizer item in the list is clicked.
     * Navigates to the AdminOrganizerDetailsFragment.
     *
     * @param user The AdminUserItem that was clicked.
     */
    @Override
    public void onUserClick(AdminUserItem user) {
        Fragment details = AdminOrganizerDetailsFragment.newInstance(user.getId());
        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        ft.replace(R.id.dashboard_container, details);
        ft.addToBackStack(null);
        ft.commit();
    }

}
