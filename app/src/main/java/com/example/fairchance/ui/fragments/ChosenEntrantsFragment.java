package com.example.fairchance.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Organizer view for managing entrants who have been selected (chosen) via the lottery.
 * Implements US 02.06.01 (View chosen entrants).
 * Implements US 02.07.02 (Send notifications to chosen entrants).
 */
public class ChosenEntrantsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";
    private static final String ARG_EVENT_NAME = "EVENT_NAME";
    private String eventId;
    private String eventName = "";
    private FirebaseFunctions functions;
    private EventRepository repository;

    private RecyclerView rvChosenEntrants;
    private SelectedParticipantAdapter adapter;
    private final List<String> chosenIds = new ArrayList<>();

    /**
     * Required empty public constructor.
     */
    public ChosenEntrantsFragment() {
    }

    /**
     * Creates a new instance of the fragment.
     *
     * @param eventId   The ID of the event.
     * @param eventName The name of the event.
     * @return New fragment instance.
     */
    public static ChosenEntrantsFragment newInstance(String eventId, String eventName) {
        ChosenEntrantsFragment f = new ChosenEntrantsFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        args.putString("EVENT_NAME", eventName);
        f.setArguments(args);
        return f;
    }

    /**
     * Inflates the chosen entrants list layout.
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
        return inflater.inflate(R.layout.chosen_entrants_list, container, false);
    }

    /**
     * Initializes the view components and adapter, and loads the list of entrants.
     *
     * @param view               The created view.
     * @param savedInstanceState Previous state.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            eventName = getArguments().getString("EVENT_NAME", "");
        }

        functions = FirebaseFunctions.getInstance();
        repository = new EventRepository();

        rvChosenEntrants = view.findViewById(R.id.rvChosenEntrants);
        rvChosenEntrants.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SelectedParticipantAdapter(
                chosenIds,
                eventName,
                "",
                false,
                null
        );

        rvChosenEntrants.setAdapter(adapter);

        Button btnSendNotification = view.findViewById(R.id.btnSendNotification);
        btnSendNotification.setOnClickListener(v -> onSendNotificationsClicked());

        Button btnCancelPending = view.findViewById(R.id.btnCancelPending);
        btnCancelPending.setOnClickListener(v -> onCancelPendingClicked());

        loadChosenEntrants();
    }

    /**
     * Fetches the 'selected' sub-collection for the event.
     */
    private void loadChosenEntrants() {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(this::onChosenLoaded)
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Failed to load chosen entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Updates the adapter with the list of user IDs retrieved from Firestore.
     *
     * @param snap The query snapshot containing entrant documents.
     */
    private void onChosenLoaded(QuerySnapshot snap) {
        chosenIds.clear();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            chosenIds.add(doc.getId());
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Triggers a Cloud Function to send "You Won!" notifications to all selected entrants.
     */
    private void onSendNotificationsClicked() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event ID found for notifications.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);

        Task<Object> task = functions
                .getHttpsCallable("sendChosenNotifications")
                .call(data)
                .continueWith(t -> {
                    HttpsCallableResult result = t.getResult();
                    return result != null ? result.getData() : null;
                });

        task.addOnSuccessListener(resultObj -> {
            if (getContext() == null) return;

            long sent = 0;
            long failed = 0;

            if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> res = (Map<String, Object>) resultObj;
                Object s = res.get("sentCount");
                Object f = res.get("failureCount");
                if (s instanceof Number) sent = ((Number) s).longValue();
                if (f instanceof Number) failed = ((Number) f).longValue();
            }

            String msg = "Notifications sent: " + sent +
                    " success, " + failed + " failed.";
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

            loadChosenEntrants();
        });

        task.addOnFailureListener(e -> {
            if (getContext() == null) return;
            Toast.makeText(getContext(),
                    "Failed to send notifications: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Shows a confirmation dialog, then calls repository to cancel all pending entrants.
     */
    private void onCancelPendingClicked() {
        if (eventId == null || eventId.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Pending Entrants")
                .setMessage("Are you sure you want to cancel all entrants who have not yet signed up? This action moves them to the Cancelled list and cannot be undone.")
                .setPositiveButton("Yes, Cancel All", (dialog, which) -> {
                    repository.cancelPendingEntrants(eventId, new EventRepository.EventTaskCallback() {
                        @Override
                        public void onSuccess() {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "All pending entrants cancelled.", Toast.LENGTH_SHORT).show();
                            loadChosenEntrants();
                        }

                        @Override
                        public void onError(String message) {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }
}
