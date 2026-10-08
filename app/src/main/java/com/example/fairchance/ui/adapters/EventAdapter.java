package com.example.fairchance.ui.adapters;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.EventDetailsActivity;
import com.example.fairchance.ui.fragments.EventDetailsFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * The primary adapter for displaying Event cards in the main feed.
 * This complex adapter handles:
 * 1. Rendering event details for both Entrants and Organizers.
 * 2. Filtering by search text, category, and date (US 01.01.04).
 * 3. Managing the "Join/Leave Waiting List" button state based on user status (US 01.01.01, US 01.01.02).
 * 4. Handling Geolocation requirements for joining specific events (US 02.02.03).
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull;
    private String currentCategory = "All";
    private String currentSearchText = "";
    private String currentDateFilter = "ALL";
    private final EventRepository repo = new EventRepository();

    private boolean openOrganizerView = false;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
        this.eventListFull = new ArrayList<>(eventList);
    }
    public EventAdapter(List<Event> eventList, boolean openOrganizerView) {
        this.eventList = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.openOrganizerView = openOrganizerView;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void updateBaseEventsAndRefilter(List<Event> events) {
        this.eventListFull = new ArrayList<>(events);
        getFilter().filter(this.currentSearchText);
    }

    public void setEvents(List<Event> events) {
        this.eventList = events;
        this.eventListFull = new ArrayList<>(events);
        this.currentCategory = "All";
        this.currentSearchText = "";
        notifyDataSetChanged();
    }

    public void setCategory(String category) {
        this.currentCategory = category;
        getFilter().filter(this.currentSearchText);
    }

    public void setDateFilter(String dateFilter) {
        this.currentDateFilter = dateFilter;
        getFilter().filter(this.currentSearchText);
    }

    @Override
    public Filter getFilter() {
        return eventFilter;
    }

    private final Filter eventFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Event> filteredList = new ArrayList<>();
            currentSearchText = constraint.toString().toLowerCase(Locale.ROOT).trim();

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            for (Event item : eventListFull) {
                boolean categoryMatches = currentCategory.equals("All") ||
                        (item.getCategory() != null && item.getCategory().equalsIgnoreCase(currentCategory));

                boolean searchMatches = currentSearchText.isEmpty() ||
                        (item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(currentSearchText));

                boolean dateMatches = true;
                if (currentDateFilter.equals("TODAY") && item.getEventDate() != null) {
                    Calendar eventCal = Calendar.getInstance();
                    eventCal.setTime(item.getEventDate());
                    eventCal.set(Calendar.HOUR_OF_DAY, 0);
                    eventCal.set(Calendar.MINUTE, 0);
                    eventCal.set(Calendar.SECOND, 0);
                    eventCal.set(Calendar.MILLISECOND, 0);
                    dateMatches = eventCal.equals(today);
                }

                if (categoryMatches && searchMatches && dateMatches) filteredList.add(item);
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            eventList.clear();
            //noinspection unchecked
            eventList.addAll((List<Event>) results.values);
            notifyDataSetChanged();
        }
    };

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView eventImage;
        private final TextView eventTitle, eventDate, eventLocation, eventStatus;
        private final Button buttonJoin;
        private final TextView buttonDetails;

        EventViewHolder(View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_image);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
            eventLocation = itemView.findViewById(R.id.event_location);
            eventStatus = itemView.findViewById(R.id.event_status);
            buttonJoin = itemView.findViewById(R.id.button_join);
            buttonDetails = itemView.findViewById(R.id.button_details);
        }

        void bind(Event event) {
            eventTitle.setText(event.getName());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            eventDate.setText(event.getEventDate() != null ? sdf.format(event.getEventDate()) : "Date not set");
            eventLocation.setText(event.getLocation() != null ? event.getLocation() : "Location not specified");
            eventStatus.setText("Status: Registration Open");

            Glide.with(itemView.getContext())
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                    .error(R.drawable.fairchance_logo_with_words___transparent)
                    .into(eventImage);

            Context context = itemView.getContext();

            View.OnClickListener openDetails = v -> {
                if (openOrganizerView) {
                    // Organizer view: open the purple fragment with QR + buttons
                    EventDetailsFragment fragment = EventDetailsFragment.newInstance(event.getEventId());
                    FragmentTransaction transaction = ((AppCompatActivity) context)
                            .getSupportFragmentManager()
                            .beginTransaction();
                    // FIX: Use dashboard_container to ensure clean replacement (no overlap)
                    transaction.replace(R.id.dashboard_container, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                } else {
                    // Entrant view: open the green JOIN WAITLIST activity
                    Intent intent = new Intent(context, EventDetailsActivity.class);
                    intent.putExtra("EVENT_ID", event.getEventId());
                    context.startActivity(intent);
                }
            };

            buttonDetails.setOnClickListener(openDetails);
            itemView.setOnClickListener(openDetails);

            if (openOrganizerView) {
                // Organizer view: hide the join button completely
                buttonJoin.setVisibility(View.GONE);
            } else {
                buttonJoin.setVisibility(View.VISIBLE);
            }

            repo.checkEventHistoryStatus(event.getEventId(), new EventRepository.EventHistoryCheckCallback() {
                @Override
                public void onSuccess(String status) {
                    if (status == null) {
                        buttonJoin.setText("Join Waiting List");
                        buttonJoin.setEnabled(true);
                        // User has NOT joined: Set color to GREEN
                        buttonJoin.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.FCgreen)));
                    } else if ("Waiting".equals(status)) {
                        buttonJoin.setText("Leave Waiting List");
                        buttonJoin.setEnabled(true);
                        // User has joined: Set color to RED
                        buttonJoin.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                    } else {
                        // User is Selected, Confirmed, Declined, etc. -> Disable button
                        buttonJoin.setText(status);
                        buttonJoin.setEnabled(false);
                        buttonJoin.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                    }
                }

                @Override
                public void onError(String message) { }
            });

            buttonJoin.setOnClickListener(v -> {
                String label = buttonJoin.getText().toString();
                // This will only run if button is enabled
                if (label.contains("Join")) {
                    if (event.isGeolocationRequired()) {
                        Activity activity = (Activity) context;

                        // Check permissions
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(
                                    activity,
                                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                                    1001
                            );
                            Toast.makeText(context, "Location permission required. Grant it and tap Join again.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Permission granted, get location
                        FusedLocationProviderClient fused = LocationServices.getFusedLocationProviderClient(context);
                        fused.getLastLocation()
                                .addOnSuccessListener(loc -> {
                                    if (loc == null) {
                                        Toast.makeText(context, "Unable to get location. Try opening Event Details.", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    repo.joinWaitingListWithLocation(
                                            event.getEventId(),
                                            event,
                                            loc.getLatitude(),
                                            loc.getLongitude(),
                                            new EventRepository.EventTaskCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    Toast.makeText(context, "Joined with location.", Toast.LENGTH_SHORT).show();
                                                    buttonJoin.setText("Leave Waiting List");
                                                    buttonJoin.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                                                }

                                                @Override
                                                public void onError(String message) {
                                                    Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                    );
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );

                    } else {
                        // No Geolocation required
                        repo.joinWaitingList(event.getEventId(), event, new EventRepository.EventTaskCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(context, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                                buttonJoin.setText("Leave Waiting List");
                                buttonJoin.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    // Logic for "Leave Waiting List"
                    repo.leaveWaitingList(event.getEventId(), new EventRepository.EventTaskCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(context, "Left waiting list.", Toast.LENGTH_SHORT).show();
                            buttonJoin.setText("Join Waiting List");
                            // LEFT -> GREEN
                            buttonJoin.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.FCgreen)));
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }
}
