package com.example.fairchance.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.fairchance.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Dashboard for organizers: create events, view current events, access tools, or log out.
 */
public class OrganizerDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.organizer_dashboard, container, false);

        Button btnCreateEvent = view.findViewById(R.id.btnCreateNewEvent);
        Button btnCurrentEvents = view.findViewById(R.id.btnCurrentEvents);
        Button btnLottery = view.findViewById(R.id.btnLottery);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        btnCreateEvent.setOnClickListener(v ->
                openFragment(new CreateNewEventFragment())
        );

        btnCurrentEvents.setOnClickListener(v ->
                openFragment(new OngoingEventsFragment())
        );

        btnLottery.setOnClickListener(v -> {
            // TODO: link to lottery feature
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(requireContext(),
                    com.example.fairchance.ui.RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            requireActivity().finish();
        });

        return view;
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();

        transaction.replace(R.id.dashboard_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
