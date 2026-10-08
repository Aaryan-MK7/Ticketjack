package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.adapters.EventAdapter;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows all ongoing events created by the currently logged-in organizer.
 * Uses a real-time Firestore listener so updates appear immediately.
 */
public class OngoingEventsFragment extends Fragment {

    private RecyclerView rvOngoingEvents;
    private EventAdapter adapter;
    private EventRepository repository;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ongoing_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvOngoingEvents = view.findViewById(R.id.rvOngoingEvents);
        rvOngoingEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        // true = adapter is in "organizer mode" (shows admin/organizer controls)
        adapter = new EventAdapter(new ArrayList<>(), true);
        rvOngoingEvents.setAdapter(adapter);

        repository = new EventRepository();

        // Get the logged-in organizer ID
        String organizerId = null;
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            organizerId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (organizerId == null) {
            Toast.makeText(getContext(), "Not logged in as organizer.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Subscribe to real-time updates for this organizer's events
        registration = repository.getEventsForOrganizer(
                organizerId,
                new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        adapter.setEvents(events);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(),
                                "Error loading events: " + message,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Remove Firestore listener to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
    }
}
