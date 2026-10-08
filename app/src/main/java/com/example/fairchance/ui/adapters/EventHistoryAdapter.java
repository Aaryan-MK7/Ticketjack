package com.example.fairchance.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.models.EventHistoryItem;
import com.example.fairchance.ui.EventDetailsActivity;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying the user's interaction history with events.
 * Fulfills US 01.02.03 by allowing Entrants to view past events they registered for,
 * whether they were selected, waiting, or declined.
 */
public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.HistoryViewHolder> {

    private List<EventHistoryItem> historyList;
    private Context context;

    public EventHistoryAdapter(Context context, List<EventHistoryItem> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_card, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        EventHistoryItem item = historyList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEventTitle, tvEventDateTime, tvStatus;

        HistoryViewHolder(View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDateTime = itemView.findViewById(R.id.tvEventDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(EventHistoryItem item) {
            tvEventTitle.setText(item.getEventName());
            tvStatus.setText(item.getStatus());

            if (item.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                tvEventDateTime.setText(sdf.format(item.getEventDate()));
            } else {
                tvEventDateTime.setText("Date not set");
            }

            // Set status badge color
            switch (item.getStatus().toLowerCase(Locale.ROOT)) {
                case "registered":
                case "confirmed":
                    tvStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.FCgreen));
                    break;
                case "not selected":
                case "declined":
                    tvStatus.setBackgroundColor(Color.RED);
                    break;
                case "waiting":
                default:
                    tvStatus.setBackgroundColor(Color.GRAY);
                    break;
            }

            // FIX: Add click listener to launch EventDetailsActivity (US 01.02.03, Criterion 5)
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EventDetailsActivity.class);
                intent.putExtra("EVENT_ID", item.getEventId());
                context.startActivity(intent);
            });
        }
    }
}
