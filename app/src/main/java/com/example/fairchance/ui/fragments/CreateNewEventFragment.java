package com.example.fairchance.ui.fragments;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment responsible for the creation of new events by an Organizer.
 * This class handles the input of all event details including name, description,
 * poster image, date/time scheduling, and geolocation requirements.
 *
 * Implements User Stories:
 * - US 02.01.01 (Create event, generate QR code logic)
 * - US 02.01.04 (Set registration period)
 * - US 02.02.03 (Enable/disable geolocation requirement)
 * - US 02.03.01 (Optionally limit waiting list)
 * - US 02.04.01 (Upload event poster)
 */
public class CreateNewEventFragment extends Fragment {

    private ImageView ivEventPoster;
    private TextInputEditText etEventName, etEventDescription, etEventTimeLocation;
    private TextInputEditText etRegistrationDates, etRegistrationTimes;
    private CheckBox cbGeolocation, cbWaitlistLimit;
    private Button btnGenerateQRCode, btnCreateEvent;
    private TextInputEditText etEventDate, etCategory, etGuidelines;
    private final Calendar eventDateCal = Calendar.getInstance();
    private Date eventDate;

    private final Calendar regStartCal = Calendar.getInstance();
    private final Calendar regEndCal   = Calendar.getInstance();
    private Date registrationStart, registrationEnd;
    private Long waitlistLimit = null;
    private Uri posterUri = null;
    private String uploadedPosterUrl = null;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private EventRepository repo;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    posterUri = uri;
                    ivEventPoster.setImageURI(uri);
                }
            });

    /**
     * Inflates the layout for the Create New Event screen.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.create_new_event, container, false);
    }

    /**
     * Initializes the view components, sets up event listeners for date pickers,
     * toggles (geolocation, waiting list limit), and the image picker.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = new EventRepository();

        ivEventPoster         = view.findViewById(R.id.ivEventPoster);
        etEventName           = view.findViewById(R.id.etEventName);
        etEventDescription    = view.findViewById(R.id.etEventDescription);
        etEventTimeLocation   = view.findViewById(R.id.etEventTimeLocation);
        etRegistrationDates   = view.findViewById(R.id.etRegistrationDates);
        etRegistrationTimes   = view.findViewById(R.id.etRegistrationTimes);
        cbGeolocation         = view.findViewById(R.id.cbGeolocation);
        cbWaitlistLimit       = view.findViewById(R.id.cbWaitlistLimit);
        btnGenerateQRCode     = view.findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent        = view.findViewById(R.id.btnCreateEvent);
        etEventDate           = view.findViewById(R.id.etEventDate);
        etCategory            = view.findViewById(R.id.etCategory);
        etGuidelines          = view.findViewById(R.id.etGuidelines);

        cbWaitlistLimit.setChecked(false);
        waitlistLimit = null;

        ivEventPoster.setOnClickListener(v -> pickImage.launch("image/*"));

        etRegistrationDates.setKeyListener(null);
        etRegistrationDates.setOnClickListener(v -> showDateRangePicker());

        etRegistrationTimes.setKeyListener(null);
        etRegistrationTimes.setOnClickListener(v -> showStartEndTimePickers());

        cbWaitlistLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showWaitlistLimitDialog();
            } else {
                waitlistLimit = null;
            }
        });

        btnGenerateQRCode.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "QR will be generated on the event details screen after creation.",
                        Toast.LENGTH_SHORT).show());

        etEventDate.setKeyListener(null);
        etEventDate.setOnClickListener(v -> showEventDatePicker());

        btnCreateEvent.setOnClickListener(v -> onCreateEventClicked());
    }

    /**
     * Displays a MaterialDatePicker range picker for selecting the registration start and end dates.
     * Updates the UI with the selected range formatted as strings.
     */
    private void showDateRangePicker() {
        MaterialDatePicker<Long> startPicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pick registration START date")
                .build();

        startPicker.addOnPositiveButtonClickListener(startUtc -> {
            regStartCal.setTimeInMillis(startUtc);
            MaterialDatePicker<Long> endPicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Pick registration END date")
                    .build();
            endPicker.addOnPositiveButtonClickListener(endUtc -> {
                regEndCal.setTimeInMillis(endUtc);
                String display = dateFmt.format(regStartCal.getTime()) + " → " + dateFmt.format(regEndCal.getTime());
                etRegistrationDates.setText(display);
            });
            endPicker.show(getParentFragmentManager(), "endDate");
        });

        startPicker.show(getParentFragmentManager(), "startDate");
    }

    /**
     * Displays MaterialTimePickers for selecting the start and end times of the registration period.
     * Updates the Calendar objects and the UI text field.
     */
    private void showStartEndTimePickers() {
        MaterialTimePicker start = new MaterialTimePicker.Builder()
                .setTitleText("Pick registration START time")
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(9).setMinute(0)
                .build();

        start.addOnPositiveButtonClickListener(v -> {
            regStartCal.set(Calendar.HOUR_OF_DAY, start.getHour());
            regStartCal.set(Calendar.MINUTE, start.getMinute());
            regStartCal.set(Calendar.SECOND, 0);

            MaterialTimePicker end = new MaterialTimePicker.Builder()
                    .setTitleText("Pick registration END time")
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(17).setMinute(0)
                    .build();
            end.addOnPositiveButtonClickListener(v2 -> {
                regEndCal.set(Calendar.HOUR_OF_DAY, end.getHour());
                regEndCal.set(Calendar.MINUTE, end.getMinute());
                regEndCal.set(Calendar.SECOND, 0);
                String display = timeFmt.format(regStartCal.getTime()) + " → " + timeFmt.format(regEndCal.getTime());
                etRegistrationTimes.setText(display);
            });
            end.show(getParentFragmentManager(), "endTime");
        });

        start.show(getParentFragmentManager(), "startTime");
    }

    /**
     * Displays a date picker for selecting the specific date the actual event takes place.
     * Handles timezone conversion to ensure the correct date is stored.
     */
    private void showEventDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pick event date")
                .build();

        picker.addOnPositiveButtonClickListener(utcMillis -> {
            Calendar utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(utcMillis);

            eventDateCal.clear();
            eventDateCal.set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
            );
            eventDateCal.set(Calendar.MILLISECOND, 0);

            eventDate = eventDateCal.getTime();
            etEventDate.setText(dateFmt.format(eventDate));
        });

        picker.show(getParentFragmentManager(), "eventDate");
    }

    /**
     * Shows a dialog with a number picker to set a hard limit on the waiting list size.
     * If cancelled, the limit feature is disabled.
     */
    private void showWaitlistLimitDialog() {
        Context ctx = requireContext();
        NumberPicker picker = new NumberPicker(ctx);
        picker.setMinValue(1);
        picker.setMaxValue(10000);
        picker.setValue(waitlistLimit == null ? 100 : waitlistLimit.intValue());

        new AlertDialog.Builder(ctx)
                .setTitle("Set waiting list limit")
                .setView(picker)
                .setPositiveButton("Set", (d, w) -> waitlistLimit = (long) picker.getValue())
                .setNegativeButton("Cancel", (d, w) -> {
                    cbWaitlistLimit.setChecked(false);
                    waitlistLimit = null;
                })
                .show();
    }

    /**
     * Handles the "Create Event" button click.
     * Validates all form inputs, parses dates, constructs the Event object,
     * and initiates the poster upload (if applicable) or direct event creation.
     */
    private void onCreateEventClicked() {
        String name         = s(etEventName);
        String desc         = s(etEventDescription);
        String timeLocation = s(etEventTimeLocation);
        String eventDateStr = s(etEventDate);
        String category     = s(etCategory);
        String guidelines   = s(etGuidelines);
        boolean geoRequired = cbGeolocation.isChecked();

        if (TextUtils.isEmpty(name)) {
            etEventName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(desc)) {
            etEventDescription.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(eventDateStr)) {
            etEventDate.setError("Required");
            return;
        }
        if (etRegistrationDates.getText() == null || etRegistrationTimes.getText() == null ||
                TextUtils.isEmpty(etRegistrationDates.getText().toString()) ||
                TextUtils.isEmpty(etRegistrationTimes.getText().toString())) {
            Toast.makeText(requireContext(), "Pick registration start & end date/time.", Toast.LENGTH_SHORT).show();
            return;
        }

        Date parsedEventDate;
        try {
            parsedEventDate = dateFmt.parse(eventDateStr);
        } catch (ParseException e) {
            etEventDate.setError("Use format YYYY-MM-DD");
            return;
        }

        registrationStart = regStartCal.getTime();
        registrationEnd   = regEndCal.getTime();

        if (!registrationEnd.after(registrationStart)) {
            Toast.makeText(requireContext(), "End must be after start.", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event();
        event.setOrganizerId(FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown");
        event.setName(name);
        event.setDescription(desc);
        event.setLocation(timeLocation);
        event.setRegistrationStart(registrationStart);
        event.setRegistrationEnd(registrationEnd);
        event.setGeolocationRequired(geoRequired);

        event.setEventDate(parsedEventDate);
        if (!TextUtils.isEmpty(category)) {
            event.setCategory(category);
        }
        if (!TextUtils.isEmpty(guidelines)) {
            event.setGuidelines(guidelines);
        }

        if (cbWaitlistLimit.isChecked() && waitlistLimit != null) {
            event.setWaitingListLimit(waitlistLimit);
        }

        if (posterUri != null) {
            uploadPosterThenCreate(event);
        } else {
            createEvent(event);
        }
    }

    /**
     * Uploads the selected poster image to Firebase Storage.
     * On success, retrieves the download URL, sets it on the Event object,
     * and proceeds to save the event to Firestore.
     *
     * @param event The Event object with metadata populated.
     */
    private void uploadPosterThenCreate(Event event) {
        try {
            String ext = getExtFromUri(posterUri);
            String fileName = "poster_" + System.currentTimeMillis() + (ext != null ? ("." + ext) : "");
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("event_posters")
                    .child(FirebaseAuth.getInstance().getUid() == null ? "unknown" : FirebaseAuth.getInstance().getUid())
                    .child(fileName);

            ref.putFile(posterUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> {
                        uploadedPosterUrl = uri.toString();
                        event.setPosterImageUrl(uploadedPosterUrl);
                        createEvent(event);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Poster upload failed, creating event without image.", Toast.LENGTH_LONG).show();
                        createEvent(event);
                    });

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Poster upload error, creating event without image.", Toast.LENGTH_LONG).show();
            createEvent(event);
        }
    }

    /**
     * Saves the Event object to the Firestore repository.
     * Disables the create button to prevent double submissions.
     *
     * @param event The fully populated Event object.
     */
    private void createEvent(Event event) {
        btnCreateEvent.setEnabled(false);
        repo.createEvent(event, new EventRepository.EventTaskCallback() {
            @Override public void onSuccess() {
                btnCreateEvent.setEnabled(true);
                Toast.makeText(requireContext(), "Event created!", Toast.LENGTH_SHORT).show();
                clearForm();
            }
            @Override public void onError(String message) {
                btnCreateEvent.setEnabled(true);
                Toast.makeText(requireContext(), "Create failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Resets the UI form to its default state after a successful event creation.
     */
    private void clearForm() {
        posterUri = null;
        uploadedPosterUrl = null;
        ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        etEventName.setText("");
        etEventDescription.setText("");
        etEventTimeLocation.setText("");
        etRegistrationDates.setText("");
        etRegistrationTimes.setText("");
        etEventDate.setText("");
        etCategory.setText("");
        etGuidelines.setText("");
        eventDate = null;
        eventDateCal.setTime(new Date());
        cbGeolocation.setChecked(true);
        cbWaitlistLimit.setChecked(false);
        waitlistLimit = null;
        regStartCal.setTime(new Date());
        regEndCal.setTime(new Date());
    }

    /**
     * Helper to retrieve the text string from a TextInputEditText safely.
     *
     * @param et The EditText to read.
     * @return The trimmed string content or an empty string if null.
     */
    private static String s(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    /**
     * Helper to determine the file extension from a generic URI (content or file).
     *
     * @param uri The URI of the selected file.
     * @return The file extension string (e.g., "jpg", "png") or null.
     */
    private String getExtFromUri(Uri uri) {
        try {
            ContentResolver cr = requireContext().getContentResolver();
            String type = cr.getType(uri);
            if (type != null) return MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
            String name = queryDisplayName(uri);
            int dot = name.lastIndexOf('.');
            return dot >= 0 ? name.substring(dot + 1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper to query the display name of a file from a content URI.
     *
     * @param uri The content URI.
     * @return The display name or "file" if not found.
     */
    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor c = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        }
        return "file";
    }
}
