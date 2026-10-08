package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.models.AdminImageItem;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapter for the Admin Image browser.
 * Enables Administrators to view uploaded images (profiles, posters) and delete them
 * if they violate policy, as required by US 03.06.01 and US 03.03.01.
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.ImageViewHolder> {

    private List<AdminImageItem> images = new ArrayList<>();
    private final OnImageActionListener listener;

    public interface OnImageActionListener {
        void onPreview(AdminImageItem item);
        void onRemoveClicked(AdminImageItem item);
    }

    public AdminImageAdapter(OnImageActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AdminImageItem> newList) {
        this.images = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        AdminImageItem item = images.get(position);

        holder.tvTitle.setText(item.getTitle());

        String uploader = item.getUploaderName();
        if (uploader == null || uploader.isEmpty()) {
            holder.tvUploader.setText("Uploader: Unknown");
        } else {
            holder.tvUploader.setText("Uploader: " + uploader);
        }

        if (item.getUploadedAt() != null) {
            Date date = item.getUploadedAt().toDate();
            DateFormat df = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT);
            holder.tvUploadedAt.setText("Uploaded: " + df.format(date));
        } else {
            holder.tvUploadedAt.setText("Uploaded: No date");
        }

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .into(holder.ivPoster);

        View.OnClickListener previewClick = v -> {
            if (listener != null) {
                listener.onPreview(item);
            }
        };
        holder.itemView.setOnClickListener(previewClick);
        holder.ivPoster.setOnClickListener(previewClick);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPoster;
        TextView tvTitle;
        TextView tvUploader;
        TextView tvUploadedAt;
        Button btnDelete;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvEventName);
            tvUploader = itemView.findViewById(R.id.tvUploader);
            tvUploadedAt = itemView.findViewById(R.id.tvUploadedAt);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
