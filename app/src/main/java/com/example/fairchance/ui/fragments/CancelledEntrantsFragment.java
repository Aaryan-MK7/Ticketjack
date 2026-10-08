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
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Organizer view for listing entrants who have cancelled or declined an invitation.
 * Implements US 02.06.02 (View cancelled entrants) and supports sending notifications to them (US 02.07.03).
 */
public class CancelledEntrantsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";

    private String eventId;
    private EventRepository repository;

    private RecyclerView rvCancelledEntrants;
    private SelectedParticipantAdapter adapter;
    private final List<String> cancelledIds = new ArrayList<>();

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public CancelledEntrantsFragment() {
    }

    /**
     * Creates a new instance of the fragment for a specific event.
     *
     * @param eventId The unique ID of the event.
     * @return New CancelledEntrantsFragment instance.
     */
    public static CancelledEntrantsFragment newInstance(String eventId) {
        CancelledEntrantsFragment fragment = new CancelledEntrantsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Inflates the cancelled entrants list layout.
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
        return inflater.inflate(R.layout.cancelled_entrants_list, container, false);
    }

    /**
     * Initializes the RecyclerView adapter and triggers data loading.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState Previous state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        rvCancelledEntrants = view.findViewById(R.id.rvCancelledEntrants);
        rvCancelledEntrants.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SelectedParticipantAdapter(
                cancelledIds,
                "",
                "",
                false,
                null
        );
        rvCancelledEntrants.setAdapter(adapter);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Missing event id.", Toast.LENGTH_SHORT).show();
        } else {
            loadEventMeta();
            loadCancelledEntrants();
        }

        Button btnNotify = view.findViewById(R.id.btnNotifyCancelledEntrants);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v -> {
                if (eventId == null || eventId.isEmpty()) {
                    Toast.makeText(getContext(), "Event not loaded.", Toast.LENGTH_SHORT).show();
                    return;
                }
                showSendNotificationDialog();
            });
        }
    }

    /**
     * Loads event metadata (like name) to display in the UI title or adapter.
     */
    private void loadEventMeta() {
        repository.getEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                View root = getView();
                if (root != null) {
                    android.widget.TextView title = root.findViewById(R.id.tvCancelledEntrantsTitle);
                    if (title != null && event.getName() != null) {
                        title.setText(event.getName() + " – Cancelled Entrants' List");
                    }
                }

                if (event.getName() != null) {
                    adapter.setEventName(event.getName());
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(),
                        "Failed to load event: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Fetches the list of user IDs from the 'cancelled' sub-collection in Firestore.
     */
    private void loadCancelledEntrants() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .document(eventId)
                .collection("cancelled")
                .get()
                .addOnSuccessListener(snapshot -> {
                    cancelledIds.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        cancelledIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                getContext(),
                                "Failed to load cancelled entrants: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /**
     * Displays a dialog allowing the organizer to compose and send a notification
     * to all cancelled entrants.
     */
    private void showSendNotificationDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_send_notif_cancelled, null, false);

        TextInputEditText etMessage = dialogView.findViewById(R.id.etNotificationText);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnConfirm.setOnClickListener(v -> {
            String message = etMessage != null ? etMessage.getText().toString().trim() : "";
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message.", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.sendCancelledNotifications(
                    eventId,
                    message,
                    new EventRepository.NotificationCallback() {
                        @Override
                        public void onSuccess(int sentCount, int failureCount) {
                            Toast.makeText(
                                    getContext(),
                                    "Sent to " + sentCount + " entrant(s). Failures: " + failureCount,
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(
                                    getContext(),
                                    "Error: " + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
