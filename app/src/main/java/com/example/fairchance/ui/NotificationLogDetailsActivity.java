package com.example.fairchance.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fairchance.R;
import com.example.fairchance.models.NotificationLog;
import java.text.DateFormat;
import java.util.Date;

/**
 * Activity that shows details for a single notification log entry.
 */
public class NotificationLogDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_SENDER_NAME = "extra_sender_name";
    private static final String EXTRA_MESSAGE_TYPE = "extra_message_type";
    private static final String EXTRA_MESSAGE_BODY = "extra_message_body";
    private static final String EXTRA_EVENT_NAME = "extra_event_name";
    private static final String EXTRA_RECIPIENT_COUNT = "extra_recipient_count";
    private static final String EXTRA_RECIPIENT_IDS = "extra_recipient_ids";
    private static final String EXTRA_TIMESTAMP = "extra_timestamp";

    /**
     * Launches this screen to display details for the given log.
     */
    public static void start(Context context, NotificationLog log) {
        Intent intent = new Intent(context, NotificationLogDetailsActivity.class);
        intent.putExtra(EXTRA_SENDER_NAME, log.getSenderName());
        intent.putExtra(EXTRA_MESSAGE_TYPE, log.getMessageType());
        intent.putExtra(EXTRA_MESSAGE_BODY, log.getMessageBody());
        intent.putExtra(EXTRA_EVENT_NAME, log.getEventName());
        intent.putExtra(EXTRA_RECIPIENT_COUNT, log.getRecipientCount());
        intent.putStringArrayListExtra(
                EXTRA_RECIPIENT_IDS,
                log.getRecipientIds() != null
                        ? new java.util.ArrayList<>(log.getRecipientIds())
                        : new java.util.ArrayList<>()
        );
        if (log.getTimestamp() != null) {
            intent.putExtra(EXTRA_TIMESTAMP, log.getTimestamp().getTime());
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_log_details);

        TextView tvSender = findViewById(R.id.tvDetailSender);
        TextView tvType = findViewById(R.id.tvDetailType);
        TextView tvEvent = findViewById(R.id.tvDetailEvent);
        TextView tvTimestamp = findViewById(R.id.tvDetailTimestamp);
        TextView tvRecipients = findViewById(R.id.tvDetailRecipients);
        TextView tvBody = findViewById(R.id.tvDetailBody);

        String senderName = getIntent().getStringExtra(EXTRA_SENDER_NAME);
        String type = getIntent().getStringExtra(EXTRA_MESSAGE_TYPE);
        String eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);
        String body = getIntent().getStringExtra(EXTRA_MESSAGE_BODY);
        long recipientCount = getIntent().getLongExtra(EXTRA_RECIPIENT_COUNT, 0);
        java.util.ArrayList<String> recipientIds =
                getIntent().getStringArrayListExtra(EXTRA_RECIPIENT_IDS);
        long tsMillis = getIntent().getLongExtra(EXTRA_TIMESTAMP, -1);

        tvSender.setText(senderName != null ? senderName : "Unknown sender");
        tvType.setText(type != null ? type : "N/A");
        tvEvent.setText(eventName != null ? eventName : "No event");

        if (tsMillis > 0) {
            Date date = new Date(tsMillis);
            String formatted = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT).format(date);
            tvTimestamp.setText(formatted);
        } else {
            tvTimestamp.setText("Unknown");
        }

        StringBuilder recipientsText = new StringBuilder();
        recipientsText.append("Recipients (").append(recipientCount).append("):\n");
        if (recipientIds != null && !recipientIds.isEmpty()) {
            for (String id : recipientIds) {
                recipientsText.append("- ").append(id).append("\n");
            }
        } else {
            recipientsText.append("No recipient IDs stored.");
        }

        tvRecipients.setText(recipientsText.toString());
        tvBody.setText(body != null ? body : "");
    }
}
