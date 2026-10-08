package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.models.NotificationLog;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying the audit log of system notifications.
 * Used primarily by the Admin interface to review communication history between
 * organizers and entrants (US 03.08.01).
 */
public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.LogViewHolder> {

    public interface OnLogClickListener {
        void onLogClick(NotificationLog log);
    }

    private final List<NotificationLog> logs = new ArrayList<>();
    private final OnLogClickListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT
    );

    public NotificationLogAdapter(OnLogClickListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<NotificationLog> newLogs) {
        logs.clear();
        if (newLogs != null) logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NotificationLog log = logs.get(position);

        holder.tvSender.setText("Sender: " + (log.getSenderName() != null ? log.getSenderName() : "Unknown"));
        holder.tvMessageType.setText("Type: " + (log.getMessageType() != null ? log.getMessageType() : "N/A"));

        String eventDisplay = (log.getEventName() != null && !log.getEventName().isEmpty())
                ? log.getEventName()
                : "No event";
        holder.tvEvent.setText("Event: " + eventDisplay);

        holder.tvRecipients.setText("Recipients: " + log.getRecipientCount());

        String ts = (log.getTimestamp() != null)
                ? dateFormat.format(log.getTimestamp())
                : "Unknown";
        holder.tvTimestamp.setText(ts);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLogClick(log);
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessageType, tvEvent, tvRecipients, tvTimestamp;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessageType = itemView.findViewById(R.id.tvMessageType);
            tvEvent = itemView.findViewById(R.id.tvEvent);
            tvRecipients = itemView.findViewById(R.id.tvRecipients);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
