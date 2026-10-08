package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import java.util.List;

/**
 * Adapter for displaying cancelled entrants who constitute the "Replacement Pool".
 * Enables Organizers to draw replacement applicants when a selected user declines or cancels,
 * fulfilling the functionality required by US 02.05.03.
 */
public class ReplacementPoolAdapter
        extends RecyclerView.Adapter<ReplacementPoolAdapter.ReplacementViewHolder> {

    public interface OnDrawReplacementClickListener {
        void onDrawReplacementClicked(String cancelledUserId);
    }

    private final List<String> userIds;
    private final OnDrawReplacementClickListener listener;

    public ReplacementPoolAdapter(List<String> userIds,
                                  OnDrawReplacementClickListener listener) {
        this.userIds = userIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReplacementViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                    int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_participant, parent, false);
        return new ReplacementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplacementViewHolder holder, int position) {
        String uid = userIds.get(position);
        holder.bind(uid);
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    class ReplacementViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvParticipantName;
        private final TextView tvEventName;
        private final Button btnAction;

        ReplacementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            btnAction = itemView.findViewById(R.id.btnNotifyEntrant);
        }

        void bind(String uid) {
            tvParticipantName.setText("User ID: " + uid);
            tvEventName.setText("Status: Cancelled");
            btnAction.setText("Draw Replacement");

            btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDrawReplacementClicked(uid);
                }
            });
        }
    }
}
