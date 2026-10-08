package com.example.fairchance.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.example.fairchance.models.User;
import com.example.fairchance.ui.EventDetailsActivity;
import com.example.fairchance.ui.adapters.EventAdapter;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Home screen for users with the Entrant role.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Shows a personalized welcome message for the logged-in entrant.</li>
 *     <li>Displays a searchable, filterable list of upcoming events.</li>
 *     <li>Supports category filters (e.g., Music, Dance, Tech) and a "Today's Events" date filter.</li>
 *     <li>Allows entrants to scan QR codes to open specific events.</li>
 *     <li>Provides navigation to a "How it works" guidelines screen.</li>
 * </ul>
 */
public class EntrantHomeFragment extends Fragment {

    private static final String TAG = "EntrantHomeFragment";

    // UI references
    private RecyclerView eventsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView tvWelcomeName;
    private EditText searchEditText;
    private ImageButton scanButton;
    private Button btnAll, btnMusic, btnDance, btnArt, btnTech, btnSports;
    private Button btnHowItWorks;
    private Button btnFilterToday;

    // State for UI filters
    private Button currentCategoryButton;
    private boolean isTodayFilterActive = false;

    // Data + adapter
    private EventAdapter eventAdapter;
    private final List<Event> eventList = new ArrayList<>();
    private EventRepository eventRepository;
    private AuthRepository authRepository;
    private ListenerRegistration eventListenerRegistration;

