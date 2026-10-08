package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.EventHistoryItem;
import com.example.fairchance.ui.adapters.EventHistoryAdapter;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment displays the entrant's full event history.
 * It shows a list of all events the user has joined, been invited to, or confirmed,
 * along with their final status (e.g., "Confirmed", "Declined", "Not selected").
 * Fulfills US 01.02.03.
 */
public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private RecyclerView historyRecyclerView;
    private EventHistoryAdapter historyAdapter;
    private List<EventHistoryItem> historyList = new ArrayList<>();
    private EventRepository eventRepository;
    private ListenerRegistration eventHistoryRegistration; // FIX: Added field for listener registration

    private ProgressBar progressBar;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entrant_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();

        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new EventHistoryAdapter(getContext(), historyList);
        historyRecyclerView.setAdapter(historyAdapter);

        loadHistory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventHistoryRegistration != null) {
            eventHistoryRegistration.remove();
            Log.d(TAG, "Event history listener removed.");
        }
    }

    /**
     * Fetches the user's complete event history from the EventRepository
     * and populates the RecyclerView. (Now uses a real-time listener for US 01.02.03)
     */
    private void loadHistory() {
        showLoading(true);
        // FIX: Replaced one-time call with real-time listener and stored registration
        eventHistoryRegistration = eventRepository.getEventHistory(new EventRepository.EventHistoryListCallback() {
            @Override
            public void onSuccess(List<EventHistoryItem> items) {
                showLoading(false);
                if (items.isEmpty()) {
                    showEmptyView(true);
                } else {
                    showEmptyView(false);
                    historyList.clear();
                    historyList.addAll(items);
                    historyAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Log.e(TAG, "Error loading history: " + message);
                Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Helper method to show/hide the main progress bar.
     * @param isLoading True to show loading state, false otherwise.
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method to show/hide the "You have no event history" text.
     * @param show True to show the empty view, false to show the RecyclerView.
     */
    private void showEmptyView(boolean show) {
        if (show) {
            emptyView.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
