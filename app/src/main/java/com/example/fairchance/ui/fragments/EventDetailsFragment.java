package com.example.fairchance.ui.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Shows full details for a single event.
 * <p>
 * Organizer capabilities:
 * - View QR code for entrants to scan
 * - See registration window and details
 * - Open waiting/chosen/cancelled/final entrants lists
 * - Open sampling/replacement screen
 * - Update poster and edit event (if not admin)
 * <p>
 * Admins see the event details but not organizer actions.
 */
public class EventDetailsFragment extends Fragment {

    private TextView tvEventName, tvEventDescription, tvEventTimeLocation, tvRegistrationDates, tvRegistrationTimes;
    private ImageView ivEventPoster, ivQRCode;
    private Button btnWaitingList, btnChosen, btnCancelled, btnFinal;

    // Optional views for geolocation + editing
    private TextView tvGeolocationRequiredLabel, tvGeolocationRequiredValue;
    private Button btnUpdatePoster, btnEditEvent;

    // Sampling & Replacement control
    private Button btnSamplingReplacement;

    // Wraps all organizer-only actions (buttons below the QR)
    private Group groupOrganizerActions;

    private String eventId;
    private EventRepository repository;
    private Event loadedEvent;
    private AuthRepository authRepository;

    /**
     * Image picker used when updating the event poster.
     */
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && loadedEvent != null) {
                    repository.uploadPosterAndUpdate(
                            loadedEvent.getEventId(),
                            uri,
                            new EventRepository.EventTaskCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Poster updated.", Toast.LENGTH_SHORT).show();
                                    loadEventDetails(loadedEvent.getEventId());
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(getContext(), "Upload failed: " + message, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                }
            });

    public EventDetailsFragment() { }

    /**
     * Factory method to create a details fragment for a given event ID.
     *
     * @param eventId ID of the event to display
     */
    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();
        authRepository = new AuthRepository();

        // Bind views
        ivEventPoster = view.findViewById(R.id.ivEventPoster);
        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvEventName = view.findViewById(R.id.tvEventName);
        tvEventDescription = view.findViewById(R.id.tvEventDescription);
        tvEventTimeLocation = view.findViewById(R.id.tvEventTimeLocation);
        tvRegistrationDates = view.findViewById(R.id.tvRegistrationDates);
        tvRegistrationTimes = view.findViewById(R.id.tvRegistrationTimes);

        btnWaitingList = view.findViewById(R.id.btnEntrantsWaitingList);
        btnChosen = view.findViewById(R.id.btnChosenEntrants);
        btnCancelled = view.findViewById(R.id.btnCancelledEntrants);
        btnFinal = view.findViewById(R.id.btnFinalEntrants);
        btnSamplingReplacement = view.findViewById(R.id.btnSamplingReplacement);

        groupOrganizerActions = view.findViewById(R.id.groupOrganizerActions);

        tvGeolocationRequiredLabel = view.findViewById(R.id.tvGeolocationRequiredLabel);
        tvGeolocationRequiredValue = view.findViewById(R.id.tvGeolocationRequiredValue);
        btnUpdatePoster = view.findViewById(R.id.btnUpdatePoster);
        btnEditEvent = view.findViewById(R.id.btnEditEvent);

        // Hide organizer-only actions for admins (admins are read-only here)
        if (groupOrganizerActions != null) {
            authRepository.getUserRole(new AuthRepository.RoleCallback() {
                @Override
                public void onRoleFetched(String role) {
                    if ("admin".equalsIgnoreCase(role)) {
                        groupOrganizerActions.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String message) {
                    // If role lookup fails, default is organizer behavior (no further action).
                }
            });
        }

        // Update poster from gallery
        if (btnUpdatePoster != null) {
            btnUpdatePoster.setOnClickListener(v -> {
                if (loadedEvent == null) return;
                pickImage.launch("image/*");
            });
        }

        // Edit event details
        if (btnEditEvent != null) {
            btnEditEvent.setOnClickListener(v1 -> {
                if (eventId != null) {
                    FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.dashboard_container, UpdateEventFragment.newInstance(eventId));
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });
        }

        // Load event if passed in arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("EVENT_ID");
            loadEventDetails(eventId);
        }

        // Open waiting list
        btnWaitingList.setOnClickListener(v -> {
            if (eventId != null && !eventId.isEmpty() && loadedEvent != null) {
                openFragment(EntrantsWaitingListFragment.newInstance(
                        eventId,
                        loadedEvent.getName()
                ));
            } else {
                Toast.makeText(getContext(),
                        "Event not loaded yet.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Open chosen entrants list
        btnChosen.setOnClickListener(v -> {
            if (eventId != null && !eventId.isEmpty() && loadedEvent != null) {
                openFragment(ChosenEntrantsFragment.newInstance(eventId, loadedEvent.getName()));
            } else {
                Toast.makeText(getContext(),
                        "No event ID available for chosen entrants.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Open cancelled entrants list
        btnCancelled.setOnClickListener(v -> {
            if (eventId != null && !eventId.isEmpty()) {
                openFragment(CancelledEntrantsFragment.newInstance(eventId));
            } else {
                Toast.makeText(getContext(),
                        "No event ID available for cancelled entrants.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Open final entrants list
        btnFinal.setOnClickListener(v -> {
            if (eventId != null && !eventId.isEmpty()) {
                String name = (loadedEvent != null && loadedEvent.getName() != null)
                        ? loadedEvent.getName()
                        : "";
                openFragment(FinalEntrantsFragment.newInstance(eventId, name));
            } else {
                Toast.makeText(
                        getContext(),
                        "No event ID available for final entrants.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        // Open sampling/replacement flow
        if (btnSamplingReplacement != null) {
            btnSamplingReplacement.setOnClickListener(v -> {
                if (loadedEvent == null) {
                    Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                FragmentTransaction ft = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();

                ft.replace(R.id.dashboard_container,
                        SamplingReplacementFragment.newInstance(loadedEvent.getEventId()));
                ft.addToBackStack(null);
                ft.commit();
            });
        }
    }

    /**
     * Fetches event details from the repository and populates the UI.
     *
     * @param eventId the event ID to load
     */
    private void loadEventDetails(String eventId) {
        repository.getEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                loadedEvent = event;
                populateUI(event);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "Error loading event: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Binds event data onto the screen:
     * name, description, location, registration window, poster, geolocation flag, and QR code.
     */
    private void populateUI(Event event) {
        tvEventName.setText(event.getName());
        tvEventDescription.setText(event.getDescription());
        tvEventTimeLocation.setText(event.getLocation());

        SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (event.getRegistrationStart() != null && event.getRegistrationEnd() != null) {
            tvRegistrationDates.setText(
                    sdfDate.format(event.getRegistrationStart()) + " - " +
                            sdfDate.format(event.getRegistrationEnd()));
            tvRegistrationTimes.setText(
                    sdfTime.format(event.getRegistrationStart()) + " - " +
                            sdfTime.format(event.getRegistrationEnd()));
        }

        // Load poster image (if URL is null, Glide will just show placeholder)
        Glide.with(requireContext())
                .load(event.getPosterImageUrl())
                .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                .into(ivEventPoster);

        // Show whether geolocation is required for this event (if view exists)
        if (tvGeolocationRequiredValue != null) {
            tvGeolocationRequiredValue.setText(event.isGeolocationRequired() ? "Yes" : "No");
        }

        // Generate QR code encoding the event ID
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(event.getEventId(), BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(matrix);
            ivQRCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper to replace the dashboard container with another fragment
     * and push the transaction onto the back stack.
     */
    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.dashboard_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