    /**
     * ActivityResultLauncher that receives the result of the QR code scan.
     * <p>
     * When a QR code is successfully scanned, the event ID is extracted and
     * {@link EventDetailsActivity} is opened for that event.
     */
    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(
                            result.getResultCode(),
                            result.getData()
                    );
                    if (scanResult != null) {
                        if (scanResult.getContents() == null) {
                            Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_LONG).show();
                        } else {
                            String eventId = scanResult.getContents();
                            Log.d(TAG, "Scanned Event ID: " + eventId);

                            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
                            intent.putExtra("EVENT_ID", eventId);
                            startActivity(intent);
                        }
                    }
                }
            }
    );

    /**
     * Inflates the layout for the entrant home dashboard.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entrant_main_dashboard, container, false);
    }

    /**
     * Initializes repositories, sets up UI components, listeners, and starts
     * loading both user profile and event data.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Repositories
        eventRepository = new EventRepository();
        authRepository = new AuthRepository();

        // Bind views
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        tvWelcomeName = view.findViewById(R.id.textView3);
        searchEditText = view.findViewById(R.id.editText);
        scanButton = view.findViewById(R.id.imageButton);
        btnHowItWorks = view.findViewById(R.id.btn_how_it_works);
        btnFilterToday = view.findViewById(R.id.btn_filter_today);

        btnAll = view.findViewById(R.id.button);
        btnMusic = view.findViewById(R.id.button2);
        btnDance = view.findViewById(R.id.button3);
        btnArt = view.findViewById(R.id.button4);
        btnTech = view.findViewById(R.id.button5);
        btnSports = view.findViewById(R.id.button_sports);

        // Default selected category is "All"
        currentCategoryButton = btnAll;
        updateCategoryButtonAppearance(currentCategoryButton, true);

        // RecyclerView setup
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList);
        eventsRecyclerView.setAdapter(eventAdapter);

        // Load logged-in user's name
        loadUserProfile();

        // Search filter: forwards text changes to the adapter's filter
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) { }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {
                // Filter events by search query (e.g., title)
                eventAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Category filter click listeners
        View.OnClickListener categoryClickListener = v -> {
            Button clickedButton = (Button) v;
            String category = clickedButton.getText().toString();

            // Reset previous selection
            if (currentCategoryButton != null) {
                updateCategoryButtonAppearance(currentCategoryButton, false);
            }

            // Apply new selection
            currentCategoryButton = clickedButton;
            updateCategoryButtonAppearance(currentCategoryButton, true);

            // Let adapter filter by the chosen category
            eventAdapter.setCategory(category);
        };

        btnAll.setOnClickListener(categoryClickListener);
        btnMusic.setOnClickListener(categoryClickListener);
        btnDance.setOnClickListener(categoryClickListener);
        btnArt.setOnClickListener(categoryClickListener);
        btnTech.setOnClickListener(categoryClickListener);
        btnSports.setOnClickListener(categoryClickListener);

        // "How it works" navigation
        btnHowItWorks.setOnClickListener(v -> navigateToGuidelines());

        // Date filter toggle ("Today's Events" <-> "Show All Dates")
        btnFilterToday.setOnClickListener(v -> toggleDateFilter());

        // QR scan button
        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(requireActivity());
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Event QR Code");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);

            qrCodeLauncher.launch(integrator.createScanIntent());
        });

        // Listen to events in real-time
        loadEvents();
    }

    /**
     * Called when the fragment becomes visible again.
     * <p>
     * This ensures that if the user joins or leaves a waitlist in
     * {@link EventDetailsActivity} and then returns, any button states in the
     * list (e.g., "Join" / "Leave" color) are refreshed.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (eventAdapter != null) {
            eventAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Toggles the "Today's Events" date filter.
     * <p>
     * When the filter is active, only events happening today are shown.
     * When inactive, all event dates are shown.
     */
    private void toggleDateFilter() {
        if (getContext() == null) return;

        isTodayFilterActive = !isTodayFilterActive;

        if (isTodayFilterActive) {
            btnFilterToday.setText("Show All Dates");
            btnFilterToday.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            btnFilterToday.setBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), R.color.FCblue)
            );
            eventAdapter.setDateFilter("TODAY");
        } else {
            btnFilterToday.setText("Today's Events");
            btnFilterToday.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.text_primary)
            );
            btnFilterToday.setBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), android.R.color.transparent)
            );
            eventAdapter.setDateFilter("ALL");
        }
    }

    /**
     * Updates the appearance of a category button to indicate whether it is selected.
     *
     * @param button     the button to update
     * @param isSelected {@code true} if the button is the current category, {@code false} otherwise
     */
    private void updateCategoryButtonAppearance(Button button, boolean isSelected) {
        if (getContext() == null || button == null) return;

        if (isSelected) {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            button.setBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), R.color.FCblue)
            );
        } else {
            button.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.text_primary)
            );
            button.setBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), android.R.color.transparent)
            );
        }
    }

    /**
     * Navigates to {@link GuidelinesFragment} when the "How it Works" button is clicked.
     * <p>
     * The transaction is added to the back stack so that the user can press
     * the back button to return to the entrant home screen.
     */
    private void navigateToGuidelines() {
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.dashboard_container, new GuidelinesFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    /**
     * Loads the current user's profile and updates the welcome text.
     * <p>
     * If the user does not have a name set, "User" is shown as a fallback.
     */
    private void loadUserProfile() {
        authRepository.getUserProfile(new AuthRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (user.getName() != null && !user.getName().isEmpty()) {
                    tvWelcomeName.setText(user.getName());
                } else {
                    tvWelcomeName.setText("User");
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load user name: " + message);
                tvWelcomeName.setText("User");
            }
        });
    }

    /**
     * Subscribes to all events via {@link EventRepository} and updates the adapter.
     * <p>
     * This uses a Firestore real-time listener, so UI updates when events change.
     */
    private void loadEvents() {
        showLoading(true);

        eventListenerRegistration = eventRepository.getAllEvents(
                new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        showLoading(false);

                        if (events.isEmpty()) {
                            showEmptyView(true);
                        } else {
                            showEmptyView(false);
                            eventAdapter.updateBaseEventsAndRefilter(events);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        showLoading(false);
                        Log.e(TAG, "Error loading events: " + message);
                        Toast.makeText(getContext(),
                                "Failed to load events",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /**
     * Cleans up the Firestore listener when the view is destroyed.
     * <p>
     * This avoids memory leaks and unnecessary network usage when the fragment
     * is no longer visible.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventListenerRegistration != null) {
            eventListenerRegistration.remove();
            Log.d(TAG, "Firestore listener removed.");
        }
    }

    /**
     * Shows or hides the loading indicator and hides other views while loading.
     *
     * @param isLoading {@code true} to show the progress bar, {@code false} to hide it.
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Shows or hides the "No events found" view and the events list accordingly.
     *
     * @param show {@code true} to show the empty view; {@code false} to show the list of events.
     */
    private void showEmptyView(boolean show) {
        if (show) {
            emptyView.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            eventsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
