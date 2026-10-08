package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fairchance.R;
import com.example.fairchance.models.AdminUserItem;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the Admin interface to browse and filter user profiles.
 * Supports searching by name, email, or role, enabling the Admin to find
 * and remove profiles as per US 03.05.01 and US 03.02.01.
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(AdminUserItem user);
    }

    private final List<AdminUserItem> fullList = new ArrayList<>();
    private final List<AdminUserItem> filteredList = new ArrayList<>();
    private final OnUserClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public AdminUserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AdminUserItem> users) {
        fullList.clear();
        filteredList.clear();
        if (users != null) {
            fullList.addAll(users);
            filteredList.addAll(users);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.toLowerCase(Locale.getDefault());
            for (AdminUserItem u : fullList) {
                if (u.getName().toLowerCase(Locale.getDefault()).contains(lower)
                        || u.getEmail().toLowerCase(Locale.getDefault()).contains(lower)
                        || u.getRole().toLowerCase(Locale.getDefault()).contains(lower)) {
                    filteredList.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(filteredList.get(position));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvRole, tvRegistered;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            tvRegistered = itemView.findViewById(R.id.tvUserRegistered);
        }

        void bind(AdminUserItem user) {
            tvName.setText(user.getName());
            tvRole.setText(user.getRole().isEmpty()
                    ? "Role: Unknown"
                    : "Role: " + user.getRole());

            Timestamp ts = user.getCreatedAt();
            if (ts != null) {
                tvRegistered.setText("Registered: " + dateFormat.format(ts.toDate()));
            } else {
                tvRegistered.setText("Registered: N/A");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
