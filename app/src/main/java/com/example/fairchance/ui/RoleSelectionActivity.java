package com.example.fairchance.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.fairchance.R;

/**
 * This activity is shown to new or logged-out users, allowing them
 * to select their role before proceeding to authentication.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        CardView entrantCard = findViewById(R.id.cardEntrant);
        CardView organizerCard = findViewById(R.id.cardOrganizer);
        CardView adminCard = findViewById(R.id.cardAdmin);

        // Set listeners on the cards
        entrantCard.setOnClickListener(v -> startAuthActivity("entrant"));
        organizerCard.setOnClickListener(v -> startAuthActivity("organizer"));
        adminCard.setOnClickListener(v -> startAuthActivity("admin"));
    }

    /**
     * Starts the AuthActivity, passing the selected role as an Intent extra.
     * @param role The role selected ("entrant", "organizer", or "admin").
     */
    private void startAuthActivity(String role) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("ROLE", role);
        startActivity(intent);
    }
}
