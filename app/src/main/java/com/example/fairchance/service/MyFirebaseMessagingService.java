package com.example.fairchance.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.ui.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Handles FCM token updates and incoming push notifications.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "FAIR_CHANCE_CHANNEL";

    /**
     * Called when a new FCM token is created.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        new AuthRepository().saveFcmToken(token);
    }

    /**
     * Handles received FCM messages (data + notification payloads).
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String navTarget = null;

        if (!remoteMessage.getData().isEmpty()) {

            String eventId = remoteMessage.getData().get("eventId");
            String status = remoteMessage.getData().get("status");

            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");

            // Update event history if needed
            if (eventId != null && status != null) {
                AuthRepository authRepository = new AuthRepository();
                if (authRepository.getCurrentUser() != null) {
                    new EventRepository().updateEventHistoryStatus(eventId, status, new EventRepository.EventTaskCallback() {
                        @Override public void onSuccess() { }
                        @Override public void onError(String message) { }
                    });
                }

                if ("Selected".equalsIgnoreCase(status)) {
                    navTarget = "NAV_TO_INVITATIONS";
                }
            }

            // Prefer notification payload if provided
            if (remoteMessage.getNotification() != null) {
                title = remoteMessage.getNotification().getTitle();
                body = remoteMessage.getNotification().getBody();
            }

            if (title == null) title = "Ticketjack Update";
            if (body == null) body = "You have a new update.";

            sendNotification(title, body, navTarget);
        }

        else if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            sendNotification(title, body, null);
        }
    }

    /**
     * Displays a system notification that routes to MainActivity.
     */
    private void sendNotification(String messageTitle, String messageBody, String navTarget) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (navTarget != null) {
            intent.putExtra(navTarget, true);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_invitations_nav)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ticketjack Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        manager.notify(0, builder.build());
    }
}
