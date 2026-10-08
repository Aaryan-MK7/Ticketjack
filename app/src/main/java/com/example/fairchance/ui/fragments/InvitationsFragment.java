package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Invitation;
import com.example.fairchance.ui.adapters.InvitationAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment displays a list of pending event invitations for the entrant.
 * It shows events where the user's status is "Selected" (i.e., they won the lottery)
 * and allows them to "Accept" or "Decline" the invitation (US 01.05.02, 01.05.03).
 */
public class InvitationsFragment extends Fragment {

    private static final String TAG = "InvitationsFragment";

    private RecyclerView invitationsRecyclerView;
    private InvitationAdapter invitationAdapter;
    private List<Invitation> invitationList = new ArrayList<>();
    private EventRepository eventRepository;

    private ProgressBar progressBar;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invitations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();

        invitationsRecyclerView = view.findViewById(R.id.invitations_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        invitationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        invitationAdapter = new InvitationAdapter(getContext(), invitationList, eventRepository);
        invitationsRecyclerView.setAdapter(invitationAdapter);

        loadInvitations();
    }

    /**
     * Fetches the list of pending invitations from the EventRepository
     * (where status == "Selected") and populates the RecyclerView.
     */
    private void loadInvitations() {
        showLoading(true);
        eventRepository.getPendingInvitations(new EventRepository.InvitationListCallback() {
            @Override
            public void onSuccess(List<Invitation> items) {
                showLoading(false);
                if (items.isEmpty()) {
                    showEmptyView(true);
                } else {
                    showEmptyView(false);
                    invitationList.clear();
                    invitationList.addAll(items);
                    invitationAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Log.e(TAG, "Error loading invitations: " + message);
                Toast.makeText(getContext(), "Failed to load invitations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Helper method to show/hide the main progress bar.
     * @param isLoading True to show loading state, false otherwise.
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            invitationsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method to show/hide the "No pending invitations" text.
     * @param show True to show the empty view, false to show the RecyclerView.
     */
    private void showEmptyView(boolean show) {
        if (show) {
            emptyView.setVisibility(View.VISIBLE);
            invitationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            invitationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
