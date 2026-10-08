package com.example.fairchance.ui.fragments;

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
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows the final (confirmed) entrants and lets the organizer export them to CSV.
 * Data source:
 *   events/{eventId}/confirmedAttendees/{userId} with field confirmedAt
 *   users/{userId} for name + email
 */
public class FinalEntrantsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";
    private static final String ARG_EVENT_NAME = "EVENT_NAME";

    private String eventId;
    private String eventName = "";

    private RecyclerView rvFinalEntrants;
    private Button btnExportCsv;

    private SelectedParticipantAdapter adapter;
    private final List<String> finalIds = new ArrayList<>();

    private FirebaseFirestore db;

    public FinalEntrantsFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method for creating a FinalEntrantsFragment tied to a specific event.
     */
    public static FinalEntrantsFragment newInstance(String eventId, String eventName) {
        FinalEntrantsFragment fragment = new FinalEntrantsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_NAME, eventName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Single Firestore instance for this fragment
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.final_entrants_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve event arguments
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            eventName = getArguments().getString(ARG_EVENT_NAME, "");
        }

        rvFinalEntrants = view.findViewById(R.id.rvFinalEntrants);
        btnExportCsv = view.findViewById(R.id.btnExportCsv);

        rvFinalEntrants.setLayoutManager(new LinearLayoutManager(getContext()));

        // Simple list showing user IDs + event name, no per-row button
        adapter = new SelectedParticipantAdapter(
                finalIds,
                eventName,
                "",
                false,
                null
        );
        rvFinalEntrants.setAdapter(adapter);

        // Start disabled – only enabled once we know there are entrants
        btnExportCsv.setEnabled(false);

        loadFinalEntrants();

        btnExportCsv.setOnClickListener(v -> exportFinalEntrantsToCsv());
    }

    /**
     * Loads all confirmed attendees from events/{eventId}/confirmedAttendees.
     * If none, keeps Export button disabled.
     */
    private void loadFinalEntrants() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event id provided for final entrants.",
                    Toast.LENGTH_SHORT).show();
            btnExportCsv.setEnabled(false);
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("confirmedAttendees")
                .get()
                .addOnSuccessListener(snapshot -> {
                    finalIds.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // Doc ID is userId
                        finalIds.add(doc.getId());
                    }
                    adapter.setEventName(eventName);
                    adapter.notifyDataSetChanged();

                    boolean hasEntrants = !finalIds.isEmpty();
                    btnExportCsv.setEnabled(hasEntrants);

                    if (!hasEntrants) {
                        Toast.makeText(getContext(),
                                "No confirmed entrants yet.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnExportCsv.setEnabled(false);
                    Toast.makeText(
                            getContext(),
                            "Failed to load final entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * Exports final entrants to CSV with columns:
     *   name, email, enrolledAt, userId
     */
    private void exportFinalEntrantsToCsv() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Event not loaded.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Reload confirmedAttendees to get the latest data before export
        db.collection("events")
                .document(eventId)
                .collection("confirmedAttendees")
                .get()
                .addOnSuccessListener(this::buildCsvFromConfirmedAttendees)
                .addOnFailureListener(e -> Toast.makeText(
                        getContext(),
                        "Failed to load final entrants: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    /**
     * Builds CSV entries from confirmedAttendees and corresponding user documents.
     */
    private void buildCsvFromConfirmedAttendees(QuerySnapshot snapshot) {
        if (snapshot.isEmpty()) {
            Toast.makeText(
                    getContext(),
                    "No confirmed entrants to export.",
                    Toast.LENGTH_SHORT
            ).show();
            btnExportCsv.setEnabled(false);
            return;
        }

        // Collect user IDs + timestamps
        List<String> userIds = new ArrayList<>();
        List<Timestamp> confirmedTimes = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            userIds.add(doc.getId());
            confirmedTimes.add(doc.getTimestamp("confirmedAt"));
        }

        // Build tasks to load user docs for names/emails
        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
        for (String uid : userIds) {
            Task<DocumentSnapshot> t =
                    db.collection("users").document(uid).get();
            userTasks.add(t);
        }

        // Wait until all user docs are loaded, then build CSV
        Tasks.whenAllSuccess(userTasks)
                .addOnSuccessListener(results -> {
                    try {
                        String csv = buildCsvString(userIds, confirmedTimes, results);
                        File outFile = saveCsvToFile(csv);
                        Toast.makeText(
                                getContext(),
                                "Successfully exported final list to CSV.\n" +
                                        "Saved to: " + outFile.getAbsolutePath(),
                                Toast.LENGTH_LONG
                        ).show();
                    } catch (IOException e) {
                        Toast.makeText(
                                getContext(),
                                "Failed to save CSV: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        getContext(),
                        "Failed to load user profiles: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    /**
     * Builds the CSV text from user docs + confirmedAt timestamps.
     */
    private String buildCsvString(List<String> userIds,
                                  List<Timestamp> confirmedTimes,
                                  List<Object> userDocs) {

        SimpleDateFormat df =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        StringBuilder sb = new StringBuilder();
        sb.append("name,email,enrolledAt,userId\n");

        for (int i = 0; i < userIds.size(); i++) {
            String uid = userIds.get(i);
            Timestamp ts = confirmedTimes.get(i);

            DocumentSnapshot userDoc = (DocumentSnapshot) userDocs.get(i);
            String name = extractDisplayName(userDoc, uid);
            String email = userDoc != null ? userDoc.getString("email") : "";

            String enrolledAt = "";
            if (ts != null && ts.toDate() != null) {
                enrolledAt = df.format(ts.toDate());
            }

            // CSV row (basic escaping is fine for this use case)
            sb.append(escapeCsv(name)).append(",")
                    .append(escapeCsv(email)).append(",")
                    .append(escapeCsv(enrolledAt)).append(",")
                    .append(escapeCsv(uid)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Saves CSV text into app-specific external storage as
     * final_entrants_<eventId>.csv
     */
    private File saveCsvToFile(String csv) throws IOException {
        // App-specific external directory (not visible to other apps without sharing)
        File dir = requireContext().getExternalFilesDir(null);
        if (dir == null) {
            throw new IOException("No external files directory available");
        }

        String fileName = "final_entrants_" + eventId + ".csv";
        File outFile = new File(dir, fileName);

        try (FileWriter writer = new FileWriter(outFile, false)) {
            writer.write(csv);
        }

        return outFile;
    }

    /**
     * Gets a human-readable name from the user document, with multiple fallbacks.
     */
    private String extractDisplayName(@Nullable DocumentSnapshot userDoc,
                                      String fallbackUid) {

        if (userDoc == null || !userDoc.exists()) {
            return fallbackUid;
        }

        String name = userDoc.getString("fullName");
        if (name == null || name.isEmpty()) {
            name = userDoc.getString("name");
        }
        if (name == null || name.isEmpty()) {
            name = userDoc.getString("displayName");
        }
        if (name == null || name.isEmpty()) {
            name = fallbackUid;
        }
        return name;
    }

    /**
     * Very small CSV escape (wraps in quotes if there is a comma or quote).
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}
