package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.fairchance.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows a map of where entrants joined the waiting list (US 02.02.02).
 * - Pins for entrants with a 'location' GeoPoint in events/{eventId}/waitingList
 * - Tapping a pin shows entrant name + registration timestamp
 * - "No Location" text list for entrants without geolocation
 * - Uses a real-time listener so it updates when the waiting list changes
 */
public class WaitingListMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_EVENT_ID = "EVENT_ID";
    private static final String ARG_EVENT_NAME = "EVENT_NAME";

    private String eventId;
    private String eventName;

    private MapView mapView;
    private GoogleMap googleMap;

    private TextView tvNoLocationList;

    private ListenerRegistration waitingListListener;

    private List<DocumentSnapshot> cachedWaitingListDocs = new ArrayList<>();

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());

    public WaitingListMapFragment() {

    }

    public static WaitingListMapFragment newInstance(String eventId, String eventName) {
        WaitingListMapFragment fragment = new WaitingListMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_NAME, eventName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            eventName = getArguments().getString(ARG_EVENT_NAME, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_view_map, container, false);

        mapView = view.findViewById(R.id.mapWaitingList);
        tvNoLocationList = view.findViewById(R.id.tvNoLocationList);

        // MapView lifecycle
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (!cachedWaitingListDocs.isEmpty()) {
            renderFromWaitingListDocs(cachedWaitingListDocs);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        startListeningToWaitingList();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (waitingListListener != null) {
            waitingListListener.remove();
            waitingListListener = null;
        }
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void startListeningToWaitingList() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Event not loaded.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        waitingListListener = db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(
                                getContext(),
                                "Error loading waiting list locations: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    List<DocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        docs.add(doc);
                    }

                    cachedWaitingListDocs = docs;  // keep a copy

                    if (googleMap != null) {
                        renderFromWaitingListDocs(docs);
                    }

                });
    }

    /**
     * Render pins on the map + update "No Location" list from the given docs.
     */
    private void renderFromWaitingListDocs(List<DocumentSnapshot> docs) {
        if (googleMap == null) return;

        googleMap.clear();

        List<String> noLocationLines = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasAnyLocation = false;

        for (DocumentSnapshot doc : docs) {
            String userId = doc.getId();
            GeoPoint gp = doc.getGeoPoint("location");
            Timestamp joinedAt = doc.getTimestamp("joinedAt");

            // Format joinedAt
            String joinedText;
            if (joinedAt != null) {
                joinedText = timeFormat.format(joinedAt.toDate());
            } else {
                joinedText = "Unknown time";
            }

            if (gp == null) {
                // No location → add to "No Location" list
                noLocationLines.add(userId + " • " + joinedText);
                continue;
            }

            hasAnyLocation = true;
            LatLng latLng = new LatLng(gp.getLatitude(), gp.getLongitude());
            boundsBuilder.include(latLng);

            // For the marker title, we want entrant name if possible:
            // Fetch name asynchronously and then add the marker.
            fetchUserDisplayNameAndAddMarker(userId, latLng, joinedText);
        }

        // Update "No Location" list
        if (noLocationLines.isEmpty()) {
            tvNoLocationList.setText("(None)");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String line : noLocationLines) {
                sb.append("• ").append(line).append("\n");
            }
            tvNoLocationList.setText(sb.toString().trim());
        }

        if (hasAnyLocation) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100)
                );
            } catch (IllegalStateException ignored) {
                // Happens if only one point was included; safe to ignore
            }
        }
    }

    /**
     * Loads user's display name from users/{userId} and adds a marker.
     * The marker's title = display name (or userId fallback)
     * The marker's snippet = joined timestamp text.
     *
     * Tapping a pin shows this info in the standard Google Maps info window.
     */
    private void fetchUserDisplayNameAndAddMarker(
            String userId,
            LatLng position,
            String joinedText
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = null;
                    if (doc.exists()) {
                        name = doc.getString("fullName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("name");
                        }
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("email");
                        }
                    }
                    if (name == null || name.isEmpty()) {
                        name = "User: " + userId;
                    }

                    String markerTitle;
                    if (eventName != null && !eventName.isEmpty()) {
                        markerTitle = name + " @ " + eventName;
                    } else {
                        markerTitle = name;
                    }

                    String snippet = "Joined: " + joinedText;

                    if (googleMap != null) {
                        googleMap.addMarker(
                                new MarkerOptions()
                                        .position(position)
                                        .title(markerTitle)
                                        .snippet(snippet)
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    // If we fail to get the name, still show a marker with userId
                    if (googleMap != null) {
                        String markerTitle = "User: " + userId;
                        String snippet = "Joined: " + joinedText;
                        googleMap.addMarker(
                                new MarkerOptions()
                                        .position(position)
                                        .title(markerTitle)
                                        .snippet(snippet)
                        );
                    }
                });
    }
}
