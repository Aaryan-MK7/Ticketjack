package com.example.fairchance.models;

import com.google.firebase.Timestamp;

/**
 * Represents a summary of an image (profile or event poster) for the Admin interface.
 * Acts as a Data Transfer Object (DTO) to facilitate the listing and deletion of images
 * stored in the application, decoupling the list view from the full heavy-weight models.
 */
public class AdminImageItem {

    private String id;
    private String imageUrl;
    private String title;
    private String uploaderName;
    private Timestamp uploadedAt;

    public AdminImageItem() {
    }

    public AdminImageItem(String id, String imageUrl, String title, String uploaderName, Timestamp uploadedAt) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.uploaderName = uploaderName;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
