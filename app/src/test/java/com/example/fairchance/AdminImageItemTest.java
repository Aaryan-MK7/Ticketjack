package com.example.fairchance;

import com.example.fairchance.models.AdminImageItem;
import com.google.firebase.Timestamp;
import java.util.Date;
import org.junit.Test;

import static org.junit.Assert.*;

public class AdminImageItemTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        String id = "img1";
        String url = "https://example.com/poster.png";
        String title = "Swim Lessons Poster";
        String uploaderName = "Alice";
        Timestamp uploadedAt = new Timestamp(new Date());

        AdminImageItem item = new AdminImageItem(
                id,
                url,
                title,
                uploaderName,
                uploadedAt
        );

        assertEquals(id, item.getId());
        assertEquals(url, item.getImageUrl());
        assertEquals(title, item.getTitle());
        assertEquals(uploaderName, item.getUploaderName());
        assertEquals(uploadedAt, item.getUploadedAt());
    }

    @Test
    public void setters_updateFieldsCorrectly() {
        AdminImageItem item = new AdminImageItem();

        String newId = "img2";
        String newUrl = "https://example.com/new.png";
        String newTitle = "Updated Poster";
        String newUploader = "Bob";
        Timestamp newUploadedAt = new Timestamp(new Date());

        item.setId(newId);
        item.setImageUrl(newUrl);
        item.setTitle(newTitle);
        item.setUploaderName(newUploader);
        item.setUploadedAt(newUploadedAt);

        assertEquals(newId, item.getId());
        assertEquals(newUrl, item.getImageUrl());
        assertEquals(newTitle, item.getTitle());
        assertEquals(newUploader, item.getUploaderName());
        assertEquals(newUploadedAt, item.getUploadedAt());
    }
}
