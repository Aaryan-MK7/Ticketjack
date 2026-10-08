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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the waiting list for an event.
 * Allows the organizer to:
 *  - View all waiting entrants
 *  - Send custom notifications to all waiting entrants
 *  - View their map locations (another fragment handles the map)
 */
public class EntrantsWaitingListFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";
    private static final String ARG_EVENT_NAME = "EVENT_NAME";

    private String eventId;
    private String eventName;

    private RecyclerView rvWaitingList;
    private Button btnSendNotification;
    private Button btnViewWaitingListMap;

    private SelectedParticipantAdapter adapter;
    private final List<String> waitingIds = new ArrayList<>();

    private EventRepository repository;

    public EntrantsWaitingListFragment() {}

    /**
     * Creates a new instance of this fragment with event ID + name.
     */
    public static EntrantsWaitingListFragment newInstance(String eventId, String eventName) {
        EntrantsWaitingListFragment fragment = new EntrantsWaitingListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_NAME, eventName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entrants_waiting_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve arguments
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            eventName = getArguments().getString(ARG_EVENT_NAME, "");
        }

        repository = new EventRepository();

        // UI setup
        rvWaitingList = view.findViewById(R.id.rvWaitingList);
        btnSendNotification = view.findViewById(R.id.btnSendNotification);
        btnViewWaitingListMap = view.findViewById(R.id.btnViewWaitingListMap);

        rvWaitingList.setLayoutManager(new LinearLayoutManager(getContext()));

        // Adapter shows a list of entrant IDs with NO action button
        adapter = new SelectedParticipantAdapter(
                waitingIds,
                eventName,
                "",
                false,
                null
        );
        rvWaitingList.setAdapter(adapter);

        loadWaitingList();

        // Sends a custom message to all waiting entrants
        btnSendNotification.setOnClickListener(v -> showWaitingNotificationDialog());

        // Opens the map + list fragment
        btnViewWaitingListMap.setOnClickListener(v -> {
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(getContext(),
                        "Event not loaded.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            WaitingListMapFragment fragment =
                    WaitingListMapFragment.newInstance(eventId, eventName);

            // Replace container with the map screen
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dashboard_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    /**
     * Loads the waiting list from Firestore:
     * events/{eventId}/waitingList
     * Each document ID = userId of an entrant.
     */
    private void loadWaitingList() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event id provided for waiting list.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    waitingIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        waitingIds.add(doc.getId());
                    }

                    adapter.setEventName(eventName);
                    adapter.notifyDataSetChanged();

                    if (waitingIds.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No entrants on the waiting list yet.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                getContext(),
                                "Failed to load waiting list: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /**
     * Shows a dialog where the organizer types a custom message,
     * then sends it to all waiting entrants through EventRepository.
     */
    private void showWaitingNotificationDialog() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Event not loaded.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_send_notif_waiting, null, false);

        TextInputEditText etMessage = dialogView.findViewById(R.id.etNotificationText);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String message = etMessage.getText() != null
                    ? etMessage.getText().toString().trim()
                    : "";

            if (message.isEmpty()) {
                etMessage.setError("Message cannot be empty");
                return;
            }

            dialog.dismiss();

            repository.sendWaitingListNotifications(
                    eventId,
                    message,
                    new EventRepository.NotificationCallback() {
                        @Override
                        public void onSuccess(int sentCount, int failureCount) {
                            Toast.makeText(
                                    getContext(),
                                    "Notifications sent: " + sentCount +
                                            " (failures: " + failureCount + ")",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(
                                    getContext(),
                                    "Error sending notifications: " + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        });

        dialog.show();
    }
}
