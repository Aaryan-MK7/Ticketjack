package com.example.fairchance.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.AuthRepository;
import com.example.fairchance.ui.MainActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * The main entry point (launcher) activity for the app.
 * It displays a splash screen, checks the user's current authentication state,
 * and routes them to the appropriate activity (MainActivity or RoleSelectionActivity).
 * It also handles requesting notification permissions and saving the FCM token.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DELAY = 1500L;
    private static final String TAG = "SplashActivity";
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authRepository = new AuthRepository();

        requestNotificationPermission();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = authRepository.getCurrentUser();
            if (currentUser != null) {

                getAndSaveFcmToken();
                goToMainApp();
            } else {
                goToRoleSelection();
            }
        }, SPLASH_SCREEN_DELAY);
    }

    /**
     * Requests the POST_NOTIFICATIONS permission on Android 13+ (API 33).
     * On older devices, this check is skipped.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    /**
     * Gets the current FCM registration token and saves it to the user's profile in Firestore.
     * This ensures the user can receive targeted push notifications.
     */
    private void getAndSaveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    authRepository.saveFcmToken(token);
                });
    }

    /**
     * Navigates to the MainActivity.
     */
    private void goToMainApp() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to the RoleSelectionActivity.
     */
    private void goToRoleSelection() {
        Intent intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
        startActivity(intent);
        finish();
    }
}
