package com.example.fairchance.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EnrollmentRepository;
import com.example.fairchance.data.repository.EventCatalogRepository;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.fragments.GuidelinesFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Shows full details for a single event and allows joining/leaving the waitlist.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EventCatalogRepository eventCatalogRepository;
    private EnrollmentRepository enrollmentRepository;
    private String currentEventId;
    private Event currentEvent;
    private String currentEventStatus = null;
    private ListenerRegistration waitlistListener;
    private FusedLocationProviderClient fusedLocationClient;

    // UI
    private ImageView eventPosterImage;
    private ProgressBar progressBar;
    private LinearLayout eventDetailsContent;
    private TextView tvEventName, tvEventDate, tvEventLocation, tvEventDescription,
            tvEventGuidelines, tvEventWaitlistCount;
    private Button btnJoinWaitlist;
    private ProgressBar joinProgressBar;
    private TextView tvSeeFullGuidelines;
    private FrameLayout guidelinesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventCatalogRepository = new EventCatalogRepository();
        enrollmentRepository = new EnrollmentRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        currentEventId = getIntent().getStringExtra("EVENT_ID");
        if (currentEventId == null) {
            Log.e(TAG, "Event ID is null. Finishing activity.");
            Toast.makeText(this, "Error: Event not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        eventPosterImage = findViewById(R.id.event_poster_image);
        progressBar = findViewById(R.id.event_details_progress);
        eventDetailsContent = findViewById(R.id.event_details_content);
        tvEventName = findViewById(R.id.event_name_text);
        tvEventDate = findViewById(R.id.event_date_text);
        tvEventLocation = findViewById(R.id.event_location_text);
        tvEventDescription = findViewById(R.id.event_description_text);
        tvEventGuidelines = findViewById(R.id.event_guidelines_text);
        tvEventWaitlistCount = findViewById(R.id.event_waitlist_count_text);
        btnJoinWaitlist = findViewById(R.id.join_waitlist_button);
        joinProgressBar = findViewById(R.id.join_progress);
        tvSeeFullGuidelines = findViewById(R.id.tv_see_full_guidelines);
        guidelinesContainer = findViewById(R.id.guidelines_container);

        loadEventDetails();

        btnJoinWaitlist.setOnClickListener(v -> {
            if (currentEventStatus == null) {
                initiateJoinProcess();
            } else if ("Waiting".equals(currentEventStatus)) {
                leaveWaitlist();
            }
        });

        tvSeeFullGuidelines.setOnClickListener(v -> {
            if (guidelinesContainer != null) {
                String rules = (currentEvent != null && currentEvent.getGuidelines() != null)
                        ? currentEvent.getGuidelines()
                        : "No specific guidelines provided by the organizer.";

                guidelinesContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.guidelines_container, GuidelinesFragment.newInstance(rules))
                        .addToBackStack("guidelines")
                        .commit();
            }
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0 && guidelinesContainer != null) {
                guidelinesContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (waitlistListener != null) {
            waitlistListener.remove();
        }
    }

    /**
     * Loads event, then waitlist count and user status.
     */
    private void loadEventDetails() {
        setLoading(true);
        eventCatalogRepository.getEvent(currentEventId, new EventCatalogRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                populateUi(event);
                loadWaitlistCount();
                checkUserStatus();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Log.e(TAG, "Error loading event: " + message);
                Toast.makeText(EventDetailsActivity.this, "Failed to load event.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Listens for real-time waitlist count updates.
     */
    private void loadWaitlistCount() {
        if (waitlistListener != null) {
            waitlistListener.remove();
        }

        waitlistListener = eventCatalogRepository.observeWaitingListCount(currentEventId, new EventCatalogRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                if (currentEvent != null && currentEvent.getWaitingListLimit() > 0) {
                    long limit = currentEvent.getWaitingListLimit();
                    long left = Math.max(0, limit - count);
                    tvEventWaitlistCount.setText(
                            "Waitlist: " + count + " / " + limit + " (" + left + " spots left)"
                    );
                } else {
                    tvEventWaitlistCount.setText(count + " people on waitlist");
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading waitlist count: " + message);
                tvEventWaitlistCount.setText("Error loading count");
            }
        });
    }

    /**
     * Checks current user's status for this event.
     */
    private void checkUserStatus() {
        eventCatalogRepository.getCurrentUserStatus(currentEventId, new EventCatalogRepository.StatusCallback() {
            @Override
            public void onSuccess(String status) {
                currentEventStatus = status;
                updateJoinButtonUI();
                setLoading(false);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error checking user status: " + message);
                setLoading(false);
            }
        });
    }

    /**
     * Starts join flow (with optional location).
     */
    private void initiateJoinProcess() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent.isGeolocationRequired()) {
            checkLocationPermissionAndJoin();
        } else {
            performJoin(null, null);
        }
    }

    private void checkLocationPermissionAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        setButtonLoading(true);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        performJoin(location.getLatitude(), location.getLongitude());
                    } else {
                        setButtonLoading(false);
                        Toast.makeText(
                                EventDetailsActivity.this,
                                "Unable to verify location. Ensure GPS is on.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setButtonLoading(false);
                    Log.e(TAG, "Error fetching location", e);
                    Toast.makeText(
                            EventDetailsActivity.this,
                            "Error fetching location: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(
                        this,
                        "Location permission is required to join this event.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    /**
     * Joins the waitlist, optionally with location.
     */
    private void performJoin(@Nullable Double lat, @Nullable Double lng) {
        setButtonLoading(true);

        EnrollmentRepository.TaskCallback callback = new EnrollmentRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setButtonLoading(false);
                currentEventStatus = "Waiting";
                updateJoinButtonUI();
                Toast.makeText(EventDetailsActivity.this, "Joined waitlist!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setButtonLoading(false);
                Toast.makeText(
                        EventDetailsActivity.this,
                        "Failed to join: " + message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        if (lat != null && lng != null) {
            enrollmentRepository.joinWaitingList(currentEventId, lat, lng, callback);
        } else {
            enrollmentRepository.joinWaitingList(currentEventId, null, null, callback);
        }
    }

    /**
     * Leaves the waitlist.
     */
    private void leaveWaitlist() {
        setButtonLoading(true);
        enrollmentRepository.leaveWaitingList(currentEventId, new EnrollmentRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setButtonLoading(false);
                currentEventStatus = null;
                updateJoinButtonUI();
                Toast.makeText(EventDetailsActivity.this, "Left waitlist.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setButtonLoading(false);
                Toast.makeText(
                        EventDetailsActivity.this,
                        "Failed to leave: " + message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Fills event detail views.
     */
    private void populateUi(Event event) {
        tvEventName.setText(event.getName());
        tvEventDescription.setText(event.getDescription());

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            tvEventLocation.setText(event.getLocation());
        } else {
            tvEventLocation.setText("Location not specified");
        }

        if (event.getEventDate() != null) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getEventDate()));
        } else {
            tvEventDate.setText("Date not set");
        }

        if (event.getGuidelines() != null && !event.getGuidelines().isEmpty()) {
            tvEventGuidelines.setText(event.getGuidelines());
        } else {
            tvEventGuidelines.setText("No specific guidelines provided.");
        }

        Glide.with(this)
                .load(event.getPosterImageUrl())
                .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                .error(R.drawable.fairchance_logo_with_words___transparent)
                .into(eventPosterImage);
    }

    /**
     * Updates main action button based on current status.
     */
    private void updateJoinButtonUI() {
        if ("Waiting".equals(currentEventStatus)) {
            btnJoinWaitlist.setText("Leave Waitlist");
            btnJoinWaitlist.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.ticketjack_error)
            ));
            btnJoinWaitlist.setEnabled(true);
        } else if (currentEventStatus == null) {
            btnJoinWaitlist.setText("Join Waitlist");
            btnJoinWaitlist.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ticketjack_primary))
            );
            btnJoinWaitlist.setEnabled(true);
        } else {
            btnJoinWaitlist.setText("You are " + currentEventStatus);
            btnJoinWaitlist.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.ticketjack_outline)
            ));
            btnJoinWaitlist.setEnabled(false);
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            eventDetailsContent.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            eventDetailsContent.setVisibility(View.VISIBLE);
        }
    }

    private void setButtonLoading(boolean isLoading) {
        if (isLoading) {
            joinProgressBar.setVisibility(View.VISIBLE);
            btnJoinWaitlist.setText("");
            btnJoinWaitlist.setEnabled(false);
        } else {
            joinProgressBar.setVisibility(View.GONE);
            btnJoinWaitlist.setEnabled(true);
            // Text is set by updateJoinButtonUI()
        }
    }
}
