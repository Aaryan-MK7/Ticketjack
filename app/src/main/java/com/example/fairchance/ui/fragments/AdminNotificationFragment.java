package com.example.fairchance.ui.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.NotificationLogRepository;
import com.example.fairchance.models.NotificationLog;
import com.example.fairchance.ui.NotificationLogDetailsActivity;
import com.example.fairchance.ui.adapters.NotificationLogAdapter;
import com.example.fairchance.util.SimpleTextWatcher;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that displays the administrative log of all notifications sent within the system.
 * Allows the Admin to filter logs by date range and event name to audit communication.
 *
 * Implements User Story:
 * - US 03.08.01 (Review logs of all notifications sent to entrants)
 */
public class AdminNotificationFragment extends Fragment
        implements NotificationLogAdapter.OnLogClickListener {

    private NotificationLogRepository logRepository;
    private NotificationLogAdapter adapter;

    private final List<NotificationLog> allLogs = new ArrayList<>();

    private TextView tvStartDate, tvEndDate, tvEmptyState;
    private EditText etFilterEvent;
    private Date startDateFilter = null;
    private Date endDateFilter = null;

    private final DateFormat dateFormat =
            DateFormat.getDateInstance(DateFormat.MEDIUM);

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public AdminNotificationFragment() {
        // Required empty public constructor
    }

    /**
     * Inflates the layout for the Admin Notification Log screen.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_notification, container, false);
    }

    /**
     * Initializes the RecyclerView, sets up date pickers and text filters,
     * and fetches the notification logs from the repository.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logRepository = new NotificationLogRepository();

        Button backButton = view.findViewById(R.id.button);
        if (backButton != null) {
            backButton.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }

        RecyclerView rvLogs = view.findViewById(R.id.rvNotificationLogs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationLogAdapter(this);
        rvLogs.setAdapter(adapter);

        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        etFilterEvent = view.findViewById(R.id.etFilterEvent);

        Button btnClearFilters = view.findViewById(R.id.btnClearFilters);

        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));

        btnClearFilters.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            tvStartDate.setText("Start date");
            tvEndDate.setText("End date");
            etFilterEvent.setText("");
            applyFilters();
        });

        etFilterEvent.addTextChangedListener(new SimpleTextWatcher(this::applyFilters));

        loadLogs();
    }

    /**
     * Fetches all notification logs from the repository asynchronously.
     * On success, populates the local list and applies any existing filters.
     */
    private void loadLogs() {
        logRepository.fetchAllLogs(new NotificationLogRepository.LogListCallback() {
            @Override
            public void onSuccess(List<NotificationLog> logs) {
                allLogs.clear();
                allLogs.addAll(logs);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Failed to load logs: " + message,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Shows a DatePickerDialog to select a start or end date filter.
     *
     * @param isStart True if picking the start date, False for the end date.
     */
    private void showDatePicker(boolean isStart) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 0, 0, 0);
                    Date chosen = calendar.getTime();
                    if (isStart) {
                        startDateFilter = chosen;
                        tvStartDate.setText(dateFormat.format(chosen));
                    } else {
                        calendar.set(year, month, dayOfMonth, 23, 59, 59);
                        endDateFilter = calendar.getTime();
                        tvEndDate.setText(dateFormat.format(chosen));
                    }
                    applyFilters();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    /**
     * Filters the local list of logs based on the selected date range and event name query.
     * Updates the adapter with the filtered results.
     */
    private void applyFilters() {
        String eventQuery = etFilterEvent.getText() != null
                ? etFilterEvent.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        List<NotificationLog> filtered = new ArrayList<>();
        for (NotificationLog log : allLogs) {
            Date ts = log.getTimestamp();
            if (startDateFilter != null && (ts == null || ts.before(startDateFilter))) {
                continue;
            }
            if (endDateFilter != null && (ts == null || ts.after(endDateFilter))) {
                continue;
            }

            if (!eventQuery.isEmpty()) {
                String eventName = log.getEventName() != null
                        ? log.getEventName().toLowerCase(Locale.ROOT)
                        : "";
                if (!eventName.contains(eventQuery)) {
                    continue;
                }
            }

            filtered.add(log);
        }

        adapter.setLogs(filtered);
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Callback method when a notification log item is clicked.
     * Opens the detailed view of that log entry.
     *
     * @param log The NotificationLog item that was clicked.
     */
    @Override
    public void onLogClick(NotificationLog log) {
        NotificationLogDetailsActivity.start(requireContext(), log);
    }
}
