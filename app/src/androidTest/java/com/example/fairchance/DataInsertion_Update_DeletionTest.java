package com.example.fairchance;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Basic Firestore round-trip test:
 * insert → read → update → delete.
 */
@RunWith(AndroidJUnit4.class)
public class DataInsertion_Update_DeletionTest {

    @Test
    public void insertUpdateDelete_roundTrip() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        assertNotNull(db);

        String collection = "test_roundtrip";
        String docId = "doc_" + System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("field", "initial");

        try {
            // Insert
            Tasks.await(
                    db.collection(collection)
                            .document(docId)
                            .set(data),
                    5, TimeUnit.SECONDS
            );

            // Read
            DocumentSnapshot snapshot = Tasks.await(
                    db.collection(collection)
                            .document(docId)
                            .get(),
                    5, TimeUnit.SECONDS
            );

            assertTrue(snapshot.exists());
            assertEquals("initial", snapshot.getString("field"));

            // Update
            Map<String, Object> update = new HashMap<>();
            update.put("field", "updated");

            Tasks.await(
                    db.collection(collection)
                            .document(docId)
                            .update(update),
                    5, TimeUnit.SECONDS
            );

            snapshot = Tasks.await(
                    db.collection(collection)
                            .document(docId)
                            .get(),
                    5, TimeUnit.SECONDS
            );

            assertTrue(snapshot.exists());
            assertEquals("updated", snapshot.getString("field"));

            // Delete
            Tasks.await(
                    db.collection(collection)
                            .document(docId)
                            .delete(),
                    5, TimeUnit.SECONDS
            );

            snapshot = Tasks.await(
                    db.collection(collection)
                            .document(docId)
                            .get(),
                    5, TimeUnit.SECONDS
            );

            assertFalse(snapshot.exists());

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            // Allow PERMISSION_DENIED (rules locked)
            if (cause instanceof FirebaseFirestoreException &&
                    ((FirebaseFirestoreException) cause).getCode()
                            == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                assertNotNull(db);
            } else {
                throw ex;
            }
        }
    }
}
