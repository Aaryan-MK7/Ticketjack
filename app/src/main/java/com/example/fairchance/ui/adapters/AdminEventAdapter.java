package com.example.fairchance.ui.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.fragments.EventDetailsFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the Admin Event Management interface.
 * Allows Administrators to browse all events in the system (US 03.04.01) and
 * remove them if necessary (US 03.01.01).
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    private final List<Event> events = new ArrayList<>();
    private final EventRepository repo = new EventRepository();

    // For looking up organizer names
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> organizerNameCache = new HashMap<>();

    public void setEvents(List<Event> newEvents) {
        events.clear();
        if (newEvents != null) {
            events.addAll(newEvents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class AdminEventViewHolder extends RecyclerView.ViewHolder {

        ImageView ivEventImage;
        TextView tvEventName, tvEventDescription, tvOrganizer;
        Button btnViewDetails, btnRemoveEvent;

        AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnRemoveEvent = itemView.findViewById(R.id.btnRemoveEvent);
        }

        void bind(Event event) {
            Context ctx = itemView.getContext();

            tvEventName.setText(
                    event.getName() != null ? event.getName() : "Untitled Event"
            );
            tvEventDescription.setText(
                    event.getDescription() != null ? event.getDescription() : "No description provided"
            );

            Glide.with(ctx)
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                    .error(R.drawable.fairchance_logo_with_words___transparent)
                    .into(ivEventImage);

            // Organizer name lookup
            String organizerId = event.getOrganizerId();
            if (organizerId == null || organizerId.isEmpty()) {
                tvOrganizer.setText("Organizer: Unknown");
            } else if (organizerNameCache.containsKey(organizerId)) {
                tvOrganizer.setText("Organizer: " + organizerNameCache.get(organizerId));
            } else {
                tvOrganizer.setText("Organizer: loading…");
                db.collection("users").document(organizerId).get()
                        .addOnSuccessListener((DocumentSnapshot doc) -> {
                            String name = doc.getString("name");
                            if (name == null || name.trim().isEmpty()) {
                                name = organizerId; // fall back to id
                            }
                            organizerNameCache.put(organizerId, name);
                            tvOrganizer.setText("Organizer: " + name);
                        })
                        .addOnFailureListener(e -> {
                            tvOrganizer.setText("Organizer: " + organizerId);
                        });
            }

            View.OnClickListener openDetails = v -> {
                if (!(ctx instanceof FragmentActivity)) return;
                FragmentActivity act = (FragmentActivity) ctx;
                EventDetailsFragment frag = EventDetailsFragment.newInstance(event.getEventId());
                FragmentTransaction ft = act.getSupportFragmentManager().beginTransaction();
                // NOTE: if your admin uses dashboard_container, change this ID:
                ft.replace(R.id.fragment_container, frag);
                ft.addToBackStack(null);
                ft.commit();
            };

            btnViewDetails.setOnClickListener(openDetails);
            itemView.setOnClickListener(openDetails);

            btnRemoveEvent.setOnClickListener(v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle("Remove Event")
                        .setMessage("Are you sure you want to remove this event and its associated data?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            repo.deleteEvent(event.getEventId(), new EventRepository.EventTaskCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(ctx, "Event removed.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(ctx, "Error: " + message, Toast.LENGTH_LONG).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }
}
