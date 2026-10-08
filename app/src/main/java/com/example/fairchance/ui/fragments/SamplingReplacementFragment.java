package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen where the organizer can sample N attendees for a specific event
 * and draw replacements for cancelled entrants.
 */
public class SamplingReplacementFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";

    private EventRepository repository;
    private String eventId;
    private String eventName = "Name of Event";

    private TextInputLayout tilSampleNumber;
    private TextInputEditText etSampleNumber;
    private Button btnSampleAttendees;

    private RecyclerView rvSelectedParticipants;
    private SelectedParticipantAdapter selectedAdapter;
    private final List<String> selectedIds = new ArrayList<>();

    private RecyclerView rvReplacementPool;
    private SelectedParticipantAdapter replacementAdapter;
    private final List<String> replacementIds = new ArrayList<>();

    private TextView tvSummary;

    private FirebaseFunctions functions;

    public SamplingReplacementFragment() { }

    public static SamplingReplacementFragment newInstance(String eventId) {
        SamplingReplacementFragment fragment = new SamplingReplacementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sampling_replacement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();
        functions = FirebaseFunctions.getInstance();

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        tilSampleNumber = view.findViewById(R.id.tilSampleNumber);
        etSampleNumber = view.findViewById(R.id.etSampleNumber);
        btnSampleAttendees = view.findViewById(R.id.btnSampleAttendees);
        tvSummary = view.findViewById(R.id.tvSummary);

        rvSelectedParticipants = view.findViewById(R.id.rvSelectedParticipants);
        rvSelectedParticipants.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedAdapter = new SelectedParticipantAdapter(
                selectedIds,
                eventName,
                "Notify Entrant",
                true,
                entrantId -> notifySingleEntrant(entrantId)
        );
        rvSelectedParticipants.setAdapter(selectedAdapter);

        rvReplacementPool = view.findViewById(R.id.rvReplacementPool);
        rvReplacementPool.setLayoutManager(new LinearLayoutManager(getContext()));
        replacementAdapter = new SelectedParticipantAdapter(
                replacementIds,
                eventName,
                "Draw Replacement",
                true,
                entrantId -> onDrawReplacementClicked(entrantId)
        );
        rvReplacementPool.setAdapter(replacementAdapter);

        btnSampleAttendees.setOnClickListener(v -> onSampleClicked());

        if (eventId != null && !eventId.isEmpty()) {
            loadEventName();
            loadSelectedParticipants(0);
            loadReplacementPool();
        }
    }

    /** Load the event's name so the cards can show it. */
    private void loadEventName() {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            eventName = name;
                            selectedAdapter.setEventName(eventName);
                            replacementAdapter.setEventName(eventName);
                        }
                    }
                });
    }

    private void onSampleClicked() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event selected for sampling.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        tilSampleNumber.setError(null);

        String text = etSampleNumber.getText() != null
                ? etSampleNumber.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(text)) {
            tilSampleNumber.setError("Please enter a number.");
            return;
        }

        int requested;
        try {
            requested = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            tilSampleNumber.setError("Invalid number.");
            return;
        }

        if (requested <= 0) {
            tilSampleNumber.setError("Number must be greater than 0.");
            return;
        }

        repository.sampleAttendees(eventId, requested,
                new EventRepository.EventTaskCallback() {
                    @Override
                    public void onSuccess() {
                        if (getContext() == null) return;
                        loadSelectedParticipants(requested);
                        loadReplacementPool();
                    }

                    @Override
                    public void onError(String message) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(),
                                "Sampling failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Load all currently selected entrants (any status). */
    private void loadSelectedParticipants(int requested) {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(query -> {
                    if (getContext() == null) return;

                    selectedIds.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        selectedIds.add(doc.getId()); // doc ID is userId
                    }
                    selectedAdapter.notifyDataSetChanged();

                    int selectedCount = selectedIds.size();

                    if (requested > 0) {
                        String msg = "Selected " + selectedCount +
                                " entrant(s) (requested " + requested + ").";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

                        tvSummary.setText("Last draw: requested " + requested +
                                ", selected " + selectedCount + " entrant(s).");
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Failed to load selected entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Replacement pool = all entries in "selected" where status == "cancelled".
     * These are the people who gave up their spot and need replacements.
     */
    private void loadReplacementPool() {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null) return;

                    replacementIds.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        Boolean replacementDrawn = doc.getBoolean("replacementDrawn");
                        if ("cancelled".equals(status) && (replacementDrawn == null || !replacementDrawn)) {
                            replacementIds.add(doc.getId());
                        }
                    }

                    replacementAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Failed to load replacement pool: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Notify a single selected entrant using the Cloud Function. */
    private void notifySingleEntrant(String entrantId) {
        if (eventId == null || eventId.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "No event ID for notification.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("entrantId", entrantId);

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

            String msg = "Notification for " + entrantId +
                    ": " + sent + " success, " + failed + " failed.";
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

            loadSelectedParticipants(0);
        });

        task.addOnFailureListener(e -> {
            if (getContext() == null) return;
            Toast.makeText(getContext(),
                    "Failed to notify entrant: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void onDrawReplacementClicked(String entrantId) {
        // [FIX] Now correctly calls the replacement logic instead of showing a placeholder toast
        drawReplacementForCancelled(entrantId);
    }

    /**
     * Called when the organizer taps "Draw Replacement" on a cancelled entrant.
     * For now this simply calls sampleAttendees(eventId, 1) to fill the open spot,
     * then hides this cancelled entry from the Replacement Pool.
     */
    private void drawReplacementForCancelled(String cancelledEntrantId) {
        if (eventId == null || eventId.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "No event ID for replacement.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        repository.sampleAttendees(eventId, 1,
                new EventRepository.EventTaskCallback() {
                    @Override
                    public void onSuccess() {
                        if (getContext() == null) return;

                        // Mark this cancelled entrant as already handled so it disappears
                        FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId)
                                .collection("selected")
                                .document(cancelledEntrantId)
                                .update("replacementDrawn", true);

                        Toast.makeText(getContext(),
                                "Replacement drawn for cancelled entrant.",
                                Toast.LENGTH_LONG).show();

                        loadSelectedParticipants(0);
                        loadReplacementPool();
                    }

                    @Override
                    public void onError(String message) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(),
                                "Failed to draw replacement: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
